package com.batch.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.PayloadParser;
import com.batch.android.messaging.model.AlertMessage;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.ModalMessage;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.model.WebViewMessage;
import com.batch.android.module.MessagingModule;

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

    /**
     * Returns the format of the displayable message, if any.
     * <p>
     * You should cache this result rather than access the getter multiple times, as it involves
     * some computation.
     * <p>
     * Note: This getter bypasses most of the checks of the message's internal representation.
     * Having a valid format returned here does not mean that other operations
     * (such as {@link Batch.Messaging#loadFragment(Context, BatchMessage)}) will succeed.
     *
     * @return the format of the displayable message, if any.
     */
    public Format getFormat() {
        try {
            Message msg = PayloadParser.parseBasePayload(getJSON());

            if (msg instanceof AlertMessage) {
                return Format.ALERT;
            } else if (msg instanceof UniversalMessage) {
                return Format.FULLSCREEN;
            } else if (msg instanceof BannerMessage) {
                return Format.BANNER;
            } else if (msg instanceof ModalMessage) {
                return Format.MODAL;
            } else if (msg instanceof ImageMessage) {
                return Format.IMAGE;
            } else if (msg instanceof WebViewMessage) {
                return Format.WEBVIEW;
            }
        } catch (Exception e) {
            Logger.internal(MessagingModule.TAG, "Could not read base payload from message", e);
        }
        return Format.UNKNOWN;
    }

    protected abstract String getKind();

    protected abstract Bundle getBundleRepresentation();

    /**
     * Formats that can be contained into a BatchMessage.
     * <p>
     * This list might evolve in the future
     */
    @PublicSDK
    public enum Format {
        /**
         * UNKNOWN means that the message is invalid and does not contain any displayable message,
         * or that the format is unknown to this version of the SDK, and might be available in a newer one.
         */
        UNKNOWN,
        /**
         * ALERT is simple a system alert
         */
        ALERT,
        /**
         * FULLSCREEN is the fullscreen format
         */
        FULLSCREEN,
        /**
         * BANNER is a banner that can be attached on top or bottom of your screen
         */
        BANNER,
        /**
         * BANNER is a popup that takes over the screen modally, like a system alert but with a custom style
         */
        MODAL,
        /**
         * IMAGE is a modal popup that simply shows an image in an alert (detached) or fullscreen (attached) style
         */
        IMAGE,
        /**
         * WEBVIEW is a fullscreen format that load an URL into a WebView
         */
        WEBVIEW,
    }
}
