package com.batch.android.push.formats;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;
import com.batch.android.R;
import com.batch.android.json.JSONObject;

/**
 * The News format is a notification format that works
 * very similarly to BigPictureStyle, but allows multiple lines when expanded.
 * <p>
 * It also automatically manages falling back on a reduced version of the large picture
 * when collapsed, if a large icon has not been specified
 */
public class APENFormat extends BaseFormat implements NotificationFormat {

    private ImageView.ScaleType imageScaleType;

    public APENFormat(
        @NonNull String title,
        @NonNull String body,
        @Nullable Bitmap collapsedIcon,
        @Nullable Bitmap picture
    ) {
        super(title, body, collapsedIcon, picture);
        imageScaleType = ImageView.ScaleType.CENTER_CROP;
    }

    public RemoteViews generateCollapsedView(@NonNull String packageName) {
        final RemoteViews view = new RemoteViews(packageName, R.layout.com_batchsdk_notification_layout_apen_collapsed);
        view.setTextViewText(R.id.com_batchsdk_notification_title, title);
        view.setTextViewText(R.id.com_batchsdk_notification_body, body);

        Bitmap finalIcon = icon != null ? icon : picture;

        if (finalIcon != null) {
            view.setImageViewBitmap(R.id.com_batchsdk_notification_icon1, finalIcon);
        } else {
            view.setViewVisibility(R.id.com_batchsdk_notification_icon1, View.GONE);
        }

        return view;
    }

    public RemoteViews generateExpandedView(@NonNull String packageName) {
        final RemoteViews view = new RemoteViews(packageName, R.layout.com_batchsdk_notification_layout_apen_expanded);

        view.setTextViewText(R.id.com_batchsdk_notification_title, title);
        view.setTextViewText(R.id.com_batchsdk_notification_body, body);

        view.setViewVisibility(R.id.com_batchsdk_notification_icon_centercrop, View.GONE);
        view.setViewVisibility(R.id.com_batchsdk_notification_icon_fitcenter, View.GONE);

        int targetImageResID;
        switch (imageScaleType) {
            case CENTER_CROP:
            default:
                targetImageResID = R.id.com_batchsdk_notification_icon_centercrop;
                break;
            case FIT_CENTER:
                targetImageResID = R.id.com_batchsdk_notification_icon_fitcenter;
                break;
        }

        if (picture != null) {
            view.setImageViewBitmap(targetImageResID, picture);
            view.setViewVisibility(targetImageResID, View.VISIBLE);
        }

        return view;
    }

    public NotificationCompat.Style getSupportNotificationStyle() {
        return new NotificationCompat.DecoratedCustomViewStyle();
    }

    @Override
    public void applyArguments(@Nullable JSONObject arguments) {
        if (arguments != null) {
            switch (arguments.reallyOptInteger("scale", 0)) {
                case 0:
                default:
                    imageScaleType = ImageView.ScaleType.CENTER_CROP;
                    break;
                case 1:
                    imageScaleType = ImageView.ScaleType.FIT_CENTER;
                    break;
            }
        }
    }

    public void applyExtraBuilderConfiguration(@NonNull NotificationCompat.Builder builder) {}

    @VisibleForTesting
    ImageView.ScaleType getImageScaleType() {
        return imageScaleType;
    }

    @SuppressLint("AnnotateVersionCheck")
    public static boolean isSupported() {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
