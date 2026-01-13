package com.purno.jobman;

class InMemoryJobManagerTest extends AbstractJobManagerTest {

    @Override
    protected JobStore createJobStore() {
        return new InMemoryJobStore();
    }
}
