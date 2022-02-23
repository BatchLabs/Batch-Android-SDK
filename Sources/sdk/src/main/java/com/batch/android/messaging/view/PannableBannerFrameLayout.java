package com.batch.android.messaging.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import java.lang.reflect.Method;

/**
 * Pannable layout. Allows a layout to be swiped up and down, and dismissed by reaching a threshold,
 * or by flinging the view swiftly.
 * <p>
 * You can make any layout pannable simply by changing what this class extends
 */
public class PannableBannerFrameLayout extends FrameLayout implements GestureDetector.OnGestureListener {

    private static final int FLING_VELOCITY_DISMISS_THRESHOLD = 1500;

    // Specifies how much of the view's height has to have been panned to trigger a dismiss
    private static final float PAN_HEIGHT_DISMISS_RATIO_THRESHOLD = 0.5f;

    private boolean supportsAndroidXAnimation;

    private GestureDetector detector;

    private DismissDirection dismissDirection;

    private OnDismissListener dismissListener;

    private boolean isPannable;

    /**
     * initialSwipeYOffset is used to store the initial motion event Y,
     * so that swipes are relative to the touch movement
     * rather than relative to the view
     * <p>
     * Also used to compute if the touch slop is satisfied
     */
    private float initialSwipeYOffset = 0;

    /**
     * initialInterceptYOffset is used to store the initial motion event Y for touch interception
     * calculation, to ensure touch slop
     */
    private float initialInterceptYOffset = 0;

    /**
     * Store the cancellation animation so that we can cancel it
     * The cancellation animation is the one translating the view back
     * to its original position once the swipe has been cancelled
     */
    private Object cancellationAnimation;

    private boolean isPanning;

    /**
     * Cancellation animation duration (ms)
     */
    private int cancellationAnimationDuration;

    /**
     * Dismiss animation duration (ms)
     */
    private int dismissAnimationDuration;

    /**
     * Scaled touch slop for view.
     * The touch slop is the distance a user is allowed to move their finger before the touch starts being registered as something else than a tap
     */
    private int touchSlop;

    public PannableBannerFrameLayout(Context context) {
        super(context);
        setup();
    }

    public PannableBannerFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PannableBannerFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    public void setup() {
        detector = new GestureDetector(getContext(), this);
        dismissDirection = DismissDirection.TOP;
        cancellationAnimationDuration = 500;
        dismissAnimationDuration = 100;
        isPanning = false;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        dismissListener = null;

        try {
            Class.forName("androidx.dynamicanimation.animation.DynamicAnimation");
            Class.forName("androidx.dynamicanimation.animation.SpringAnimation");
            Class.forName("androidx.dynamicanimation.animation.SpringForce");
            supportsAndroidXAnimation = true;
        } catch (ClassNotFoundException ignored) {
            supportsAndroidXAnimation = false;
        }
    }

    public void setDismissDirection(DismissDirection dismissDirection) {
        this.dismissDirection = dismissDirection;
    }

