package com.purno.jobman;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

public interface Job<R> extends Serializable {
    long getId();
    void setId(long id);

    String getName();
    void setName(String name);

    String getDescription();
    void setDescription(String description);

    JobState getState();
    void setState(JobState state);

    String getMessage();
    void setMessage(String message);

    boolean isHeavyWeight();
    void setHeavyWeight(boolean heavyWeight);

    boolean isCancelable();
    void setCancelable(boolean cancelable);

    Instant getScheduledTime();
    void setScheduledTime(Instant scheduledTime);

    Instant getStartTime();
    void setStartTime(Instant startTime);

    Instant getEndTime();
    void setEndTime(Instant endTime);

    Duration getDuration();
    void setDuration(Duration duration);

    void run();

    R getResult();
    void setResult(R result);
}
