package com.batch.android.messaging.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.styled.Styleable;
import java.util.Map;

/**
 * Countdown progress view. Given a duration and color, will progressively shrink until gone
 */
public class CountdownView extends ProgressBar implements Styleable {

    private static final String TAG = "CountdownView";
    private static final int MAX_PROGRESS = 1000;

    private boolean animating = false;
    private long animationEndDate = 0L;
    private long duration = 0L;

    public CountdownView(Context context) {
        super(context, null, android.R.style.Widget_ProgressBar_Horizontal);
        setIndeterminate(false);
        setMax(MAX_PROGRESS);
        setProgress(MAX_PROGRESS);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        StyleHelper.applyCommonRules(this, rules);

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("color".equalsIgnoreCase(rule.getKey())) {
                try {
                    setColor(Color.parseColor(rule.getValue()));
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable color (" + rule.getValue() + ")", e);
                }
            }
        }
    }

    public void animateForDuration(long durationMS) {
        animating = true;
        duration = durationMS;
        animationEndDate = AnimationUtils.currentAnimationTimeMillis() + durationMS;
        setProgress(MAX_PROGRESS);
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
            setProgress(0);
            animating = false;
        } else {
            setProgress((int) ((((float) animationEndDate - currentAnimationTime) / duration) * MAX_PROGRESS));
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

    /**
     * Set the progress color
     */
    @SuppressLint("RtlHardcoded")
    public void setColor(@ColorInt int color) {
        ShapeDrawable d = new ShapeDrawable(new RectShape());
        d.getPaint().setColor(color);
        setProgressDrawable(new ClipDrawable(d, Gravity.LEFT, ClipDrawable.HORIZONTAL));
        invalidate();
    }

    //region State restoration

    @Nullable
    @Override
    public Parcelable onSaveInstanceState() {
        AnimatedCountdownSavedState state = new AnimatedCountdownSavedState(super.onSaveInstanceState());
        state.duration = duration;
        state.animationEndDate = animationEndDate;
        state.animating = animating;
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
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
