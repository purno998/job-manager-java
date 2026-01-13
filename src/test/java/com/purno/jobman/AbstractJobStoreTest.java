package com.purno.jobman;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractJobStoreTest {
    protected JobStore jobStore;

    protected abstract JobStore createJobStore();

    @BeforeEach
    void setUp() {
        jobStore = createJobStore();
    }

    @Test
    void testSaveAndGet() {
        TestJob job = new TestJob();
        job.setName("Test Job");
        job.setState(JobState.INIT);

        jobStore.save(job);

        assertTrue(job.getId() > 0);
        Job<?> retrieved = jobStore.get(job.getId());
        assertNotNull(retrieved);
        assertEquals("Test Job", retrieved.getName());
    }

    @Test
    void testUpdate() {
        TestJob job = new TestJob();
        job.setName("Original Name");
        jobStore.save(job);

        job.setName("Updated Name");
        job.setState(JobState.RUNNING);
        jobStore.save(job);

        Job<?> retrieved = jobStore.get(job.getId());
        assertEquals("Updated Name", retrieved.getName());
        assertEquals(JobState.RUNNING, retrieved.getState());
    }

    @Test
    void testDelete() {
        TestJob job = new TestJob();
        jobStore.save(job);
        long id = job.getId();

        jobStore.delete(id);

        assertNull(jobStore.get(id));
        assertEquals(0, jobStore.count());
    }

    @Test
    void testGetForRunningNow() {
        // Job 1: Scheduled in the past
        TestJob job1 = new TestJob();
        job1.setState(JobState.INIT);
        job1.setScheduledTime(Instant.now().minusSeconds(60));
        jobStore.save(job1);

        // Job 2: Scheduled in the future
        TestJob job2 = new TestJob();
        job2.setState(JobState.INIT);
        job2.setScheduledTime(Instant.now().plusSeconds(60));
        jobStore.save(job2);

        // Job 3: Already running (should not be picked up)
        TestJob job3 = new TestJob();
        job3.setState(JobState.RUNNING);
        jobStore.save(job3);

        List<Job<?>> runnable = jobStore.getForRunningNow(true);

        assertEquals(1, runnable.size());
        assertEquals(job1.getId(), runnable.getFirst().getId());
    }

    @Test
    void testGetAllAndCount() {
        jobStore.save(new TestJob());
        jobStore.save(new TestJob());

        assertEquals(2, jobStore.count());
        assertEquals(2, jobStore.getAll().size());
    }
}