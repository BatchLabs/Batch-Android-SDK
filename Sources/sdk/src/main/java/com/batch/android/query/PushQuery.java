package com.batch.android.query;

import android.content.Context;
import com.batch.android.BatchPushRegistration;
import com.batch.android.core.NotificationAuthorizationStatus;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;

/**
 * Query to send push token to server
 *
 */
public class PushQuery extends Query {

    /**
     * Registration information
     */
    private BatchPushRegistration registration;

    // -------------------------------------------->

    public PushQuery(Context context, BatchPushRegistration registration) {
        super(context, QueryType.PUSH);
        if (registration == null) {
            throw new NullPointerException("registration==null");
        }

        this.registration = registration;
    }

    // -------------------------------------------->

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = super.toJSON();
        obj.put("tok", registration.getToken());
        obj.put("provider", registration.getProvider());
        obj.put("senderid", registration.getSenderID() != null ? registration.getSenderID() : JSONObject.NULL);
        obj.put(
            "gcpprojectid",
            registration.getGcpProjectID() != null ? registration.getGcpProjectID() : JSONObject.NULL
        );
        obj.put("nty", getNotificationType());

        return obj;
    }

    /**
     * Get the current notification type
     *
     * @return
     */
    private int getNotificationType() {
        // 15 = alert + sound + vibrate + lights
        return NotificationAuthorizationStatus.canAppShowNotifications(
                getContext(),
                BatchNotificationChannelsManagerProvider.get()
            )
            ? 15
            : 0;
    }
}
