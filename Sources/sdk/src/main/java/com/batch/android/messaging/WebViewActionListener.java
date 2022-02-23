package com.batch.android.messaging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchMessagingWebViewJavascriptBridge;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.MessagingError;

public interface WebViewActionListener {
    void onCloseAction();

    void onDismissAction(@Nullable String analyticsID);

    void onErrorAction(
        @NonNull BatchMessagingWebViewJavascriptBridge.DevelopmentErrorCause developmentCause,
        @NonNull MessagingError messagingCause,
        @Nullable String description
    );

    void onOpenDeeplinkAction(@NonNull String url, @Nullable Boolean openInAppOverride, @Nullable String analyticsID);

    void onPerformAction(@NonNull String action, @NonNull JSONObject args, @Nullable String analyticsID);
}
