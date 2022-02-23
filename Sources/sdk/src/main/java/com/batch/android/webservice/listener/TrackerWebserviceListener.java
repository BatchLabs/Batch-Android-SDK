package com.batch.android.webservice.listener;

import com.batch.android.FailReason;
import com.batch.android.event.Event;
import java.util.List;

/**
 * Listener for the tracker webservice
 *
 */
public interface TrackerWebserviceListener {
    /**
     * Called when a request succeed
     *
     * @param events event successfully sent
     */
    void onSuccess(List<Event> events);

    /**
     * Called when a request fail
     *
     * @param reason reason of the failure
     * @param events event not sent
     */
    void onFailure(FailReason reason, List<Event> events);

    /**
     * Called when the webservice finish, on failure or on success
     */
    void onFinish();
}
