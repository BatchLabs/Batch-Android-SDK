package com.batch.android;

import android.content.Context;
import android.content.Intent;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.InternalPushData;

/**
 * Convenience object to retrieve Batch data out of a Batch Push intent
 *
 * @deprecated Use {@link BatchPushPayload}
 */
@PublicSDK
@Deprecated
public class BatchPushData {

    /**
     * Internal push data
     */
    private InternalPushData internalPushData;
    /**
     * Saved application context
     */
    private Context context;

    // ------------------------------------------------>

    /**
     * Build a BatchPushData object out of a Batch Push intent
     *
     * @param context context of your application
     * @param intent  Batch push intent
     * @throws NullPointerException     if context or intent is null
     * @throws IllegalArgumentException if the intent is not a Batch Push one (always check with {@link Batch.Push#isBatchPush(Intent)} )
     */
    public BatchPushData(Context context, Intent intent) {
        if (intent == null) {
            throw new NullPointerException("intent==null");
        }

        if (context == null) {
            throw new NullPointerException("context==null");
        }

        this.context = context.getApplicationContext();

        internalPushData = InternalPushData.getPushDataForReceiverIntent(intent);
        if (internalPushData == null) {
            throw new IllegalArgumentException("intent is not a Batch Push one");
        }
    }

    // ------------------------------------------------>

    /**
     * Does this push contains a deeplink
     *
     * @return true if this push contains a deeplink, false otherwise
     */
    public boolean hasDeeplink() {
        return internalPushData.hasScheme();
    }

    /**
     * Get the deeplink url contained in this push.<br>
     * You should always check if the push contains a deeplink using {@link #hasDeeplink()}
     *
     * @return the deeplink if any, null otherwise
     */
    public String getDeeplink() {
        return internalPushData.getScheme();
    }

    /**
     * Does this push contains a custom large icon
     *
     * @return true if this push contains a custom large icon to download, false otherwise
     */
    public boolean hasCustomLargeIcon() {
        return internalPushData.hasCustomBigIcon();
    }

    /**
     * Get the custom large icon url contained in this push.<br>
     * You should always check if the push contains a custom large icon using {@link #hasCustomLargeIcon()}.<br>
     * <br>
     * The url returned by this method is already optimized for the device, you have to download the image and use it in the notification
     *
     * @return the custom large icon url if any, null otherwise
     */
    public String getCustomLargeIconURL() {
        String url = internalPushData.getCustomBigIconURL();
        if (url == null) {
            return null;
        }

        return ImageDownloadWebservice.buildImageURL(context, url, internalPushData.getCustomBigIconAvailableDensity());
    }

    /**
     * Does this push contains a big picture
     *
     * @return true if this push contains a big picture to download, false otherwise
     */
    public boolean hasBigPicture() {
        return internalPushData.hasCustomBigImage();
    }

    /**
     * Get the big picture url contained in this push.<br>
     * You should always check if the push contains a big picture using {@link #hasBigPicture()}.<br>
     * <br>
     * The url returned by this method is already optimized for the device, you have to download the image and use it in the notification
     *
     * @return the big picture url if any, null otherwise
     */
    public String getBigPictureURL() {
        String url = internalPushData.getCustomBigImageURL();
        if (url == null) {
            return null;
        }

        return ImageDownloadWebservice.buildImageURL(
            context,
            url,
            internalPushData.getCustomBigImageAvailableDensity()
        );
    }
}
