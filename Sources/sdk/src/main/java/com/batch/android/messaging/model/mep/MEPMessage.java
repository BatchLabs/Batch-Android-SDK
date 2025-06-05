package com.batch.android.messaging.model.mep;

import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import com.batch.android.messaging.model.Message;

public class MEPMessage extends Message {

    public String bodyText;
    public String bodyRawHtml;

    protected MEPMessage(String messageIdentifier) {
        super(messageIdentifier);
    }

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
}
