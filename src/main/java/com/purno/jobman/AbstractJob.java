package com.purno.jobman;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
public abstract class AbstractJob<R> implements Job<R> {
    private long id;
    private String name;
    private String description;
    private JobState state =  JobState.INIT;
    private String message;
    private boolean heavyWeight;
    private boolean cancelable;
    private Instant scheduledTime;
    private Instant startTime;
    private Instant endTime;
    private Duration duration;
    private R result;
}
