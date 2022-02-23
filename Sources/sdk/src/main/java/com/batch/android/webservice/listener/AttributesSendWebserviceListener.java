package com.batch.android.webservice.listener;

import com.batch.android.FailReason;
import com.batch.android.query.response.AttributesSendResponse;

/**
 * Listener for AttributesSendWebservice
 *
 */
public interface AttributesSendWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess(AttributesSendResponse response);

    /**
     * Called on error
     *
     * @param reason
     */
    void onError(FailReason reason);
}
