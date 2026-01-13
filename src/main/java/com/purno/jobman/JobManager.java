package com.purno.jobman;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
public class JobManager {
    private final JobStore jobStore;
    private final Consumer<Job<?>> progressConsumer;
    private final int heavyWeightJobLimit;

    private final ScheduledExecutorService jobRunner = Executors.newScheduledThreadPool(1);
    private final ExecutorService jobExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<Long, Thread> jobThreads = new ConcurrentHashMap<>();
    private int heavyWeightJobCount = 0;

    public JobManager(JobStore jobStore, Consumer<Job<?>> progressConsumer, int heavyWeightJobLimit) {
        this.jobStore = jobStore == null ? new InMemoryJobStore() : jobStore;
        this.progressConsumer = progressConsumer == null ? this::logProgress : progressConsumer;
        this.heavyWeightJobLimit = heavyWeightJobLimit > 0
                ? heavyWeightJobLimit
                : Runtime.getRuntime().availableProcessors();

        jobRunner.scheduleWithFixedDelay(this::run, 0, 5, TimeUnit.SECONDS);
    }

    private void logProgress(Job<?> job) {
        log.debug("Job: {}, state: {}, message: {}", job.getId(), job.getState(), job.getMessage());
    }

    private void run() {
        try {
            boolean includeHeavyWeight = heavyWeightJobCount < heavyWeightJobLimit;
            for (Job<?> job : jobStore.getForRunningNow(includeHeavyWeight)) {
                if (job.isHeavyWeight()) {
                    heavyWeightJobCount++;
                }

                job.setState(JobState.WAITING);
                job.setMessage("Waiting");
                reportProgress(job);

                jobExecutor.execute(() -> execute(job));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void execute(Job<?> job) {
        if (jobThreads.containsKey(job.getId())) {
            return;
        }

        jobThreads.put(job.getId(), Thread.currentThread());
        Instant start = Instant.now();

        try {
            if (job.getStartTime() == null) {
                job.setStartTime(start);
            }

            job.setState(JobState.RUNNING);
            job.setMessage("Running");
            reportProgress(job);

            job.run();

            job = jobStore.get(job.getId());
            if (job.getState() == JobState.RUNNING) {
                job.setState(JobState.SUCCESSFUL);
                job.setMessage("Success");
            }
        }  catch (Exception e) {
            job = jobStore.get(job.getId());
            if (job.getState() == JobState.RUNNING) {
                job.setState(JobState.FAILED);
                job.setMessage("Failed: "  + e.getMessage());
            }
        } finally {
            jobThreads.remove(job.getId());
            if (job.isHeavyWeight()) {
                heavyWeightJobCount--;
            }

            Instant end = Instant.now();
            if (job.getState().isDone()) {
                job.setEndTime(end);
            }

            Duration duration = Duration.between(start, end);
            if (job.getDuration() == null) {
                job.setDuration(duration);
            } else {
                job.setDuration(job.getDuration().plus(duration));
            }

            jobStore.save(job);
            reportProgress(job);
        }
    }

    public void reportProgress(Job<?> job) {
        progressConsumer.accept(job);
    }

    public void add(Job<?> job) {
        jobStore.save(job);
    }

    private void cancel(long jobId, boolean force) {
        Job<?> job = jobStore.get(jobId);
        if (job == null) {
            throw new IllegalStateException("Job with id " + jobId + " not found");
        }
        if (!job.isCancelable() && !force) {
            throw new IllegalStateException("Job with id " + jobId + " is not cancelable");
        }

        job.setState(JobState.CANCELED);
        job.setMessage("Canceling");
        jobStore.save(job);

        Thread thread = jobThreads.get(jobId);
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void cancel(long jobId) {
        cancel(jobId, false);
    }

    public void remove(long jobId) {
        cancel(jobId, true);
        jobStore.delete(jobId);
    }

    @SneakyThrows
    public void stop() {
        jobRunner.shutdown();
        jobExecutor.shutdown();

        log.info("JobRunner terminated: {}", jobRunner.awaitTermination(1, TimeUnit.MINUTES));
        log.info("JobExecutor terminated: {}", jobExecutor.awaitTermination(1, TimeUnit.MINUTES));
    }
}
