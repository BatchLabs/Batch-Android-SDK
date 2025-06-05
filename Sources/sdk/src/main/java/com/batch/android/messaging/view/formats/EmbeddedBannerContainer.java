package com.batch.android.messaging.view.formats;

import static com.batch.android.module.MessagingModule.ACTION_DISMISS_BANNER;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.batch.android.BatchMessage;
import com.batch.android.MessagingAnalyticsDelegate;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.messaging.view.PannableBannerFrameLayout;
import com.batch.android.messaging.view.formats.mep.BannerView;
import com.batch.android.module.MessagingModule;
import com.google.android.material.appbar.AppBarLayout;

/**
 * Container for Batch's banners.
 * Handles everything related to the banner: its lifecycle, window insets, etc...
 * <p>
 * Kinda similar to <a href="https://android.googlesource.com/platform/frameworks/support.git/+/master/design/src/android/support/design/widget/BaseTransientBottomBar.java">...</a>
 */
public abstract class EmbeddedBannerContainer implements PannableBannerFrameLayout.OnDismissListener {

    /**
     * Duration of the animation in milliseconds.
     */
    protected static final int IN_OUT_ANIMATION_DURATION_MS = 300;

    /**
     * Context.
     */
    @NonNull
    protected Context context;

    /**
     * Messaging module.
     */
    @NonNull
    protected final MessagingModule messagingModule;

    /**
     * Analytics delegate.
     */
    @NonNull
    protected final MessagingAnalyticsDelegate analyticsDelegate;

    /**
     * Parent view.
     */
    @Nullable
    protected ViewGroup parentView;

    /**
     * Root view.
     */
    @NonNull
    protected BaseView rootView;

    /**
     * Banner view.
     */
    @NonNull
    protected RelativeLayout bannerView;

    /**
     * Payload message
     */
    @NonNull
    protected BatchMessage payloadMessage;

    /**
     * Pinned vertical edge.
     */
    @NonNull
    protected VerticalEdge pinnedVerticalEdge;

    /**
     * Whether the banner has been dismissed.
     */
    protected boolean alreadyDismissed = false;

    /**
     * Whether the banner has been shown.
     */
    protected boolean alreadyShown = false;

    /**
     * Token for the auto close handler.
     */
    protected Object autoCloseHandlerToken = new Object();

