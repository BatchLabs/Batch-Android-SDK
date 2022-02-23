package com.batch.android.messaging;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WebViewHelper {

    @Nullable
    public static String getAnalyticsIDFromURL(@NonNull String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getQueryParameter("batchAnalyticsID");
        } catch (UnsupportedOperationException ignored) {}
        return null;
    }
}
