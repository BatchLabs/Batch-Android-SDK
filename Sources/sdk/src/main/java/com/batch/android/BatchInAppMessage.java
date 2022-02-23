package com.batch.android;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.PayloadParser;
import com.batch.android.messaging.PayloadParsingException;
import com.batch.android.messaging.model.*;
import com.batch.android.module.MessagingModule;
import java.util.Set;

/**
 * A subclass of BatchMessage that represents an In-App message
 */
@PublicSDK
public class BatchInAppMessage extends BatchMessage implements InAppMessageUserActionSource {

    public static final String KIND = "inapp_campaign";

    private static final String LANDING_PAYLOAD_KEY = "payload";
    private static final String CUSTOM_PAYLOAD_KEY = "custom_payload";
    private static final String CAMPAIGN_TOKEN_KEY = "campaign_token";
    private static final String CAMPAIGN_ID_KEY = "campaign_id";
    private static final String CAMPAIGN_EVENT_DATA_KEY = "campaign_event_data";

    private JSONObject landingPayload;
    private JSONObject customPayload;
    private String campaignToken;
    private String campaignId;
    private JSONObject eventData;
    private Content cachedContent;

    static BatchInAppMessage getInstanceFromBundle(@NonNull Bundle bundle) {
        String landingPayloadString = bundle.getString(LANDING_PAYLOAD_KEY);
        String customPayloadString = bundle.getString(CUSTOM_PAYLOAD_KEY);
        String campaignIdString = bundle.getString(CAMPAIGN_ID_KEY);
        String eventDataString = bundle.getString(CAMPAIGN_EVENT_DATA_KEY);

        if (
            TextUtils.isEmpty(landingPayloadString) ||
            TextUtils.isEmpty(customPayloadString) ||
            TextUtils.isEmpty(campaignIdString) ||
            TextUtils.isEmpty(eventDataString)
        ) {
            throw new IllegalArgumentException("Corrupted bundle (code 1)");
        }

        try {
            return new BatchInAppMessage(
                bundle.getString(CAMPAIGN_TOKEN_KEY),
                campaignIdString,
                new JSONObject(eventDataString),
                new JSONObject(landingPayloadString),
                new JSONObject(customPayloadString)
            );
        } catch (JSONException e) {
            Logger.internal(MessagingModule.TAG, "Unexpected error while reading a BatchInAppMessage from a bundle", e);
            throw new IllegalArgumentException("Corrupted bundle (code 2)");
        }
    }

    public BatchInAppMessage(
        @Nullable String campaignToken,
        @NonNull String campaignID,
        @NonNull JSONObject eventData,
        @NonNull JSONObject landingPayload,
        @NonNull JSONObject customPayload
    ) {
        this.campaignToken = campaignToken;
        this.campaignId = campaignID;
        this.eventData = eventData;
        this.landingPayload = landingPayload;
        this.customPayload = customPayload;
    }

    @Override
    protected JSONObject getJSON() {
        return landingPayload;
    }

    /**
     * @hide
     */
    @Override
    protected JSONObject getCustomPayloadInternal() {
        return customPayload;
    }

    @Override
    protected String getKind() {
        return KIND;
    }

    @Override
    protected Bundle getBundleRepresentation() {
        // TODO: save the whole campaign...
        // Or just what we need?
        Bundle b = new Bundle();
        b.putString(LANDING_PAYLOAD_KEY, landingPayload == null ? "{}" : landingPayload.toString());
        b.putString(CUSTOM_PAYLOAD_KEY, customPayload == null ? "{}" : customPayload.toString());
        if (campaignToken != null) {
            b.putString(CAMPAIGN_TOKEN_KEY, campaignToken);
        }
        b.putString(CAMPAIGN_ID_KEY, campaignId);
        b.putString(CAMPAIGN_EVENT_DATA_KEY, eventData.toString());
        return b;
    }

    String getCampaignId() {
        return campaignId;
    }

    JSONObject getEventData() {
        return eventData;
    }

    @Override
    @NonNull
    public JSONObject getCustomPayload() {
        if (customPayload != null) {
            try {
                return new JSONObject(customPayload);
            } catch (JSONException e) {
                // Not possible
            }
        }
        return new JSONObject();
    }

    /**
     * Get an In-App Message's visual contents.<br/>
     * <p>
     * Since an In-App message's contents can change a lot between formats, you will need to cast this to
     * one of the classes implementing {@link BatchInAppMessage.Content}, such as {@link BatchAlertContent}, {@link BatchInterstitialContent}, {@link BatchBannerContent}, {@link BatchImageContent} or {@link BatchWebViewContent}<br/>
     * More types might be added in the future, so don't make any assumptions on the instance returned.
     *
     * @return The In-App message's visual contents. Can be null if an error occurred or if not applicable
     */
    @Nullable
    public synchronized Content getContent() {
        if (cachedContent == null) {
            if (landingPayload != null) {
                try {
                    Message msg = PayloadParser.parsePayload(landingPayload);
                    if (msg instanceof AlertMessage) {
                        cachedContent = new BatchAlertContent((AlertMessage) msg);
                    } else if (msg instanceof UniversalMessage) {
                        cachedContent = new BatchInterstitialContent((UniversalMessage) msg);
                    } else if (msg instanceof BannerMessage) {
                        cachedContent = new BatchBannerContent((BannerMessage) msg);
                    } else if (msg instanceof ImageMessage) {
                        cachedContent = new BatchImageContent((ImageMessage) msg);
                    } else if (msg instanceof ModalMessage) {
                        cachedContent = new BatchModalContent((ModalMessage) msg);
                    } else if (msg instanceof WebViewMessage) {
                        cachedContent = new BatchWebViewContent((WebViewMessage) msg);
                    }
                } catch (PayloadParsingException e) {
                    Logger.internal(MessagingModule.TAG, "Could not make content", e);
                }
            }
        }
        return cachedContent;
    }

    /**
     * Get the campaign token. This is the same token as you see when opening the In-App Campaign in your browser, when on the dashboard.
     * <p>
     * Can be null.
     */
    @Nullable
    public String getCampaignToken() {
        return campaignToken;
    }

    /**
     * Interface defining a BatchInAppMessage content model object.
     * See classes implementing this interface to learn more.
     */
    @PublicSDK
    public interface Content {}
}
