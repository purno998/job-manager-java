package com.purno.jobman;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.time.Duration;

public class TestJob extends AbstractJob<Void> {

    @Getter
    @Setter
    private Duration sleepTime;

    @SneakyThrows
    @Override
    public void run() {
        if (sleepTime != null) {
            Thread.sleep(sleepTime.toMillis());
        }
    }
}
