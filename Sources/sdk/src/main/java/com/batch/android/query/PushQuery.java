package com.batch.android.query;

import android.content.Context;
import com.batch.android.core.NotificationAuthorizationStatus;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.push.Registration;

/**
 * Query to send pushtoken to server
 *
 */
public class PushQuery extends Query {

    /**
     * Registration information
     */
    private Registration registration;

    // -------------------------------------------->

    public PushQuery(Context context, Registration registration) {
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

        obj.put("tok", registration.registrationID);
        obj.put("provider", registration.provider);
        obj.put("senderid", registration.senderID != null ? registration.senderID : JSONObject.NULL);
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
