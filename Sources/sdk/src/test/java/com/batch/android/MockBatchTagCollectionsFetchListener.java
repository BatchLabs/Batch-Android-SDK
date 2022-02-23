package com.batch.android;

import androidx.annotation.NonNull;
import java.util.Map;
import java.util.Set;

public class MockBatchTagCollectionsFetchListener implements BatchTagCollectionsFetchListener {

    private Map<String, Set<String>> tagCollections;
    private boolean didFail = false;
    private boolean didFinish = false;

    @Override
    public synchronized void onSuccess(@NonNull Map<String, Set<String>> tagCollections) {
        this.tagCollections = tagCollections;
        this.didFinish = true;
        this.didFail = false;
        notifyAll();
    }

    @Override
    public synchronized void onError() {
        this.didFinish = true;
        this.didFail = true;
    }

    public synchronized Map<String, Set<String>> getTagCollections() {
        return this.tagCollections;
    }

    public synchronized boolean didFinish() {
        return didFinish;
    }

    public synchronized boolean didFail() {
        return this.didFail;
    }
}
