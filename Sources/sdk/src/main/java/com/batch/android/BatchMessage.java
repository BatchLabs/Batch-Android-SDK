package com.batch.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONObject;

/**
 * Model representing a Batch Messaging message.
 */
@PublicSDK
public abstract class BatchMessage implements UserActionSource {

    /**
     * Key to retrieve the messaging payload (if applicable) from an extra
     */
    public static final String MESSAGING_EXTRA_PAYLOAD_KEY = "com.batch.messaging.payload";

    private static final String KIND_KEY = "kind";

    private static final String DATA_KEY = "data";

    protected abstract JSONObject getJSON();

    /**
     * @hide
     */
    protected abstract JSONObject getCustomPayloadInternal();

    public void writeToBundle(@NonNull Bundle bundle) {
        //noinspection ConstantConditions
        if (bundle == null) {
            throw new IllegalArgumentException("bundle cannot be null");
        }

        final Bundle payloadBundle = new Bundle();
        payloadBundle.putString(KIND_KEY, getKind());
        payloadBundle.putBundle(DATA_KEY, getBundleRepresentation());

        bundle.putBundle(MESSAGING_EXTRA_PAYLOAD_KEY, payloadBundle);
    }

    public void writeToIntent(@NonNull Intent intent) {
        //noinspection ConstantConditions
        if (intent == null) {
            throw new IllegalArgumentException("intent cannot be null");
        }

        final Bundle payloadBundle = new Bundle();
        payloadBundle.putString(KIND_KEY, getKind());
        payloadBundle.putBundle(DATA_KEY, getBundleRepresentation());

        intent.putExtra(MESSAGING_EXTRA_PAYLOAD_KEY, payloadBundle);
    }

    public static BatchMessage getMessageForBundle(@NonNull Bundle bundle) throws BatchPushPayload.ParsingException {
        //noinspection ConstantConditions
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }

        final Bundle messageBundle = bundle.getBundle(MESSAGING_EXTRA_PAYLOAD_KEY);
        if (messageBundle == null) {
            throw new BatchPushPayload.ParsingException(
                "Bundle doesn't contain the required elements for reading BatchMessage"
            );
        }

        String kind = messageBundle.getString(KIND_KEY);

        if (BatchLandingMessage.KIND.equals(kind)) {
            final Bundle data = messageBundle.getBundle(DATA_KEY);
            if (data != null) {
                return BatchPushPayload.payloadFromBundle(data).getLandingMessage();
            }
        } else if (BatchInAppMessage.KIND.equals(kind)) {
            final Bundle data = messageBundle.getBundle(DATA_KEY);
            if (data != null) {
                return BatchInAppMessage.getInstanceFromBundle(data);
            }
        }

        throw new BatchPushPayload.ParsingException("Unknown BatchMessage kind");
    }

    protected abstract String getKind();

    protected abstract Bundle getBundleRepresentation();
}
