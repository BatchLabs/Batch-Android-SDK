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
import android.os.SystemClock;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.batch.android.BatchMessage;
import com.batch.android.MessagingAnalyticsDelegate;
import com.batch.android.core.Logger;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.BaseBannerMessage.CTADirection;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.view.PannableBannerFrameLayout;
import com.batch.android.messaging.view.formats.BannerView.VerticalEdge;
import com.batch.android.messaging.view.helper.ImageHelper;
import com.batch.android.module.MessagingModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.google.android.material.appbar.AppBarLayout;

/**
 * Container for Batch's banners.
 * Handles everything related to the banner: its lifecycle, window insets, etc...
 * <p>
 * Kinda similar to https://android.googlesource.com/platform/frameworks/support.git/+/master/design/src/android/support/design/widget/BaseTransientBottomBar.java
 */
@Module
public class EmbeddedBannerContainer
    implements BannerView.OnActionListener, PannableBannerFrameLayout.OnDismissListener, ImageHelper.Cache {

    private static final int IN_OUT_ANIMATION_DURATION_MS = 300;

    private Context context;

    private ViewGroup parentView;

    private BatchMessage payloadMessage;

    private BannerMessage message;

    private BaseView rootView;

    private BannerView bannerView;

    private VerticalEdge pinnedVerticalEdge;

    private boolean alreadyShown = false;

    private boolean alreadyDismissed = false;

    private final MessagingModule messagingModule;
    private final MessagingAnalyticsDelegate analyticsDelegate;

    private LruCache<String, AsyncImageDownloadTask.Result> imageCache;

    private BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!alreadyDismissed && intent != null && ACTION_DISMISS_BANNER.equalsIgnoreCase(intent.getAction())) {
                dismissOnMainThread(true);
            }
        }
    };

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private Object autoCloseHandlerToken = new Object();

    @Provide
    public static EmbeddedBannerContainer provide(
        @NonNull View attachTo,
        @NonNull BatchMessage payloadMessage,
        @NonNull BannerMessage message,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate,
        boolean embed
    ) {
        return new EmbeddedBannerContainer(
            MessagingModuleProvider.get(),
            attachTo,
            payloadMessage,
            message,
            analyticsDelegate,
            embed
        );
    }

    private EmbeddedBannerContainer(
        @NonNull MessagingModule messagingModule,
        @NonNull View attachTo,
        @NonNull BatchMessage payloadMessage,
        @NonNull BannerMessage message,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate,
        boolean embed
    ) {
        super();
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
        this.payloadMessage = payloadMessage;
        this.message = message;
        // We have max 1 image on a banner
        this.imageCache = new LruCache<>(1);

        pinnedVerticalEdge = message.ctaDirection == CTADirection.VERTICAL ? VerticalEdge.BOTTOM : VerticalEdge.TOP;

        rootView = new BaseView(context);

        // Allows swipe to dismiss translation to works correctly
        rootView.setClipChildren(false);
        rootView.setClipToPadding(false);

        ViewCompat.setAccessibilityLiveRegion(rootView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        ViewCompat.setImportantForAccessibility(rootView, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        rootView.setFitsSystemWindows(true);

        bannerView = makeBannerView();
        pinnedVerticalEdge = bannerView.getPinnedVerticalEdge();

        rootView.setPannable(message.allowSwipeToDismiss);
        rootView.setDismissDirection(
            pinnedVerticalEdge == VerticalEdge.TOP
                ? PannableBannerFrameLayout.DismissDirection.TOP
                : PannableBannerFrameLayout.DismissDirection.BOTTOM
        );
        rootView.setDismissListener(this);

        LayoutParams bannerLP = bannerView.getLayoutParams();
        // BannerView should already come with FrameLayout Params
        // It's tight coupling, but we avoid parsing the rules twice or communicating them
        if (!(bannerLP instanceof FrameLayout.LayoutParams)) {
            bannerLP = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        ((FrameLayout.LayoutParams) bannerLP).gravity = layoutGravityForPinnedEdge();
        rootView.addView(bannerView, bannerLP);

        ReflectionHelper.optOutOfDarkModeRecursively(rootView);
    }

    @Nullable
    private ViewGroup findBestParentView(View view) {
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

    @NonNull
    private BannerView makeBannerView() {
        BannerView v = new BannerView(this.context, this.message, null, new DOMNode("root"), this);
        v.setActionListener(this);
        return v;
    }

    public void show() {
        if (alreadyShown) {
            return;
        }

        alreadyShown = true;

        int edgeGravity = layoutGravityForPinnedEdge();

        // Make the root frame layout fill the whole container if not in a corrdinatorlayout
        // Otherwise swipe to dismiss will clip incorrectly due to the translation Y
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

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
        bannerView.onShown();

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
                public void onViewAttachedToWindow(View v) {}

                @Override
                public void onViewDetachedFromWindow(View v) {
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(dismissReceiver);
                    unscheduleAutoClose();
                }
            }
        );

        scheduleAutoClose();
    }

    private void scheduleAutoClose() {
        if (bannerView != null) {
            if (message.autoCloseDelay > 0 && bannerView.canAutoClose()) {
                bannerView.startAutoCloseCountdown();
                long when = SystemClock.uptimeMillis() + message.autoCloseDelay;
                mainThreadHandler.postAtTime(this::performAutoClose, autoCloseHandlerToken, when);
            }
        }
    }

    private void unscheduleAutoClose() {
        mainThreadHandler.removeCallbacksAndMessages(autoCloseHandlerToken);
    }

    private void performAutoClose() {
        if (!alreadyDismissed) {
            dismiss(true);
            analyticsDelegate.onAutoClosedAfterDelay();
        }
    }

    public void dismissOnMainThread(final boolean animated) {
        mainThreadHandler.post(() -> dismiss(animated));
    }

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
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        removeFromParent();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        removeFromParent();
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
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

    private void removeFromParent() {
        final ViewParent parent = rootView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(rootView);
            analyticsDelegate.onViewDismissed();
        }
    }

    private int layoutGravityForPinnedEdge() {
        return pinnedVerticalEdge == VerticalEdge.TOP ? Gravity.TOP : Gravity.BOTTOM;
    }

    @Override
    public void onCloseAction() {
        dismiss(true);
        analyticsDelegate.onClosed();
    }

    @Override
    public void onCTAAction(int index, @NonNull CTA cta) {
        dismiss(true);
        analyticsDelegate.onCTAClicked(index, cta);
        messagingModule.performAction(context, payloadMessage, cta);
    }

    @Override
    public void onGlobalAction() {
        dismiss(true);
        analyticsDelegate.onGlobalTap(message.globalTapAction);
        if (message.globalTapAction != null) {
            messagingModule.performAction(context, payloadMessage, message.globalTapAction);
        } else {
            Logger.internal(MessagingModule.TAG, "Could not perform global tap action. Internal error.");
        }
    }

    @Override
    public void onDismiss(PannableBannerFrameLayout layout) {
        if (!alreadyDismissed) {
            dismiss(false);
            analyticsDelegate.onClosed();
        }
    }

    @Override
    public void put(@NonNull AsyncImageDownloadTask.Result result) {
        imageCache.put(result.getKey(), result);
    }

    @Nullable
    @Override
    public AsyncImageDownloadTask.Result get(@NonNull String key) {
        return imageCache.get(key);
    }

    /**
     * Root view for Banners. Handles fitting to system windows, so that it can dodge the statusbar and navigation bar
     */
    static class BaseView extends PannableBannerFrameLayout {

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
