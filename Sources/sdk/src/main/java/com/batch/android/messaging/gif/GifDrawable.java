package com.batch.android.messaging.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import com.batch.android.core.NamedThreadFactory;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GifDrawable extends Drawable implements Animatable {

    private static final int BUFFER_SIZE = 3; // Try to have 3 frames ready

    private static final int MESSAGE_FRAME_PRODUCED = 0;
    private static final int MESSAGE_RAN_OUT_OF_MEMORY = 1;

    private Paint paint;
    private int dpi;
    private boolean animating;
    private boolean ranOutOfMemory;

    private GifDecoder gifDecoder;
    private FrameInfo currentFrame = null;
    private Queue<FrameInfo> nextFrames = new LinkedList<>();
    private long nextFrameDeadline = 0; // Timestamp after which a frame should be rendered

    // Handler for onFrameProduced callback, which will ensure main thread scheduling
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_FRAME_PRODUCED) {
                onFrameProduced((FrameInfo) msg.obj);
            } else if (msg.what == MESSAGE_RAN_OUT_OF_MEMORY) {
                ranOutOfMemory();
            }
        }
    };

    private Runnable produceNextFrameRunnable = this::produceNextFrame;

    private Executor frameProducerExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("gif"));

    public GifDrawable(Context context, GifDecoder decoder) {
        dpi = context.getResources().getDisplayMetrics().densityDpi;
        paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        this.gifDecoder = decoder;

        for (int i = 0; i < BUFFER_SIZE; i++) {
            frameProducerExecutor.execute(produceNextFrameRunnable);
        }
    }

    // region Frame producing

    @WorkerThread
    private void produceNextFrame() {
        if (ranOutOfMemory) {
            return;
        }
        try {
            gifDecoder.advance();
            FrameInfo nextFrame = new FrameInfo(gifDecoder.getNextFrame(), gifDecoder.getNextDelay());
            Message.obtain(mainThreadHandler, MESSAGE_FRAME_PRODUCED, nextFrame).sendToTarget();
        } catch (OutOfMemoryError e) {
            //TODO: Batch log
            ranOutOfMemory = true;
            Message.obtain(mainThreadHandler, MESSAGE_RAN_OUT_OF_MEMORY).sendToTarget();
            Log.e("GIF", "Ran out of memory " + e);
        }
    }

    @UiThread
    private void onFrameProduced(FrameInfo frame) {
        nextFrames.add(frame);
    }

    @UiThread
    private void requestNewFrameIfNeeded() {
        if (ranOutOfMemory) {
            return;
        }

        // Don't advance if not animating, but still produce the first frame
        if (!animating && currentFrame != null) {
            return;
        }

        long now = System.currentTimeMillis();
        //Log.e("GIF", "ts: " + now + " deadline: " + nextFrameDeadline + " overshoot: " + (now-nextFrameDeadline)); // TODO: remove
        if (now > nextFrameDeadline) {
            //Log.e("GIF", "we're late");
            FrameInfo nextFrame = nextFrames.poll();
            // Only do work if the frame is ready. That can make timing wrong, but it's an acceptable caveat (people are used to that in gifs anyway)
            if (nextFrame != null) {
                if (currentFrame != null) {
                    currentFrame.bitmap.recycle();
                }
                currentFrame = nextFrame;
                nextFrameDeadline = Math.max(now + 1, now + currentFrame.delay - Math.abs(now - nextFrameDeadline)); // If we're late, deduce the time that's already elapsed from the next deadline. Make sure we don't ask for a deadline in the past
                //Log.e("GIF", "delay: " + currentFrame.delay + " now: " + now + " next deadline: " + nextFrameDeadline); // TODO: remove
                // We consumed a frame, prepare a new one to try to keep our cache
                frameProducerExecutor.execute(produceNextFrameRunnable);
            }
        }

        invalidateSelf();
    }

    @UiThread
    private void ranOutOfMemory() {
        // When we run out of memory, stop animating
        // Purge the cache too, but if there is no current frame try to pop one from the cache
        // Trigger an invalidation if we do that, so that draw() is called

        stop();
        if (currentFrame != null) {
            nextFrames.clear();
        } else {
            FrameInfo nextFrame = nextFrames.poll();
            nextFrames.clear();
            if (nextFrame != null) {
                currentFrame = nextFrame;
                invalidateSelf();
            }
        }
    }

    //endregion

    //region Drawable methods

    @Override
    public void draw(@NonNull Canvas canvas) {
        requestNewFrameIfNeeded();

        if (currentFrame != null) {
            canvas.drawBitmap(currentFrame.bitmap, null, getBounds(), paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        if (paint.getAlpha() < 255) {
            return PixelFormat.TRANSLUCENT;
        }
        return PixelFormat.OPAQUE;
    }

    @Override
    public int getIntrinsicHeight() {
        // Get the height from the bitmap if available, but otherwise get the one from the header
        if (currentFrame != null && currentFrame.bitmap != null) {
            return currentFrame.bitmap.getScaledHeight(dpi);
        }
        return gifDecoder.getHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        // Get the width from the bitmap if available, but otherwise scale the one from the header
        if (currentFrame != null && currentFrame.bitmap != null) {
            return currentFrame.bitmap.getScaledWidth(dpi);
        }
        return gifDecoder.getWidth();
    }

    //endregion

    //region Animatable methods

    @Override
    public void start() {
        if (ranOutOfMemory) {
            return;
        }
        animating = true;
        invalidateSelf();
    }

    @Override
    public void stop() {
        animating = false;
    }

    @Override
    public boolean isRunning() {
        return animating;
    }

    private static class FrameInfo {

        Bitmap bitmap;
        int delay;

        FrameInfo(Bitmap bitmap, int delay) {
            this.bitmap = bitmap;
            this.delay = delay;
        }
    }
}
