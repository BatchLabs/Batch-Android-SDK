package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.messaging.model.WebViewMessage;

/**
 * Model for the content of an WebView message
 */
@PublicSDK
public class BatchWebViewContent implements BatchInAppMessage.Content {

    private final String url;

    BatchWebViewContent(@NonNull WebViewMessage from) {
        url = from.url;
    }

    @Nullable
    public String getURL() {
        return url;
    }
}
