package com.batch.android;

import androidx.annotation.NonNull;
import java.util.Map;

public class MockBatchAttributesFetchListener implements BatchAttributesFetchListener {

    private Map<String, BatchUserAttribute> attributes;
    private boolean didFail = false;
    private boolean didFinish = false;

    @Override
    public synchronized void onSuccess(@NonNull Map<String, BatchUserAttribute> attributes) {
        this.attributes = attributes;
        this.didFinish = true;
        this.didFail = false;
        notifyAll();
    }

    @Override
    public synchronized void onError() {
        this.didFinish = true;
        this.didFail = true;
    }

    public synchronized Map<String, BatchUserAttribute> getAttributes() {
        return this.attributes;
    }

    public synchronized boolean didFinish() {
        return didFinish;
    }

    public synchronized boolean didFail() {
        return this.didFail;
    }
}
