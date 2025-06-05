package com.batch.android;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.module.MessagingModule;

/**
 * Class representing a Batch Messaging view.
 * <p>
 * This is used to manually handle display of Mobile Landings and In-App Messages.
 * <p>
 * {@link BatchMessagingView} should be instantiated from
 * the static factory methods: {@link Batch.Messaging#loadMessagingView(Context, BatchMessage)}.
 * <p>
 * Then you will have to call {@link BatchMessagingView#showView(Activity)}
 * or {@link BatchMessagingView#showFragment(FragmentActivity, String)}
 * according to the {@link BatchMessagingView#kind}.
 * <p>
 * Note that: this methods should be called from the UI thread.
 * <p>
 * Example:
 * <br />
 * <pre>
 * {@code
 *   val messagingView = Batch.Messaging.loadMessagingView(this, message)
 *   when (messagingView.kind) {
 *       BatchMessagingView.Kind.Fragment -> messagingView.showFragment(supportFragmentManager, "batch-landing")
 *       BatchMessagingView.Kind.View -> messagingView.showView(this)
 *    }
 * }
 * </pre>
 */
@PublicSDK
public class BatchMessagingView {

    /**
     * Kind of view which can be wrapped by this class.
     */
    @PublicSDK
    public enum Kind {
        /**
         * A view that can be shown as a {@link DialogFragment}.
         */
        Fragment,
        /**
         * A view that can be shown as a {@link View}.
         */
        View,
    }

    /**
     * Represents the kind of view this is.
     */
    @NonNull
    private final Kind kind;

    /**
     * The wrapped view
     */
    @NonNull
    private final Object wrappedValue;

    /**
     * Private constructor to create a BatchMessagingView.
     * <p>
     * Please use the static factory methods instead: {@link Batch.Messaging#loadMessagingView(Context, BatchMessage)}.
     *
     * @param kind         The kind of view this is.
     * @param wrappedValue The wrapped value, either a {@link DialogFragment} or a {@link BatchBannerView}.
     */
    BatchMessagingView(@NonNull Kind kind, @NonNull Object wrappedValue) {
        this.kind = kind;
        this.wrappedValue = wrappedValue;
    }

    /**
     * Get the kind of view wrapped.
     *
     * @return The kind of view this is.
     */
    @NonNull
    public Kind getKind() {
        return kind;
    }

    /**
     * Get the wrapped value as a DialogFragment.
     * <p>
     * You should check the kind of view before calling this method.
     *
     * @return The wrapped value as a DialogFragment, or null.
     */
    @Nullable
    public DialogFragment toFragment() {
        return wrappedValue instanceof DialogFragment ? (DialogFragment) wrappedValue : null;
    }

    /**
     * Shows the BatchMessagingView for the specified activity.
     * <p>
     * This is equivalent to calling show(findViewById(android.R.id.content)).
     * If you'd like to attach the message on a CoordinatorLayout, you should use {@link #showView(View)},
     * or have your activity implement {@link Batch.Messaging.DisplayHintProvider}.
     * <p>
     * This can only be called once per instance of BatchMessagingView, even if {@link #dismissView(boolean)} has been called.
     * <p>
     * You can run this method on any thread.
     * <p>
     * You should check the kind of view before calling this method.
     * @param activity Activity to display the banner on. Can't be null, must currently be onscreen.
     */
    public void showView(@NonNull Activity activity) {
        if (wrappedValue instanceof BatchBannerView) {
            ((BatchBannerView) wrappedValue).show(activity);
        } else {
            Logger.error(
                MessagingModule.TAG,
                "Cannot show a BatchMessagingView of kind " + kind + " as a view. Please use showFragment() instead."
            );
        }
    }

    /**
     * Show the wrapped value as a View for the specified anchor view.
     * <p>
     * Just like a Snack bar, the anchor view can be any view from your hierarchy.
     * BatchMessagingView will automatically explore your view hierarchy to find the most appropriate view to display itself onto.
     * Usually, this should be a CoordinatorLayout, or your root view.
     * <p>
     * This can only be called once per instance of BatchBannerView, even if {@link #dismissView(boolean)} has been called.
     * <p>
     * You can run this method on any thread.
     * <p>
     * You should check the kind of view before calling this method.
     * @param anchorView View used as a base to find the best view to be attached to. Can't be null, must be in your hierarchy.
     */
    public void showView(@NonNull View anchorView) {
        if (wrappedValue instanceof BatchBannerView) {
            ((BatchBannerView) wrappedValue).show(anchorView);
        } else {
            Logger.error(
                MessagingModule.TAG,
                "Cannot show a BatchMessagingView of kind " + kind + " as a view. Please use showFragment() instead."
            );
        }
    }

    /**
     * Dismiss the BatchMessagingView when it's {@link BatchMessagingView.Kind#View#}.
     * <p>
     * Calling this doesn't allow you to call {@link #showView(Activity)} or {@link #showView(View)} again.
     * <p>
     * You should check the kind of view before calling this method.
     * @param animated true if the dismissal should be animated, false otherwise
     */
    public void dismissView(boolean animated) {
        if (wrappedValue instanceof BatchBannerView) {
            ((BatchBannerView) wrappedValue).dismiss(animated);
        } else {
            Logger.error(MessagingModule.TAG, "Cannot dismiss a BatchMessagingView of kind " + kind + ".");
        }
    }

    /**
     * Show the wrapped value as a DialogFragment.
     * <p>
     * You should check the kind of view before calling this method.
     *
     * @param activity The activity to show the dialog in.
     * @param tag      The tag to use for the fragment.
     */
    public void showFragment(@NonNull FragmentActivity activity, @Nullable String tag) {
        if (wrappedValue instanceof DialogFragment) {
            ((DialogFragment) wrappedValue).show(
                    activity.getSupportFragmentManager(),
                    tag == null ? "batch-messaging-fragment" : tag
                );
        } else {
            Logger.error(
                MessagingModule.TAG,
                "Cannot show a BatchMessagingView of kind " + kind + " as a fragment. Please use showView() instead."
            );
        }
    }

    /**
     * Show the wrapped value as a DialogFragment.
     * <p>
     * You should check the kind of view before calling this method.
     *
     * @param fragmentManager The fragment manager to use.
     * @param tag             The tag to use for the fragment.
     */
    public void showFragment(@NonNull FragmentManager fragmentManager, @Nullable String tag) {
        if (wrappedValue instanceof DialogFragment) {
            ((DialogFragment) wrappedValue).show(fragmentManager, tag == null ? "batch-messaging-fragment" : tag);
        } else {
            Logger.error(
                MessagingModule.TAG,
                "Cannot show a BatchMessagingView of kind " + kind + " as a fragment. Please use showView() instead."
            );
        }
    }

    /**
     * Dismiss the BatchMessagingView when it's {@link BatchMessagingView.Kind#Fragment}.
     * <p>
     * You should check the kind of view before calling this method.
     */
    public void dismissFragment() {
        if (wrappedValue instanceof DialogFragment) {
            ((DialogFragment) wrappedValue).dismiss();
        } else {
            Logger.error(MessagingModule.TAG, "Cannot dismiss a BatchMessagingView of kind " + kind + ".");
        }
    }
}
