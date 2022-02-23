package com.batch.android.push.formats;

import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.batch.android.json.JSONObject;

public interface NotificationFormat {
    @Nullable
    RemoteViews generateCollapsedView(@NonNull String packageName);

    @Nullable
    RemoteViews generateExpandedView(@NonNull String packageName);

    @Nullable
    NotificationCompat.Style getSupportNotificationStyle();

    void applyArguments(@Nullable JSONObject arguments);

    void applyExtraBuilderConfiguration(@NonNull NotificationCompat.Builder builder);
}
