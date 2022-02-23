package com.batch.android.post;

import com.batch.android.core.ByteArrayHelper;
import com.batch.android.core.Logger;
import com.batch.android.inbox.InboxCandidateNotificationInternal;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Collection;

public class InboxSyncPostDataProvider implements PostDataProvider<JSONObject> {

    private static final String TAG = "InboxSyncPostDataProvider";
    private final JSONObject body;

    public InboxSyncPostDataProvider(Collection<InboxCandidateNotificationInternal> candidates) {
        this.body = new JSONObject();

        try {
            JSONArray notifications = new JSONArray();
            for (InboxCandidateNotificationInternal candidate : candidates) {
                JSONObject notification = new JSONObject();
                notification.put("notificationId", candidate.identifier);
                notification.put("read", !candidate.isUnread);
                notifications.put(notification);
            }

            this.body.put("notifications", notifications);
        } catch (JSONException e) {
            Logger.error(TAG, "Could not create post data", e);
        }
    }

    @Override
    public JSONObject getRawData() {
        return body;
    }

    @Override
    public byte[] getData() {
        return ByteArrayHelper.getUTF8Bytes(body.toString());
    }

    public boolean isEmpty() {
        return this.body.keySet().isEmpty();
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
