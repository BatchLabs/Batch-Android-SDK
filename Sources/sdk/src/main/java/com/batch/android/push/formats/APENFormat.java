package com.batch.android.push.formats;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
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

    private static final String LAYOUT_TYPE_KEY = "apen_layout_type";

    protected enum LayoutType {
        /**
         * Layout match_parent and image scale type to center_crop
         */
        CENTER_CROP_MP(0, false),
        /**
         * Layout 200dp and image scale type to center_crop
         */
        CENTER_CROP_200(0, true),

        /**
         * Layout match_parent and image scale type to fit_center
         */
        FIT_CENTER_MP(1, false),

        /**
         * Layout 200dp and image scale type to fit_center (default)
         */
        FIT_CENTER_200(1, true);

        /**
         * ImageView scale type for the remote view
         */
        private final int imageScaleType;

        /**
         * Whether we should force the layout height to 200dp
         */
        private final boolean forceLayoutHeight;

        LayoutType(int imageScaleType, boolean forceLayoutHeight) {
            this.imageScaleType = imageScaleType;
            this.forceLayoutHeight = forceLayoutHeight;
        }

        /**
         * Whether we should force the layout height to 200dp
         * @return true if should force the layout height to 200dp
         */
        public boolean shouldForceLayoutHeight() {
            return this.forceLayoutHeight;
        }

        /**
         * Whether the image scale type should be fit_center
         * @return true if should be fit_center
         */
        public boolean shouldFitCenter() {
            return imageScaleType == 1;
        }
    }

    /**
     * Layout type of the remote view
     */
    private LayoutType layoutType = LayoutType.FIT_CENTER_200;

    public APENFormat(
        @NonNull String title,
        @NonNull String body,
        @Nullable Bitmap collapsedIcon,
        @Nullable Bitmap picture
    ) {
        super(title, body, collapsedIcon, picture);
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
        final RemoteViews view;
        if (layoutType.shouldForceLayoutHeight()) {
            view = new RemoteViews(packageName, R.layout.com_batchsdk_notification_layout_apen_expanded_200dp);
        } else {
            view = new RemoteViews(packageName, R.layout.com_batchsdk_notification_layout_apen_expanded);
        }

        view.setTextViewText(R.id.com_batchsdk_notification_title, title);
        view.setTextViewText(R.id.com_batchsdk_notification_body, body);

        view.setViewVisibility(R.id.com_batchsdk_notification_icon_centercrop, View.GONE);
        view.setViewVisibility(R.id.com_batchsdk_notification_icon_fitcenter, View.GONE);

        int targetImageResID;
        if (layoutType.shouldFitCenter()) {
            targetImageResID = R.id.com_batchsdk_notification_icon_fitcenter;
        } else {
            targetImageResID = R.id.com_batchsdk_notification_icon_centercrop;
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
            switch (arguments.optString(LAYOUT_TYPE_KEY)) {
                case "fill_mp":
                    this.layoutType = LayoutType.CENTER_CROP_MP;
                    break;
                case "fill_200":
                    this.layoutType = LayoutType.CENTER_CROP_200;
                    break;
                case "fit_mp":
                    this.layoutType = LayoutType.FIT_CENTER_MP;
                    break;
                case "fit_200":
                default:
                    this.layoutType = LayoutType.FIT_CENTER_200;
                    break;
            }
        }
    }

    public void applyExtraBuilderConfiguration(@NonNull NotificationCompat.Builder builder) {}

    @VisibleForTesting
    LayoutType getLayoutType() {
        return layoutType;
    }
}
