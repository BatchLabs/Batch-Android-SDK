package com.batch.android.eventdispatcher;

import static com.batch.android.core.InternalPushData.BATCH_BUNDLE_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchMessage;
import com.batch.android.BatchPushPayload;

/**
 * Payload accessor for a {@link Batch.EventDispatcher.Type#NOTIFICATION_DISPLAY} or {@link Batch.EventDispatcher.Type#NOTIFICATION_OPEN}.
 */
public class PushEventPayload implements Batch.EventDispatcher.Payload {

    private BatchPushPayload payload;
    private boolean isOpening;

    public PushEventPayload(BatchPushPayload payload) {
        this(payload, false);
    }

    public PushEventPayload(BatchPushPayload payload, boolean isOpening) {
        this.payload = payload;
        this.isOpening = isOpening;
    }

    @Nullable
    @Override
    public String getTrackingId() {
        // No tracking ID in push campaign
        return null;
    }

    @Nullable
    @Override
    public String getWebViewAnalyticsID() {
        return null;
    }

    @Nullable
    @Override
    public String getDeeplink() {
        return payload.getDeeplink();
    }

    @Override
    public boolean isPositiveAction() {
        return isOpening;
    }

    @Nullable
    @Override
    public String getCustomValue(@NonNull String key) {
        if (BATCH_BUNDLE_KEY.equals(key)) {
            // Hide batch payload
            return null;
        }
        return payload.getPushBundle().getString(key);
    }

    @Nullable
    @Override
    public BatchMessage getMessagingPayload() {
        return null;
    }

    @Nullable
    @Override
    public BatchPushPayload getPushPayload() {
        return payload;
    }
}
