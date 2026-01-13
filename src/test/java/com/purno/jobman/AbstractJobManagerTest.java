package com.purno.jobman;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
abstract class AbstractJobManagerTest {
    protected JobStore jobStore;
    protected Consumer<Job<?>> progressConsumer;
    protected JobManager jobManager;
    private final Map<Long, List<Job<?>>> progressMap = new ConcurrentHashMap<>();

    protected abstract JobStore createJobStore();

    @BeforeEach
    void setUp() {
        jobStore = createJobStore();
        progressConsumer = this::logProgress;
        jobManager = new JobManager(jobStore, progressConsumer, 10);
    }

    @AfterEach
    void tearDown() {
        jobManager.stop();
    }

    private void logProgress(Job<?> job) {
        progressMap.computeIfAbsent(job.getId(), _ -> new ArrayList<>())
                .add(job);
    }

    @SneakyThrows
    private void waitForJob(long jobId, JobState targetState) {
        JobState state = JobState.INIT;
        while (state != targetState) {
            Thread.sleep(7000);

            List<Job<?>> logs = progressMap.get(jobId);
            state = logs == null || logs.isEmpty()
                    ? JobState.INIT
                    : logs.getLast().getState();
            log.info("Job {} with state {}", jobId, state);
        }
    }

    @Test
    @Timeout(10)
    void testJobSuccessfulExecution() {
        TestJob job = new TestJob();
        job.setName("Success Job");

        jobManager.add(job);
        waitForJob(job.getId(), JobState.SUCCESSFUL);

        Job<?> retrieved = jobStore.get(job.getId());
        assertEquals(JobState.SUCCESSFUL, retrieved.getState());
        assertNotNull(retrieved.getStartTime());
        assertNotNull(retrieved.getEndTime());
    }

    @SneakyThrows
    @Test
    void testHeavyWeightJobLimit() {
        // Add more heavyweight jobs than the limit (10)
        for (int i = 0; i < 15; i++) {
            TestJob job = new TestJob();
            job.setHeavyWeight(true);
            jobManager.add(job);
        }

        Thread.sleep(8000);

        // Check that count of jobs in WAITING/RUNNING does not exceed limit
        long activeHeavyJobs = jobStore.getAll().stream()
                .filter(j -> j.isHeavyWeight() && (j.getState() == JobState.RUNNING || j.getState() == JobState.WAITING))
                .count();

        assertTrue(activeHeavyJobs <= 10);
    }

    @Test
    @Timeout(10)
    void testCancelRunningJob() throws InterruptedException {
        TestJob job = new TestJob();
        job.setCancelable(true);
        job.setSleepTime(Duration.ofSeconds(20));

        jobManager.add(job);
        waitForJob(job.getId(), JobState.RUNNING);

        jobManager.cancel(job.getId());

        // Wait for interruption to propagate
        Thread.sleep(1000);

        assertEquals(JobState.CANCELED, jobStore.get(job.getId()).getState());
    }

    @Test
    void testRemoveJob() {
        TestJob job = new TestJob();
        jobManager.add(job);
        long id = job.getId();

        jobManager.remove(id);

        assertNull(jobStore.get(id));
    }
}