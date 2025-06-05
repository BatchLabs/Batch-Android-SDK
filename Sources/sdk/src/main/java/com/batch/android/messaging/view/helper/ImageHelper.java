package com.batch.android.messaging.view.helper;

import android.graphics.Bitmap;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.gif.GifHelper;
import com.batch.android.module.MessagingModule;

public class ImageHelper {

    public interface Cache {
        void put(@NonNull AsyncImageDownloadTask.Result result);

        @Nullable
        AsyncImageDownloadTask.Result get(@NonNull String key);
    }

    public static void setDownloadResultInImage(
        @NonNull ImageView targetImage,
        @NonNull AsyncImageDownloadTask.Result result
    ) {
        if (result instanceof AsyncImageDownloadTask.BitmapResult) {
            targetImage.setImageBitmap(((AsyncImageDownloadTask.BitmapResult) result).get());
        } else if (result instanceof AsyncImageDownloadTask.GIFResult) {
            try {
                byte[] gifData = ((AsyncImageDownloadTask.GIFResult) result).get();
                targetImage.setImageDrawable(GifHelper.getDrawableForBytes(targetImage.getContext(), gifData, true));
            } catch (Exception e) {
                Logger.internal(MessagingModule.TAG, "Could not start GIF", e);
            }
        } else {
            Logger.internal(MessagingModule.TAG, "Could not display AsyncImageDownloadTask.Result: unknown type");
        }
    }

    /**
     * Set the download result in the image view and resize the for the given width.
     * @param targetImage The target image view.
     * @param result The download result.
     * @param width The width to resize the image to.
     */
    public static void setDownloadResultInImageWithResize(
        @NonNull ImageView targetImage,
        @NonNull AsyncImageDownloadTask.Result result,
        float width
    ) {
        if (result instanceof AsyncImageDownloadTask.BitmapResult) {
            Bitmap bitmap = ((AsyncImageDownloadTask.BitmapResult) result).get();
            float computedHeight = (width / (float) bitmap.getWidth()) * (float) bitmap.getHeight();
            if (width > 0 && computedHeight > 0) {
                Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, (int) width, (int) computedHeight, true);
                targetImage.setImageBitmap(resizedImage);
            }
        } else if (result instanceof AsyncImageDownloadTask.GIFResult) {
            try {
                byte[] gifData = ((AsyncImageDownloadTask.GIFResult) result).get();
                targetImage.setImageDrawable(GifHelper.getDrawableForBytes(targetImage.getContext(), gifData, true));
            } catch (Exception e) {
                Logger.internal(MessagingModule.TAG, "Could not start GIF", e);
            }
        } else {
            Logger.internal(MessagingModule.TAG, "Could not display AsyncImageDownloadTask.Result: unknown type");
        }
    }
}
