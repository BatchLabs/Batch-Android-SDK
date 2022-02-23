package com.batch.android.messaging.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import androidx.annotation.Nullable;

/**
 * CloseButton subclass that supports animating its progress
 */
public class AnimatedCloseButton extends CloseButton {

    private boolean animating = false;
    private long animationEndDate = 0L;
    private long duration = 0L;

    public AnimatedCloseButton(Context context) {
        super(context);
    }

    public AnimatedCloseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedCloseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused")
    public AnimatedCloseButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void animateForDuration(long durationMS) {
        animating = true;
        duration = durationMS;
        animationEndDate = AnimationUtils.currentAnimationTimeMillis() + durationMS;
        setCountdownProgress(1.0f);
        invalidate();
    }

    public boolean isAnimating() {
        return animating;
    }

    private void onAnimationFrame() {
        // Don't use setCountdownProgress as it instantly calls invalidate, and we want a gentler
        // approach
        long currentAnimationTime = AnimationUtils.currentAnimationTimeMillis();
        if (currentAnimationTime >= animationEndDate) {
            countdownProgress = 0;
            animating = false;
        } else {
            countdownProgress = ((float) animationEndDate - currentAnimationTime) / duration;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (animating) {
            onAnimationFrame();
        }
    }

    //region State restoration

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        AnimatedCountdownSavedState state = new AnimatedCountdownSavedState(super.onSaveInstanceState());
        state.duration = duration;
        state.animationEndDate = animationEndDate;
        state.animating = animating;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof AnimatedCountdownSavedState) {
            AnimatedCountdownSavedState savedState = (AnimatedCountdownSavedState) state;
            super.onRestoreInstanceState(savedState.getSuperState());
            duration = savedState.duration;
            animationEndDate = savedState.animationEndDate;
            animating = savedState.animating;
            if (animating) {
                postInvalidate();
            }
        } else {
            super.onRestoreInstanceState(state);
        }
    }
    //endregion
}
