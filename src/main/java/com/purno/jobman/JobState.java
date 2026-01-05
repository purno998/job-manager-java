package com.purno.jobman;

public enum JobState {
    INIT,
    WAITING,
    RUNNING,
    PAUSED,
    SUCCESSFUL,
    FAILED,
    CANCELED;

    public boolean isDone() {
        return this == SUCCESSFUL || this == FAILED || this == CANCELED;
    }
}