    /**
     * Dismiss receiver.
     */
    protected BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!alreadyDismissed && intent != null && ACTION_DISMISS_BANNER.equalsIgnoreCase(intent.getAction())) {
                dismissOnMainThread(true);
            }
        }
    };

    /**
     * Main thread handler.
     */
    protected Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Constructor.
     *
     * @param attachTo The view to attach the banner to
     * @param messagingModule The messaging module
     * @param analyticsDelegate The analytics delegate
     * @param payloadMessage The payload message
     * @param embed Whether the banner should be embedded
     */
    protected EmbeddedBannerContainer(
        @NonNull View attachTo,
        @NonNull MessagingModule messagingModule,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate,
        @NonNull BatchMessage payloadMessage,
        boolean embed
    ) {
        this.payloadMessage = payloadMessage;
        this.messagingModule = messagingModule;
        this.analyticsDelegate = analyticsDelegate;

        if (embed) {
            if (!(attachTo instanceof FrameLayout)) {
                throw new IllegalArgumentException("Banners cannot be embedded in views that are not FrameLayouts");
            }
            this.parentView = (FrameLayout) attachTo;
        } else {
            this.parentView = findBestParentView(attachTo);
        }
        if (this.parentView == null) {
            throw new IllegalArgumentException("Could not find any suitable view to attach the banner to");
        }

        this.context = parentView.getContext();
        this.rootView = new BaseView(context);
    }

    /**
     * Make the banner view.
     */
    protected void makeView() {
        // Allows swipe to dismiss translation to works correctly
        rootView.setClipChildren(false);
        rootView.setClipToPadding(false);

        rootView.setAccessibilityLiveRegion(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        rootView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        bannerView = makeBannerView();
        pinnedVerticalEdge = getPinnedVerticalEdge();

        rootView.setPannable(allowSwipeToDismiss());
        rootView.setDismissDirection(
            pinnedVerticalEdge == VerticalEdge.TOP
                ? PannableBannerFrameLayout.DismissDirection.TOP
                : PannableBannerFrameLayout.DismissDirection.BOTTOM
        );
        rootView.setDismissListener(this);

        ViewGroup.LayoutParams bannerLP = bannerView.getLayoutParams();
        // BannerView should already come with FrameLayout Params
        // It's tight coupling, but we avoid parsing the rules twice or communicating them
        if (!(bannerLP instanceof FrameLayout.LayoutParams)) {
            bannerLP =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        ((FrameLayout.LayoutParams) bannerLP).gravity = layoutGravityForPinnedEdge();
        rootView.addView(bannerView, bannerLP);
    }

    /**
     * Make the banner view.
     * @return The banner view
     */
    protected abstract RelativeLayout makeBannerView();

    /**
     * Whether the banner can be dismissed by swiping down/up according to the edge.
     * @return True if the banner can be dismissed
     */
    protected abstract boolean allowSwipeToDismiss();

    /**
     * Get the edge that the banner is pinned to.
     * @return The edge that the banner is pinned to
     */
    protected abstract VerticalEdge getPinnedVerticalEdge();

    /**
     * Schedule the auto close.
     */
    protected abstract void scheduleAutoClose();

    /**
     * Unschedule the auto close.
     */
    protected void unscheduleAutoClose() {
        mainThreadHandler.removeCallbacksAndMessages(autoCloseHandlerToken);
    }

    /**
     * Show the banner.
     */
    public void show() {
        if (alreadyShown) {
            return;
        }
        alreadyShown = true;
        int edgeGravity = layoutGravityForPinnedEdge();

        // Make the root frame layout fill the whole container if not in a coordinatorlayout
        // Otherwise swipe to dismiss will clip incorrectly due to the translation Y
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );

        if (parentView instanceof FrameLayout) {
            lp = new FrameLayout.LayoutParams(lp);

            ((FrameLayout.LayoutParams) lp).gravity = edgeGravity;
        } else if (ReflectionHelper.isInstanceOfCoordinatorLayout(parentView)) {
            try {
                final CoordinatorLayout.LayoutParams clp = new CoordinatorLayout.LayoutParams(lp);
                if (pinnedVerticalEdge == VerticalEdge.TOP) {
                    clp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
                }
                clp.gravity = edgeGravity;
                clp.insetEdge = edgeGravity;
                lp = clp;
            } catch (NoClassDefFoundError ignored) {
                Log.e(
                    "Messaging",
                    "Could not show banner: CoordinatorLayout.LayoutParams or AppBarLayout.ScrollingViewBehavior are not available."
                );
                return;
            }
        }

        rootView.setAlpha(0);
        parentView.addView(rootView, lp);
        if (bannerView instanceof BannerView) {
            ((BannerView) bannerView).onShown();
        }
        analyticsDelegate.onViewShown();

        final ObjectAnimator anim = ObjectAnimator.ofFloat(rootView, "alpha", 0, 1);
        anim.setDuration(IN_OUT_ANIMATION_DURATION_MS);
        anim.start();

        LocalBroadcastManager
            .getInstance(context)
            .registerReceiver(dismissReceiver, new IntentFilter(ACTION_DISMISS_BANNER));

        // Make sure we unregister the broadcast receiver at some point, or we will leak memory
        bannerView.addOnAttachStateChangeListener(
            new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {}

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(dismissReceiver);
                    unscheduleAutoClose();
                }
            }
        );
        scheduleAutoClose();
    }

    /**
     * Dismiss the banner.
     *
     * @param animated Whether the dismiss should be animated
     */
    public void dismiss(boolean animated) {
        if (alreadyDismissed) {
            return;
        }
        alreadyDismissed = true;

        unscheduleAutoClose();

        if (animated) {
            final ObjectAnimator anim = ObjectAnimator.ofFloat(rootView, "alpha", 1, 0);
            anim.setDuration(IN_OUT_ANIMATION_DURATION_MS);
            anim.addListener(
                new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {}

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        removeFromParent();
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        removeFromParent();
                    }

                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {}
                }
            );
            anim.start();
        } else {
            removeFromParent();
        }

        // We also unregister this in onViewDetachedFromWindow, but we might leak if the view is dismissed
        // without having ever been attached to a window
        LocalBroadcastManager.getInstance(context).unregisterReceiver(dismissReceiver);
    }

    /**
     * Remove the banner from the parent view.
     */
    private void removeFromParent() {
        final ViewParent parent = rootView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(rootView);
            analyticsDelegate.onViewDismissed();
        }
    }

    /**
     * Find the best parent view to attach the banner to.
     *
     * @param view The view to start from
     * @return The best parent view to attach the banner to
     */
    @Nullable
    protected ViewGroup findBestParentView(@Nullable View view) {
        // Kinda looks like Snackbar's implementation but heh, we're doing the same thing :)
        // Fallback is used if we can't find a coordinatorlayout, or android.R.id.content
        FrameLayout fallback = null;

        while (view != null) {
            if (ReflectionHelper.isInstanceOfCoordinatorLayout(view)) {
                return (ViewGroup) view;
            } else if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return (FrameLayout) view;
                } else {
                    // Keep it just in case we really can't find anything better
                    // Highly unlikely that we can't get back to android.R.id.content though
                    fallback = (FrameLayout) view;
                }
            }

            final ViewParent parent = view.getParent();
            view = (parent instanceof View) ? (View) parent : null;
        }

        return fallback;
    }

    /**
     * Get the gravity for the banner view according to the pinned edge.
     *
     * @return The gravity for the banner view
     */
    protected int layoutGravityForPinnedEdge() {
        return pinnedVerticalEdge == VerticalEdge.TOP ? Gravity.TOP : Gravity.BOTTOM;
    }

    protected void performAutoClose() {
        if (!alreadyDismissed) {
            dismiss(true);
            analyticsDelegate.onAutoClosedAfterDelay();
        }
    }

    public void dismissOnMainThread(final boolean animated) {
        mainThreadHandler.post(() -> dismiss(animated));
    }

    @Override
    public void onDismiss(PannableBannerFrameLayout layout) {
        if (!alreadyDismissed) {
            dismiss(false);
            analyticsDelegate.onClosed();
        }
    }

    public enum VerticalEdge {
        TOP,
        BOTTOM,
    }

    /**
     * Root view for Banners.
     * Handles fitting to system windows, so that it can dodge the status bar and navigation bar
     */
    public static class BaseView extends PannableBannerFrameLayout {

        public BaseView(Context context) {
            super(context);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            ViewCompat.requestApplyInsets(this);
        }
    }
}
