package com.batch.android.webservice.listener;

import com.batch.android.FailReason;
import com.batch.android.query.response.AttributesCheckResponse;

/**
 * Listener for AttributesCheckWebservice
 *
 */
public interface AttributesCheckWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess(AttributesCheckResponse response);

    /**
     * Called on error
     *
     * @param reason
     */
    void onError(FailReason reason);
}
