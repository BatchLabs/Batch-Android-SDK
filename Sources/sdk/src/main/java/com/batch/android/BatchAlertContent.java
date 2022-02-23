package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.AlertMessage;

/**
 * Model for the content of an Alert In-App/Landing message
 */
@PublicSDK
public class BatchAlertContent implements BatchInAppMessage.Content {

    private String trackingIdentifier;

    private String title;

    private String body;

    private String cancelLabel;

    private CTA acceptCTA;

    BatchAlertContent(@NonNull AlertMessage from) {
        trackingIdentifier = from.devTrackingIdentifier;
        title = from.titleText;
        body = from.bodyText;
        cancelLabel = from.cancelButtonText;

        if (from.acceptCTA != null) {
            acceptCTA = new CTA(from.acceptCTA);
        }
    }

    @Nullable
    public String getTrackingIdentifier() {
        return trackingIdentifier;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getBody() {
        return cancelLabel;
    }

    @Nullable
    public String getCancelLabel() {
        return body;
    }

    @Nullable
    public CTA getAcceptCTA() {
        return acceptCTA;
    }

    @PublicSDK
    public static class CTA {

        private String label;

        private String action;

        private JSONObject args;

        CTA(@NonNull com.batch.android.messaging.model.CTA from) {
            label = from.label;
            action = from.action;
            if (from.args != null) {
                try {
                    args = new JSONObject(from.args);
                } catch (JSONException e) {
                    args = new JSONObject();
                }
            }
        }

        @Nullable
        public String getLabel() {
            return label;
        }

        @Nullable
        public String getAction() {
            return action;
        }

        @Nullable
        public JSONObject getArgs() {
            return args;
        }
    }
}
