package com.batch.android.messaging;

import static android.content.Context.VIBRATOR_SERVICE;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.batch.android.messaging.view.DelegatedTouchEventViewGroup;

public class ModalContentPanGestureDetector
    implements GestureDetector.OnGestureListener, DelegatedTouchEventViewGroup.Delegate {

    private static final long ANIMATION_DURATION = 500;
    private static final long ANIMATION_DURATION_FAST = 200;
    private static final float TRANSLATION_PAN_MULTIPLIER = 0.4f;
    private static final float SCALE_PAN_MULTIPLIER = 0.0002f;
    private static final float DISMISSABLE_TARGET_ALPHA = 0.6f;
    private static final float DISMISS_THRESHOLD_MINIMUM_VELOCITY = 1500f;
    private static final float SMALLEST_SCALE_RATIO = 0.85f;
    private static final float SCALE_RATIO_DISMISS_THRESHOLD = 0.96f;
    private static final float SPRING_STIFFNESS = 800.0f;

    private OnDismissListener dismissListener;

    private GestureDetector detector;

    private View targetView;

    private Vibrator vibrator;

    private boolean supportsAndroidXAnimation;

    /**
     * initialSwipeXOffset is used to store the initial motion event X,
     * so that swipes are relative to the touch movement
     * rather than relative to the view
     * <p>
     * Also used to compute if the touch slop is satisfied
     */
    private float initialSwipeXOffset = 0;

    /**
     * initialSwipeYOffset is used to store the initial motion event Y,
     * so that swipes are relative to the touch movement
     * rather than relative to the view
     * <p>
     * Also used to compute if the touch slop is satisfied
     */
    private float initialSwipeYOffset = 0;

    /**
     * initialInterceptXOffset is used to store the initial motion event X for touch interception
     * calculation, to ensure touch slop
     */
    private float initialInterceptXOffset = 0;

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
    private Object[] cancellationAnimations;

    private boolean isPanning;

    /**
     * Scaled touch slop for view.
     * The touch slop is the distance a user is allowed to move their finger before the touch starts being registered as something else than a tap
     */
    private int touchSlop;

    /**
     * Should the view be dismissed once released
     */
    private boolean shouldDismissOnTouchUp;

    /**
     * Should the view be allowed to pan horizontally
     */
    private boolean allowHorizontalPanning;

    public ModalContentPanGestureDetector(Context context, boolean allowHorizontalPanning) {
        detector = new GestureDetector(context, this);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        shouldDismissOnTouchUp = false;
        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        try {
            Class.forName("androidx.dynamicanimation.animation.DynamicAnimation");
            Class.forName("androidx.dynamicanimation.animation.SpringAnimation");
            Class.forName("androidx.dynamicanimation.animation.SpringForce");
            supportsAndroidXAnimation = true;
        } catch (ClassNotFoundException ignored) {
            supportsAndroidXAnimation = false;
        }
        this.allowHorizontalPanning = allowHorizontalPanning;
    }

    /**
     * Attach a delegated view group to this detector.
     * The
     *
     * @param view        ViewGroup to make panable
     * @param effectsView View to apply the visual effects on
     */
    public void attach(DelegatedTouchEventViewGroup view, View effectsView) {
        view.setTouchEventDelegate(this);
        targetView = effectsView;
    }

    public void setDismissListener(OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void dismiss() {
        if (dismissListener != null) {
            dismissListener.onPanDismiss();
        }
    }

    private boolean hasPassedTouchSlop(float var, float initialVar) {
        return Math.abs(var - initialVar) > touchSlop;
    }

    private void beginPan(float x, float y) {
        shouldDismissOnTouchUp = false;
        initialSwipeXOffset = x;
        initialSwipeYOffset = y;
        cancelCancellationAnimation();
    }

    private void startCancelAnimation() {
        if (supportsAndroidXAnimation) {
            // We want a stiffnes between medium and low
            SpringForce translationSpring = new SpringForce(0)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SPRING_STIFFNESS);

            SpringForce scaleSpring = new SpringForce(1)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SPRING_STIFFNESS);

            SpringAnimation translationAnimX = new SpringAnimation(targetView, DynamicAnimation.TRANSLATION_X, 0);
            translationAnimX.setSpring(translationSpring);

            SpringAnimation translationAnimY = new SpringAnimation(targetView, DynamicAnimation.TRANSLATION_Y, 0);
            translationAnimY.setSpring(translationSpring);

            new SpringAnimation(targetView, DynamicAnimation.SCALE_X, 1).setSpring(scaleSpring).start();
            new SpringAnimation(targetView, DynamicAnimation.SCALE_Y, 1).setSpring(scaleSpring).start();

            cancellationAnimations = new Object[] { translationAnimX, translationAnimY };
            translationAnimX.start();
            translationAnimY.start();
        } else {
            startFallbackCancelAnimation();
        }
    }

    private void startFallbackCancelAnimation() {
        PropertyValuesHolder pvtX = PropertyValuesHolder.ofFloat("translationX", targetView.getTranslationY(), 0);
        PropertyValuesHolder pvtY = PropertyValuesHolder.ofFloat("translationY", targetView.getTranslationY(), 0);
        PropertyValuesHolder pvsX = PropertyValuesHolder.ofFloat("scaleX", targetView.getScaleX(), 1f);
        PropertyValuesHolder pvsY = PropertyValuesHolder.ofFloat("scaleY", targetView.getScaleY(), 1f);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(targetView, pvtX, pvtY, pvsX, pvsY);
        animator.setInterpolator(new OvershootInterpolator());
        animator.setDuration(ANIMATION_DURATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            animator.setAutoCancel(true);
        }
        animator.start();
        cancellationAnimations = new Object[] { animator };
    }

    private void cancelCancellationAnimation() {
        if (cancellationAnimations == null) {
            return;
        }

        for (Object animation : cancellationAnimations) {
            if (animation instanceof ValueAnimator) {
                ((ValueAnimator) animation).cancel();
            } else if (supportsAndroidXAnimation && animation instanceof DynamicAnimation) {
                ((DynamicAnimation) animation).cancel();
            }
        }
        cancellationAnimations = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev, DelegatedTouchEventViewGroup source) {
        // Detect if the user is panning the view, so that it's possible even when trying to do so on a button

        final int action = ev.getAction();

        // Touch complete: give control back
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isPanning = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                initialInterceptXOffset = ev.getX();
                initialInterceptYOffset = ev.getY();
            case MotionEvent.ACTION_MOVE:
                {
                    if (isPanning) {
                        return true;
                    }

                    // Use the initial offset, so that we also reach the touch slop in the standard
                    // touch event handler
                    if (hasPassedTouchSlop(ev.getY(), initialInterceptYOffset)) {
                        beginPan(initialInterceptXOffset, initialInterceptYOffset);
                        return true;
                    }

                    if (allowHorizontalPanning && hasPassedTouchSlop(ev.getX(), initialInterceptXOffset)) {
                        beginPan(initialInterceptXOffset, initialInterceptYOffset);
                        return true;
                    }

                    break;
                }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(
        MotionEvent event,
        DelegatedTouchEventViewGroup source,
        boolean wantsCancellationOnInterception
    ) {
        if (!isPanning) {
            source.superOnTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                beginPan(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                {
                    float x = event.getX();
                    float y = event.getY();
                    if (!isPanning) {
                        boolean hasPassedSlopX = hasPassedTouchSlop(x, initialSwipeXOffset);
                        boolean hasPassedSlopY = hasPassedTouchSlop(y, initialSwipeYOffset);
                        if (!hasPassedSlopX && !hasPassedSlopY) {
                            break;
                        } else {
                            isPanning = true;

                            // Send a cancellation event if we have a foreground drawable
                            // and the delegate intercepted the push
                            // This makes the ripple disappear when something like a pan happens
                            if (wantsCancellationOnInterception) {
                                final MotionEvent cancelEvent = MotionEvent.obtain(event);
                                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                                source.superOnTouchEvent(cancelEvent);
                            }

                            // Avoid view suddenly jumping after reaching the touch slop
                            // Offset the value in the right swiping direction, or we double the unwanted effect

                            if (hasPassedSlopX) {
                                if (x < initialSwipeXOffset) {
                                    initialSwipeXOffset -= touchSlop;
                                } else {
                                    initialSwipeXOffset += touchSlop;
                                }
                            }

                            if (hasPassedSlopY) {
                                if (y < initialSwipeYOffset) {
                                    initialSwipeYOffset -= touchSlop;
                                } else {
                                    initialSwipeYOffset += touchSlop;
                                }
                            }
                        }
                    }

                    // Hard to properly explain why we do it that way
                    // But it ensures that the view does not jump around while being dragged

                    // Offset the location of the event with our translation, so that the GestureDetector properly detects velocity
                    // (or will at least try to)
                    event.offsetLocation(targetView.getTranslationX(), targetView.getTranslationY());

                    // No need to add the original translation, as offsetLocation already did it
                    float translationX = event.getX() - initialSwipeXOffset;
                    float translationY = event.getY() - initialSwipeYOffset;
                    float scaleRatio = Math.min(
                        1,
                        Math.max(SMALLEST_SCALE_RATIO, 1 + (-1 * Math.abs(translationY) * SCALE_PAN_MULTIPLIER))
                    );
                    if (Float.isNaN(scaleRatio)) {
                        break;
                    }

                    if (scaleRatio <= SCALE_RATIO_DISMISS_THRESHOLD) {
                        if (!shouldDismissOnTouchUp) {
                            shouldDismissOnTouchUp = true;
                            shouldDismissChanged();
                        }
                    } else {
                        if (shouldDismissOnTouchUp) {
                            shouldDismissOnTouchUp = false;
                            shouldDismissChanged();
                        }
                    }

                    if (allowHorizontalPanning) {
                        targetView.setTranslationX(translationX * TRANSLATION_PAN_MULTIPLIER);
                    }
                    targetView.setTranslationY(translationY * TRANSLATION_PAN_MULTIPLIER);
                    targetView.setScaleX(scaleRatio);
                    targetView.setScaleY(scaleRatio);
                    break;
                }
            case MotionEvent.ACTION_UP:
                {
                    isPanning = false;

                    if (shouldDismissOnTouchUp) {
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

    private void shouldDismissChanged() {
        targetView
            .animate()
            .alpha(shouldDismissOnTouchUp ? DISMISSABLE_TARGET_ALPHA : 1)
            .setDuration(ANIMATION_DURATION_FAST)
            .start();
        vibrate();
    }

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(25);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
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
        velocityY = Math.abs(velocityY);

        if (velocityY > DISMISS_THRESHOLD_MINIMUM_VELOCITY) {
            dismiss();
        }

        if (allowHorizontalPanning) {
            velocityX = Math.abs(velocityX);

            if (velocityX > DISMISS_THRESHOLD_MINIMUM_VELOCITY) {
                dismiss();
            }
        }
        return true;
    }

    /**
     * Dismiss listener
     */
    public interface OnDismissListener {
        void onPanDismiss();
    }
}
