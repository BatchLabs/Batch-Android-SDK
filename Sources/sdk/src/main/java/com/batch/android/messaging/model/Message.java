package com.batch.android.messaging.model;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import com.batch.android.json.JSONObject;
import java.io.Serializable;

public abstract class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public String messageIdentifier;
    public String devTrackingIdentifier;
    public String bodyText;
    public String bodyRawHtml;

    public JSONObject eventData;

    public Source source = Source.UNKNOWN;

    /**
     * Returns the parsed HTML body if applicable
     * Fallbacks on the plain text body
     */
    public CharSequence getBody() {
        final Spanned bodyHtml = getSpannedBody();
        if (!TextUtils.isEmpty(bodyHtml)) {
            return bodyHtml;
        } else {
            return bodyText;
        }
    }

    private Spanned getSpannedBody() {
        if (bodyRawHtml == null) {
            return null;
        }

        Spanned output;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            output = Html.fromHtml(bodyRawHtml, 0);
        } else {
            output = Html.fromHtml(bodyRawHtml);
        }

        if (output == null || output.length() == 0) {
            return null;
        }

        return output;
    }

    public enum Source {
        UNKNOWN,
        LANDING,
        LOCAL,
    }
}
