package com.batch.android.webservice.listener;

import com.batch.android.FailReason;

/**
 * Listener for PushWebservice
 *
 */
public interface PushWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess();

    /**
     * Called on error
     *
     * @param reason
     */
    void onError(FailReason reason);
}
