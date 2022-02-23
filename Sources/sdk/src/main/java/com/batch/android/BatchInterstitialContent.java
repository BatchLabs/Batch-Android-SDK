package com.batch.android;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.UniversalMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for the content of an Interstitial In-App/Landing message
 */
@PublicSDK
public class BatchInterstitialContent implements BatchInAppMessage.Content {

    private String trackingIdentifier;

    private String header;

    private String title;

    private String body;

    private List<CTA> ctas = new ArrayList<>();

    private String mediaURL;

    private String mediaAccessibilityDescription;

    private boolean showCloseButton;

    BatchInterstitialContent(@NonNull UniversalMessage from) {
        trackingIdentifier = from.devTrackingIdentifier;
        header = from.headingText;
        title = from.titleText;
        body = from.bodyText;

        mediaAccessibilityDescription = from.heroDescription;
        if (!TextUtils.isEmpty(from.videoURL)) {
            mediaURL = from.videoURL;
        } else {
            mediaURL = from.heroImageURL;
        }

        if (from.ctas != null) {
            for (com.batch.android.messaging.model.CTA fromCTA : from.ctas) {
                ctas.add(new CTA(fromCTA));
            }
        }

        if (from.showCloseButton != null) {
            showCloseButton = from.showCloseButton;
        }
    }

    public String getTrackingIdentifier() {
        return trackingIdentifier;
    }

    public String getHeader() {
        return header;
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

    public String getMediaURL() {
        return mediaURL;
    }

    public String getMediaAccessibilityDescription() {
        return mediaAccessibilityDescription;
    }

    public boolean shouldShowCloseButton() {
        return showCloseButton;
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
