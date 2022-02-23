package com.batch.android.push.formats;

import android.graphics.Bitmap;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.batch.android.json.JSONObject;

/**
 * Format that uses BigPictureStyle or BigTextStyle according to the notification content
 */
public class SystemFormat extends BaseFormat implements NotificationFormat {

    private boolean useLegacyBigPictureIconBehaviour;

    public SystemFormat(
        @NonNull String title,
        @NonNull String body,
        @Nullable Bitmap icon,
        @Nullable Bitmap picture,
        boolean useLegacyBigPictureIconBehaviour
    ) {
        super(title, body, icon, picture);
        this.useLegacyBigPictureIconBehaviour = useLegacyBigPictureIconBehaviour;
    }

    @Nullable
    @Override
    public RemoteViews generateCollapsedView(@NonNull String packageName) {
        return null;
    }

    @Nullable
    @Override
    public RemoteViews generateExpandedView(@NonNull String packageName) {
        return null;
    }

    @Nullable
    @Override
    public NotificationCompat.Style getSupportNotificationStyle() {
        NotificationCompat.Style style;

        if (picture != null) {
            NotificationCompat.BigPictureStyle bpStyle = new NotificationCompat.BigPictureStyle();
            bpStyle.bigPicture(picture);
            bpStyle.setSummaryText(body);

            // If we don't have a large icon, the big picture will be used as a fallback
            // Make BigPictureStyle hide it when expanded so that we don't show the same image
            // twice
            if (icon == null && !useLegacyBigPictureIconBehaviour) {
                bpStyle.bigLargeIcon(null);
            }
            style = bpStyle;
        } else {
            NotificationCompat.BigTextStyle btStyle = new NotificationCompat.BigTextStyle();
            btStyle.bigText(body);
            style = btStyle;
        }

        return style;
    }

    @Override
    public void applyArguments(@Nullable JSONObject arguments) {
        // Nothing to do
    }

    public void applyExtraBuilderConfiguration(@NonNull NotificationCompat.Builder builder) {
        if (picture != null && !useLegacyBigPictureIconBehaviour) {
            // We'll display a BigPicture notification
            // Make the large icon use the big picture as a fallback if none was set
            builder.setLargeIcon(icon != null ? icon : picture);
        } else if (icon != null) {
            builder.setLargeIcon(icon);
        }
    }
}
