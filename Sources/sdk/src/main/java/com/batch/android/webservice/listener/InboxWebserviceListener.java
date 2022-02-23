package com.batch.android.webservice.listener;

import androidx.annotation.NonNull;
import com.batch.android.inbox.InboxWebserviceResponse;

/**
 * Listener for the fetch inbox webservice
 */
public interface InboxWebserviceListener {
    /**
     * Called when a request succeed
     */
    void onSuccess(InboxWebserviceResponse result);

    /**
     * Called on error
     *
     * @param String error
     */
    void onFailure(@NonNull String error);
}
