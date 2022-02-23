package com.batch.android.messaging.view;

import android.view.MotionEvent;
import androidx.annotation.Nullable;

/**
 * Describes a ViewGroup that can delegate its touch event handling
 */
public interface DelegatedTouchEventViewGroup {
    /**
     * Calls super's superOnInterceptTouchEvent and forwards the returned value
     */
    boolean superOnInterceptTouchEvent(MotionEvent ev);

    /**
     * Calls super's onTouchEvent and forwards the returned value
     */
    boolean superOnTouchEvent(MotionEvent ev);

    void setTouchEventDelegate(@Nullable Delegate delegate);

    interface Delegate {
        /**
         * Delegated {@link android.view.ViewGroup#onInterceptTouchEvent(MotionEvent)}
         * <p>
         * Works exactly like the original method, this completly takes over it so make sure
         * you implement it correctly
         */
        boolean onInterceptTouchEvent(MotionEvent ev, DelegatedTouchEventViewGroup source);

        /**
         * Delegated {@link android.view.View#onTouchEvent(MotionEvent)}
         * <p>
         * Works exactly like the original method, this completly takes over it so make sure
         * you implement it correctly
         * <p>
         * Views can ask to receive a "fake" cancellation touch event when the delegate starts acting
         * on touches (like responding to a pan action)
         * This is useful if your view itself handles a touch event (like a tap) and not a subview
         * as this case is NOT handled by onInterceptTouchEvent
         */
        boolean onTouchEvent(
            MotionEvent event,
            DelegatedTouchEventViewGroup source,
            boolean wantsCancellationOnInterception
        );
    }
}
