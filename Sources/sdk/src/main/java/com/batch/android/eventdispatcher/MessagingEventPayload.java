package com.batch.android.eventdispatcher;

import static com.batch.android.core.InternalPushData.BATCH_BUNDLE_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchLandingMessage;
import com.batch.android.BatchMessage;
import com.batch.android.BatchPushPayload;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.Action;

/**
 * Payload accessor for a {@link Batch.EventDispatcher.Type#MESSAGING_SHOW} and other MESSAGING events.
 */
public class MessagingEventPayload implements Batch.EventDispatcher.Payload {

    private BatchMessage message;
    private JSONObject payload;
    private JSONObject customPayload;
    private Action action;
    private String buttonAnalyticsId;

    public MessagingEventPayload(BatchMessage message, JSONObject payload, JSONObject customPayload) {
        this(message, payload, customPayload, null);
    }

    public MessagingEventPayload(
        BatchMessage message,
        JSONObject payload,
        JSONObject customPayload,
        Action action,
        String buttonAnalyticsId
    ) {
        this.message = message;
        this.payload = payload;
        this.customPayload = customPayload;
        this.action = action;
        this.buttonAnalyticsId = buttonAnalyticsId;
    }

    public MessagingEventPayload(BatchMessage message, JSONObject payload, JSONObject customPayload, Action action) {
        this(message, payload, customPayload, action, null);
    }

    @Nullable
    @Override
    public String getTrackingId() {
        if (payload != null) {
            return payload.reallyOptString("did", null);
        }
        return null;
    }

    @Nullable
    @Override
    public String getWebViewAnalyticsID() {
        return buttonAnalyticsId;
    }

    @Nullable
    @Override
    public String getDeeplink() {
        if (action != null && "batch.deeplink".equals(action.action) && action.args != null) {
            return action.args.reallyOptString("l", null);
        }
        return null;
    }

    @Override
    public boolean isPositiveAction() {
        if (action != null && !action.isDismissAction()) {
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public String getCustomValue(@NonNull String key) {
        if (customPayload == null || BATCH_BUNDLE_KEY.equals(key)) {
            // Hide batch payload
            return null;
        }

        return customPayload.reallyOptString(key, null);
    }

    @Nullable
    @Override
    public BatchMessage getMessagingPayload() {
        return message;
    }

    @Nullable
    @Override
    public BatchPushPayload getPushPayload() {
        return null;
    }
}
