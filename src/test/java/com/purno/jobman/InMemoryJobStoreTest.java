package com.purno.jobman;

class InMemoryJobStoreTest extends AbstractJobStoreTest {

    @Override
    protected JobStore createJobStore() {
        return new InMemoryJobStore();
    }
}