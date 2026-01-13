package com.purno.jobman;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryJobStore  implements JobStore {
    private final Map<Long, Job<?>> jobs = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    @Override
    public void save(Job<?> job) {
        if (job.getId() <= 0) {
            job.setId(nextId.incrementAndGet());
        }

        jobs.put(job.getId(), job);
    }

    @Override
    public Job<?> get(long jobId) {
        return jobs.get(jobId);
    }

    @Override
    public void delete(long jobId) {
        jobs.remove(jobId);
    }

    @Override
    public List<Job<?>> getAll() {
        return jobs.values().stream()
                .sorted((j1, j2) -> Long.compare(j2.getId(), j1.getId()))
                .toList();
    }

    @Override
    public List<Job<?>> getForRunningNow(boolean includeHeavyWeight) {
        Instant now = Instant.now();
        return jobs.values().stream()
                .filter(j -> j.getState() == JobState.INIT)
                .filter(j -> j.getScheduledTime() == null || j.getScheduledTime().compareTo(now) <= 0)
                .filter(j -> !j.isHeavyWeight() || includeHeavyWeight)
                .sorted((j1, j2) -> Long.compare(j2.getId(), j1.getId()))
                .toList();
    }

    @Override
    public long count() {
        return jobs.size();
    }
}
