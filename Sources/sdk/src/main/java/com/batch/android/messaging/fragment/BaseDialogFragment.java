package com.batch.android.messaging.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.LruCache;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.batch.android.BatchMessage;
import com.batch.android.BatchPushPayload;
import com.batch.android.MessagingAnalyticsDelegate;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.MessagingAnalyticsDelegateProvider;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.view.helper.ImageHelper;
import com.batch.android.module.MessagingModule;
import java.lang.ref.WeakReference;

/**
 * Base dialog fragment that implements the dismiss events
 */
public abstract class BaseDialogFragment<T extends Message>
    extends DialogFragment
    implements ListenableDialog, ImageHelper.Cache {

    private static final String TAG = "BaseDialogFragment";
    private static final String BUNDLE_KEY_MESSAGE_MODEL = "messageModel";

    static final String STATE_AUTOCLOSE_TARGET_UPTIME_KEY = "autoCloseAt";

    private T messageModel = null;
    private WeakReference<DialogEventListener> eventListener = new WeakReference<>(null);

    // Configure whether auto close should automatically be configured at onStart
    // or if we should wait for a signal.
    // Note: This only controls when the auto close countdown initially starts:
    // once it has been started, onStart will automatically reschedule the dismiss timer.
    protected boolean automaticallyBeginAutoClose = true;
    protected long autoCloseAtUptime = 0L;
    private Handler autoCloseHandler;

    protected MessagingModule messagingModule;
    protected MessagingAnalyticsDelegate analyticsDelegate;
    protected LruCache<String, AsyncImageDownloadTask.Result> imageCache;

    public BaseDialogFragment() {
        // We have max 1 image on a format
        this.imageCache = new LruCache<>(1);
        messagingModule = MessagingModuleProvider.get();
    }

    protected void setMessageArguments(BatchMessage payloadMessage, T messageModel) {
        final Bundle arguments = new Bundle();
        arguments.putSerializable(BUNDLE_KEY_MESSAGE_MODEL, messageModel);
        payloadMessage.writeToBundle(arguments);
        setArguments(arguments);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            T messageModel = getMessageModel();
            BatchMessage payloadMessage = getPayloadMessage();

            if (messageModel != null && payloadMessage != null) {
                analyticsDelegate = MessagingAnalyticsDelegateProvider.get(getMessageModel(), getPayloadMessage());
                analyticsDelegate.restoreState(savedInstanceState);
            }
        }

        if (analyticsDelegate != null) {
            analyticsDelegate.onViewShown();
        } else {
            Logger.internal(TAG, "Could not create analytics delegate from arguments");
        }

        if (savedInstanceState != null) {
            autoCloseAtUptime = savedInstanceState.getLong(STATE_AUTOCLOSE_TARGET_UPTIME_KEY, autoCloseAtUptime);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (analyticsDelegate != null) {
            analyticsDelegate.onSaveInstanceState(outState);
        }

        outState.putLong(STATE_AUTOCLOSE_TARGET_UPTIME_KEY, autoCloseAtUptime);
    }

    protected BatchMessage getPayloadMessage() {
        try {
            return BatchMessage.getMessageForBundle(getArguments());
        } catch (BatchPushPayload.ParsingException e) {
            Logger.internal(TAG, "Error while reading payload message from fragment arguments", e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    protected T getMessageModel() {
        if (messageModel == null) {
            try {
                messageModel = (T) getArguments().getSerializable(BUNDLE_KEY_MESSAGE_MODEL);
            } catch (ClassCastException ignored) {}
        }
        return messageModel;
    }

    @Override
    public void setDialogEventListener(DialogEventListener eventListener) {
        this.eventListener = new WeakReference<>(eventListener);
    }

    //region: Image caching

    @Override
    public void put(@NonNull AsyncImageDownloadTask.Result result) {
        imageCache.put(result.getKey(), result);
    }

    @Nullable
    @Override
    public AsyncImageDownloadTask.Result get(@NonNull String key) {
        return imageCache.get(key);
    }

    //region: Alert lifecycle

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            if (automaticallyBeginAutoClose) {
                beginAutoCloseCountdown();
            }
            scheduleAutoCloseTask();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        unscheduleAutoCloseTask();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        DialogEventListener e = eventListener.get();
        if (e != null) {
            e.onDialogDismiss(this);
        }

        boolean shouldTrackDismiss = true;
        // If we can't get the activity, consider that we should track a dismiss
        final Activity activity = getActivity();
        if (activity != null && activity.isChangingConfigurations()) {
            shouldTrackDismiss = false;
        }
        if (shouldTrackDismiss && analyticsDelegate != null) {
            analyticsDelegate.onViewDismissed();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (analyticsDelegate != null) {
            analyticsDelegate.onClosed();
        }
    }

    //endregion

    protected void dismissSafely() {
        if (getFragmentManager() != null) {
            dismissAllowingStateLoss();
        }
    }

    //region: Auto close

    protected void beginAutoCloseCountdown() {
        // Target uptime being 0 means that we haven't started the countdown yet
        // We also don't need to start that everytime, as the view will save the countdown
        // in its state
        // Setting it to -1 if we don't need to autodimiss allows to skip this check
        if (canAutoClose() && autoCloseAtUptime == 0) {
            int autoCloseDelay = getAutoCloseDelayMillis();
            if (autoCloseDelay > 0) {
                autoCloseAtUptime = SystemClock.uptimeMillis() + autoCloseDelay;
                onAutoCloseCountdownStarted();
            } else {
                autoCloseAtUptime = -1L;
            }
        }
        scheduleAutoCloseTask();
    }

    protected void scheduleAutoCloseTask() {
        if (autoCloseHandler == null && autoCloseAtUptime > 0) {
            Handler handler = new Handler();
            handler.postAtTime(this::performAutoClose, autoCloseAtUptime);
            autoCloseHandler = handler;
        }
    }

    protected void unscheduleAutoCloseTask() {
        if (autoCloseHandler != null) {
            autoCloseHandler.removeCallbacksAndMessages(null);
            autoCloseHandler = null;
        }
    }

    // This method is only called once, views are expected to save
    // that the countdown has been started in their state
    protected abstract void onAutoCloseCountdownStarted();

    protected abstract boolean canAutoClose();

    protected abstract int getAutoCloseDelayMillis();

    protected abstract void performAutoClose();
    //endregion
}
