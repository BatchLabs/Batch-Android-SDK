package com.batch.android.core.domain;

class MockDomainManager extends DomainManager {

    boolean isFeatureActivated;

    public MockDomainManager(IDomainStore store, boolean isFeatureActivated) {
        super(store);
        this.isFeatureActivated = isFeatureActivated;
    }

    @Override
    public boolean isFeatureActivated() {
        return isFeatureActivated;
    }
}
