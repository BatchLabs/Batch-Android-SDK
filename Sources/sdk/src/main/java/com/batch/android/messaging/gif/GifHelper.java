package com.batch.android.messaging.gif;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class GifHelper {

    public static final int NEEDED_BYTES_FOR_TYPE_CHECK = 6;

    /**
     * Returns whether the data represents a GIF
     * <p>
     * Only the first 6 bytes are needed
     */
    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static boolean isPotentiallyAGif(@NonNull int[] data) {
        if (data.length < NEEDED_BYTES_FOR_TYPE_CHECK) {
            return false;
        }

        try {
            return (
                dataStartsWith(data, "GIF87a".getBytes("US-ASCII")) ||
                dataStartsWith(data, "GIF89a".getBytes("US-ASCII"))
            );
        } catch (UnsupportedEncodingException ignored) {
            return false;
        }
    }

    private static boolean dataStartsWith(int[] source, byte[] search) {
        if (source.length < search.length) {
            return false;
        }

        for (int i = 0; i < search.length; i++) {
            if (search[i] != source[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get a {@link GifDrawable} for the given byte array
     * <p>
     * It is assumed that the byte array has been checked with {@link #isPotentiallyAGif(int[])}
     */
    public static GifDrawable getDrawableForBytes(@NonNull Context context, @NonNull byte[] data, boolean start) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        GifDecoder decoder = new StandardGifDecoder(new BasicBitmapProvider(), buffer);
        GifDrawable drawable = new GifDrawable(context, decoder);
        if (start) {
            drawable.start();
        }
        return drawable;
    }
}
