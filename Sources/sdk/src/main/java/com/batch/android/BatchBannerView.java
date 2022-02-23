package com.batch.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.EmbeddedBannerContainerProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.view.formats.EmbeddedBannerContainer;
import com.batch.android.module.MessagingModule;

/**
 * BatchBannerView handles operations related to a Batch In-App Messaging banner, when used in manual mode.
 * <p>
 * It is not actually a view that you can directly add to your hierarchy. You will have to use {@link #show(View)} and {@link #dismiss(boolean)} to control how it shows up.
 */
@PublicSDK
public class BatchBannerView {

    private BatchMessage rawMessage;

    private BannerMessage message;

    private EmbeddedBannerContainer shownContainer;

    private boolean shown = false;

    private final MessagingAnalyticsDelegate analyticsDelegate;

    BatchBannerView(
        @NonNull BatchMessage rawMessage,
        @NonNull BannerMessage message,
        @NonNull MessagingAnalyticsDelegate analyticsDelegate
    ) {
        this.rawMessage = rawMessage;
        this.message = message;
        this.analyticsDelegate = analyticsDelegate;
    }

    /**
     * Shows the banner for the specified activity. This is equivalent to calling show(findViewById(android.R.id.content)).
     * If you'd like to attach the banner on a CoordinatorLayout, you should use {@link #show(View)}, or have your activity implement {@link Batch.Messaging.DisplayHintProvider}.
     * <br/>
     * This can only be called once per instance of BatchBannerView, even if {@link #dismiss(boolean)} has been called.
     * <br/>
     * You can run this method on any thread.
     *
     * @param activity Activity to display the banner on. Can't be null, must currently be onscreen.
     */
    @SuppressWarnings("ConstantConditions")
    public void show(@NonNull Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }

        // Check if the activity hints about how it wants the banner to be displayed
        View targetView = null;

        if (activity instanceof Batch.Messaging.DisplayHintProvider) {
            Batch.Messaging.DisplayHint hint =
                ((Batch.Messaging.DisplayHintProvider) activity).getBatchMessageDisplayHint(rawMessage);
            if (hint != null) {
                if (hint.strategy == Batch.Messaging.DisplayHintStrategy.EMBED) {
                    if (hint.view instanceof FrameLayout) {
                        embed((FrameLayout) hint.view);
                    } else {
                        Logger.error(MessagingModule.TAG, "Could not embed BatchBannerView, internal error.");
                    }
                    return;
                }

                targetView = hint.view;
            }
        }

        if (targetView == null) {
            targetView = activity.findViewById(android.R.id.content);
        }

        if (targetView != null) {
            show(targetView);
        } else {
            Logger.error(
                MessagingModule.TAG,
                "Could not show BatchBannerView: the given activity doesn't seem to have a valid content view"
            );
        }
    }

    /**
     * Shows the banner for the specified anchor view. Just like a Snackbar, the anchor view can be any view from your hierarchy.
     * BatchBannerView will automatically explore your view hierarchy to find the most appropriate view to display itself onto.
     * Usually, this should be a CoordinatorLayout, or your root view.
     * <br/>
     * This can only be called once per instance of BatchBannerView, even if {@link #dismiss(boolean)} has been called.
     * <br/>
     * You can run this method on any thread.
     *
     * @param anchorView View used as a base to find the best view to be attached to. Can't be null, must be in your hierarchy.
     */
    @SuppressWarnings("ConstantConditions")
    public void show(@NonNull final View anchorView) {
        if (anchorView == null) {
            throw new IllegalArgumentException("View cannot be null");
        }

        if (shown) {
            Logger.error(MessagingModule.TAG, "This banner has already been shown. Ignoring.");

            return;
        }
        shown = true;

        new Handler(anchorView.getContext().getMainLooper())
            .post(() -> {
                try {
                    LocalBroadcastManagerProvider
                        .get(anchorView.getContext())
                        .sendBroadcast(new Intent(MessagingModule.ACTION_DISMISS_BANNER));
                    shownContainer =
                        EmbeddedBannerContainerProvider.get(anchorView, rawMessage, message, analyticsDelegate, false);
                    shownContainer.show();
                } catch (Exception e) {
                    Logger.internal(MessagingModule.TAG, "Could not display banner", e);
                    Logger.error(
                        MessagingModule.TAG,
                        "Could not show BatchBannerView: internal error. Is your anchor view valid and part of the hierarchy?"
                    );
                }
            });
    }

    /**
     * Shows the banner in the given layout.
     * Should only be used in very specific cases where none of the automatic display methods are appropriate.
     * <br/>
     * Do not make any assumption about the views that Batch will add, as it is merely an implementation detail and subject to change.
     *
     * @param embedLayout Layout to embed the banner in
     */
    @SuppressWarnings("ConstantConditions")
    public void embed(@NonNull final FrameLayout embedLayout) {
        if (embedLayout == null) {
            throw new IllegalArgumentException("embedLayout cannot be null");
        }

        if (shown) {
            Logger.error(MessagingModule.TAG, "This banner has already been shown. Ignoring.");

            return;
        }
        shown = true;

        new Handler(embedLayout.getContext().getMainLooper())
            .post(() -> {
                try {
                    LocalBroadcastManagerProvider
                        .get(embedLayout.getContext())
                        .sendBroadcast(new Intent(MessagingModule.ACTION_DISMISS_BANNER));
                    shownContainer =
                        EmbeddedBannerContainerProvider.get(embedLayout, rawMessage, message, analyticsDelegate, true);
                    shownContainer.show();
                } catch (Exception e) {
                    Logger.internal(MessagingModule.TAG, "Could not embed banner", e);
                    Logger.error(
                        MessagingModule.TAG,
                        "Could not show BatchBannerView: internal error. Is your container layout valid and part of the hierarchy."
                    );
                }
            });
    }

    /**
     * Dismiss the banner if it's still on screen.
     * <p>
     * Calling this doesn't allow you to call {@link #show(View)} again.
     *
     * @param animated true if the dismissal should be animated, false otherwise
     */
    public void dismiss(boolean animated) {
        if (shownContainer != null) {
            shownContainer.dismissOnMainThread(animated);
        }
    }
}
