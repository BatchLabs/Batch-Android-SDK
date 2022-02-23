package com.batch.android.messaging.gif;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

/**
 * Simple BitmapProvider that doesn't pool bitmaps
 */
public class BasicBitmapProvider implements GifDecoder.BitmapProvider {

    @NonNull
    @Override
    public Bitmap obtain(int width, int height, @NonNull Bitmap.Config config) {
        return Bitmap.createBitmap(width, height, config);
    }

    @Override
    public void release(@NonNull Bitmap bitmap) {
        bitmap.recycle();
    }

    @NonNull
    @Override
    public byte[] obtainByteArray(int size) {
        return new byte[size];
    }

    @Override
    public void release(@NonNull byte[] bytes) {
        // gc will collect it
    }

    @NonNull
    @Override
    public int[] obtainIntArray(int size) {
        return new int[size];
    }

    @Override
    public void release(@NonNull int[] array) {
        // gc will collect it
    }
}
