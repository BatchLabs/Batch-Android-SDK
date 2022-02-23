package com.batch.android.webservice.listener.impl;

import com.batch.android.FailReason;
import com.batch.android.webservice.listener.StartWebserviceListener;

/**
 * Start webservice listener implementation
 *
 */
public final class StartWebserviceListenerImpl implements StartWebserviceListener {

    @Override
    public void onSuccess() {
        // Currently nothing to do on success
    }

    @Override
    public void onError(FailReason reason) {
        // Currently nothing to do on error
    }
}