    public void setDismissListener(OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void setPannable(boolean pannable) {
        isPannable = pannable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isPannable) {
            return super.onInterceptTouchEvent(ev);
        }
        // Detect if the user is panning the view, so that it's possible even when trying to do so on a button

        final int action = ev.getAction();

        // Touch complete: give control back
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isPanning = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialInterceptYOffset = ev.getY();
            case MotionEvent.ACTION_MOVE:
                {
                    if (isPanning) {
                        return true;
                    }

                    if (hasYPassedTouchSlop(ev.getY(), initialInterceptYOffset)) {
                        // Use the initial offset, so that we also reach the touch slop in the standard
                        // touch event handler
                        beginPan(initialInterceptYOffset);
                        return true;
                    }
                    break;
                }
        }

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    // super.onTouchEvent() will call perform click, I don't know why this warning is so annoying
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isPannable) {
            return super.onTouchEvent(event);
        }
        //Log.i("SwipeLayout", event.getAction() + " Translation Y: " + getTranslationY() + " event Y: " + event.getY() + " pointer: " + event.getPointerId(0) +" initial: " + initialSwipeYOffset);

        super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                beginPan(event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                {
                    float y = event.getY();
                    if (!isPanning) {
                        if (!hasYPassedTouchSlop(y, initialSwipeYOffset)) {
                            break;
                        } else {
                            isPanning = true;
                            // Avoid view suddenly jumping after reaching the touch slop
                            // Offset the value in the right swiping direction, or we double the unwanted effect
                            if (y < initialSwipeYOffset) {
                                initialSwipeYOffset -= touchSlop;
                            } else {
                                initialSwipeYOffset += touchSlop;
                            }
                        }
                    }

                    // Hard to properly explain why we do it that way
                    // But it ensures that the view does not jump around while being dragged

                    // Offset the location of the event with our translation, so that the GestureDetector properly detects velocity
                    // (or will at least try to)
                    event.offsetLocation(0, getTranslationY());

                    // No need to add the original translation, as offsetLocation already did it
                    float translationY = event.getY() - initialSwipeYOffset;
                    // Switch this if the view is anchored to top
                    if (
                        (dismissDirection == DismissDirection.BOTTOM && translationY < 0) ||
                        (dismissDirection == DismissDirection.TOP && translationY > 0)
                    ) {
                        translationY *= 0.25;
                    }
                    setTranslationY(translationY);
                    break;
                }
            case MotionEvent.ACTION_UP:
                {
                    isPanning = false;

                    float translationY = getTranslationY();
                    if (dismissDirection == DismissDirection.TOP) {
                        translationY *= -1;
                    }

                    if (translationY > (getHeight() * PAN_HEIGHT_DISMISS_RATIO_THRESHOLD)) {
                        dismiss();
                    } else {
                        startCancelAnimation();
                    }
                    break;
                }
            case MotionEvent.ACTION_CANCEL:
                isPanning = false;
                startCancelAnimation();
                break;
        }

        this.detector.onTouchEvent(event);

        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        //Log.i("SwipeLayout", "onDown");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {}

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {}

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Log.i("SwipeLayout", "onFling Y: " + velocityY);

        // Disregard the fling if the view is translated in the wrong direction
        // because the fling velocity is sometimes the opposite of what it should be
        float translationY = getTranslationY();
        if (
            (dismissDirection == DismissDirection.TOP && translationY > 0) ||
            (dismissDirection == DismissDirection.BOTTOM && translationY < 0)
        ) {
            //Log.i("SwipeLayout", "ignore");
            return true;
        }

        if (dismissDirection == DismissDirection.TOP) {
            velocityY *= -1;
        }

        if (velocityY > FLING_VELOCITY_DISMISS_THRESHOLD) {
            dismiss();
        }

        return true;
    }

    private boolean hasYPassedTouchSlop(float y, float initialY) {
        return Math.abs(y - initialY) > touchSlop;
    }

    private void beginPan(float y) {
        initialSwipeYOffset = y;
        cancelCancellationAnimation();
    }

    private void startCancelAnimation() {
        if (supportsAndroidXAnimation) {
            SpringForce spring = new SpringForce(0)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_LOW);
            SpringAnimation anim = new SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0);
            anim.setSpring(spring);
            anim.start();
            cancellationAnimation = anim;
        } else {
            startFallbackCancelAnimation();
        }
    }

    private void startFallbackCancelAnimation() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), 0);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(cancellationAnimationDuration);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            animator.setAutoCancel(true);
        }
        animator.start();
        cancellationAnimation = animator;
    }

    private void cancelCancellationAnimation() {
        if (cancellationAnimation instanceof ValueAnimator) {
            ((ValueAnimator) cancellationAnimation).cancel();
            cancellationAnimation = null;
        } else if (supportsAndroidXAnimation && cancellationAnimation instanceof DynamicAnimation) {
            ((DynamicAnimation) cancellationAnimation).cancel();
            cancellationAnimation = null;
        }
    }

    public void dismiss() {
        cancelCancellationAnimation();

        float endTranslationY = getHeight();
        if (dismissDirection == DismissDirection.TOP) {
            endTranslationY *= -1;
        }

        // TODO: maybe force it even if animations are disabled
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY", getTranslationY(), endTranslationY);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(dismissAnimationDuration);
        animator.addListener(
            new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (dismissListener != null) {
                        dismissListener.onDismiss(PannableBannerFrameLayout.this);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}
            }
        );
        // We need our animation to be independent of the system's animation scale value
        try {
            Class<?> c = Class.forName("android.animation.ValueAnimator");
            Method m = c.getMethod("setDurationScale", float.class);
            m.invoke(null, 1f);
        } catch (Throwable t) {
            // Welp. Too bad.
        }

        animator.start();
    }

    /**
     * Vertical direction in which dismissal should be allowed
     */
    public enum DismissDirection {
        TOP,
        BOTTOM,
    }

    /**
     * Dismiss listener
     */
    public interface OnDismissListener {
        void onDismiss(PannableBannerFrameLayout layout);
    }
}
