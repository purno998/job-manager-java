package com.purno.jobman;

import java.util.List;

public interface JobStore {
    void save(Job<?> job);

    Job<?> get(long jobId);

    void delete(long jobId);

    List<Job<?>> getAll();

    List<Job<?>> getForRunningNow();

    long count();
}
