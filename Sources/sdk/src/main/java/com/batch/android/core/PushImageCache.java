package com.batch.android.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * Static helper to manage cache for push downloaded images
 *
 */
public class PushImageCache {

    private static final String TAG = "PushImageCache";

    /**
     * Number of images that can be stored simultaneously
     */
    private static final int MAX_IMAGES_STORED = 5;

    /**
     * Name of the folder that contains push image cache
     */
    private static final String IMAGES_CACHE_FOLDER = "batch_pushimg";

    /**
     * Get the path of the cache folder
     *
     * @param context
     * @return
     */
    private static String getPushImageCacheFolder(Context context) {
        return context.getCacheDir() + "/" + IMAGES_CACHE_FOLDER;
    }

    /**
     * Get the file path for the given image identifier
     *
     * @param context
     * @param identifier
     * @return
     */
    private static String getFilePathForIdentifier(Context context, String identifier) {
        return getPushImageCacheFolder(context) + "/" + identifier + ".png";
    }

    // ------------------------------------------->

    /**
     * Store the given image into cache. Identifier should be made using {@link #buildIdentifierForURL(String)} if it comes from an URL
     *
     * @param context
     * @param identifier
     * @param image
     */
    public static void storeImageInCache(Context context, String identifier, Bitmap image) {
        // First make room for a new image if needed
        clearImagesIfNeeded(context);

        File folder = new File(getPushImageCacheFolder(context));
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(getFilePathForIdentifier(context, identifier));
        try (FileOutputStream out = new FileOutputStream(file)) {
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while storing push image in cache (" + identifier + ")", e);
        }
    }

    /**
     * Get image from cache if available. Identifier should be made using {@link #buildIdentifierForURL(String)} if it comes from an URL
     *
     * @param context
     * @param identifier
     * @return bitmap if found in cache, null otherwise
     */
    public static Bitmap getImageFromCache(Context context, String identifier) {
        String filePath = getFilePathForIdentifier(context, identifier);
        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * Build a file identifier out of the url of the file (simple md5)
     *
     * @param url
     * @return
     */
    public static String buildIdentifierForURL(String url) {
        try {
            return GenericHelper.readMD5(url);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while computing MD5 identifier for url : " + url, e);
            return null;
        }
    }

    /**
     * Make room for another image in cache if needed
     *
     * @param context
     */
    private static void clearImagesIfNeeded(Context context) {
        /*
         * List cached files
         */
        File[] files = new File(getPushImageCacheFolder(context)).listFiles();
        if (files == null || files.length < MAX_IMAGES_STORED) {
            return;
        }

        /*
         * Sort them from the oldest to the newest (to delete old ones first)
         */
        //Suppress the inspection as it's not available on API 15 (IntelliJ believes it is though)
        //noinspection UseCompareMethod
        Arrays.sort(files, (f1, f2) -> Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()));

        /*
         * Delete until there's 1 slot available for cache
         */
        for (int i = 0; i < files.length - (MAX_IMAGES_STORED + 1); i++) {
            File file = files[i];
            Logger.internal(
                TAG,
                "Delete file (" + file.getAbsolutePath() + ") cause we are reaching the push image cache limit"
            );
            file.delete();
        }
    }
}
