package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.ModalMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for the content of a modal message
 */
@PublicSDK
public class BatchModalContent implements BatchInAppMessage.Content {

    private String trackingIdentifier;

    private String title;

    private String body;

    private List<CTA> ctas = new ArrayList<>();

    private Action globalTapAction;

    private String mediaURL;

    private String mediaAccessibilityDescription;

    private boolean showCloseButton;

    private Long autoCloseTimeMillis;

    BatchModalContent(@NonNull ModalMessage from) {
        trackingIdentifier = from.devTrackingIdentifier;
        title = from.titleText;
        body = from.bodyText;

        mediaURL = from.imageURL;
        mediaAccessibilityDescription = from.imageDescription;
        showCloseButton = from.showCloseButton;

        if (from.globalTapAction != null) {
            globalTapAction = new BatchModalContent.Action(from.globalTapAction);
        }

        if (from.ctas != null) {
            for (com.batch.android.messaging.model.CTA fromCTA : from.ctas) {
                ctas.add(new BatchModalContent.CTA(fromCTA));
            }
        }

        if (from.autoCloseDelay > 0) {
            autoCloseTimeMillis = (long) from.autoCloseDelay;
        }
    }

    public String getTrackingIdentifier() {
        return trackingIdentifier;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public List<CTA> getCtas() {
        return new ArrayList<>(ctas);
    }

    public Action getGlobalTapAction() {
        return globalTapAction;
    }

    public String getMediaURL() {
        return mediaURL;
    }

    public String getMediaAccessibilityDescription() {
        return mediaAccessibilityDescription;
    }

    public boolean isShowCloseButton() {
        return showCloseButton;
    }

    public Long getAutoCloseTimeMillis() {
        return autoCloseTimeMillis;
    }

    @PublicSDK
    public static class Action {

        private String action;

        private JSONObject args;

        Action(@NonNull com.batch.android.messaging.model.Action from) {
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
        public String getAction() {
            return action;
        }

        @Nullable
        public JSONObject getArgs() {
            return args;
        }
    }

    @PublicSDK
    public static class CTA extends Action {

        private String label;

        CTA(@NonNull com.batch.android.messaging.model.CTA from) {
            super(from);
            label = from.label;
        }

        @Nullable
        public String getLabel() {
            return label;
        }
    }
}
