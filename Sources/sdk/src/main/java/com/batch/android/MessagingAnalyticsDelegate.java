package com.batch.android;

import static com.batch.android.Batch.EventDispatcher.Type.MESSAGING_CLICK;
import static com.batch.android.Batch.EventDispatcher.Type.MESSAGING_CLOSE;
import static com.batch.android.Batch.EventDispatcher.Type.MESSAGING_WEBVIEW_CLICK;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.EventDispatcherModuleProvider;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.eventdispatcher.MessagingEventPayload;
import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.module.EventDispatcherModule;
import com.batch.android.module.MessagingModule;
import com.batch.android.module.TrackerModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import java.util.ArrayList;

/**
 * Class that proxies the analytics call to the messaging module but ensures stuff like triggers only
 * occurring once.
 * It handles special cases such as In-App messages tracking an occurrence
 * <p>
 * Also makes it easily mockable
 *
 * @hide
 */
@Module
public class MessagingAnalyticsDelegate {

    private static final String STATE_KEY_CALLED_METHODS = "analyticsdelegate_called_methods";

    private MessagingModule messagingModule;
    private TrackerModule trackerModule;
    private EventDispatcherModule eventDispatcherModule;
    private Message message;
    private BatchMessage sourceMessage;
    final ArrayList<String> calledMethods = new ArrayList<>(6);

    MessagingAnalyticsDelegate(
        MessagingModule messagingModule,
        TrackerModule trackerModule,
        EventDispatcherModule eventDispatcherModule,
        Message message,
        BatchMessage sourceMessage
    ) {
        this.messagingModule = messagingModule;
        this.trackerModule = trackerModule;
        this.eventDispatcherModule = eventDispatcherModule;
        this.message = message;
        this.sourceMessage = sourceMessage;
    }

    @Provide
    public static MessagingAnalyticsDelegate provide(Message message, BatchMessage sourceMessage) {
        return new MessagingAnalyticsDelegate(
            MessagingModuleProvider.get(),
            TrackerModuleProvider.get(),
            EventDispatcherModuleProvider.get(),
            message,
            sourceMessage
        );
    }

    // Returns true if the method has already been ran once
    private boolean ensureOnce(String method) {
        synchronized (calledMethods) {
            if (calledMethods.contains(method)) {
                return true;
            } else {
                calledMethods.add(method);
                return false;
            }
        }
    }

    //region User interaction

    public void onGlobalTap(@NonNull Action action) {
        if (ensureOnce("globaltap")) {
            return;
        }
        messagingModule.onMessageGlobalTap(message, action);
        Batch.EventDispatcher.Type type = MESSAGING_CLICK;
        if (action.isDismissAction()) {
            // We trigger a close event when the global tap is a dismiss action
            type = MESSAGING_CLOSE;
        }
        eventDispatcherModule.dispatchEvent(
            type,
            new MessagingEventPayload(
                sourceMessage,
                sourceMessage.getJSON(),
                sourceMessage.getCustomPayloadInternal(),
                action
            )
        );
    }

    public void onCTAClicked(int ctaIndex, @NonNull CTA cta) {
        if (ensureOnce("ctaclicked")) {
            return;
        }
        messagingModule.onMessageCTAClicked(message, ctaIndex, cta);

        Batch.EventDispatcher.Type type = MESSAGING_CLICK;
        if (cta.isDismissAction()) {
            // We trigger a close event when the CTA is a dismiss action
            type = MESSAGING_CLOSE;
        }
        eventDispatcherModule.dispatchEvent(
            type,
            new MessagingEventPayload(
                sourceMessage,
                sourceMessage.getJSON(),
                sourceMessage.getCustomPayloadInternal(),
                cta
            )
        );
    }

    public void onWebViewClickTracked(@NonNull Action action, @Nullable String buttonAnalyticsId) {
        // This doesn't ensureOnce by design

        if (TextUtils.isEmpty(buttonAnalyticsId)) {
            buttonAnalyticsId = null;
        }
        if (buttonAnalyticsId != null && buttonAnalyticsId.length() > 30) {
            Logger.error(
                MessagingModule.TAG,
                "Could not track webview event: The analytics ID is invalid: it should be 30 characters or less. " +
                "The action will be tracked without an analytics ID, but will still be performed."
            );
            buttonAnalyticsId = null;
        }

        Batch.EventDispatcher.Type type = MESSAGING_WEBVIEW_CLICK;
        if (action.isDismissAction()) {
            // We trigger a close event when the CTA is a dismiss action
            type = MESSAGING_CLOSE;
        }

        messagingModule.onWebViewMessageClickTracked(message, action, buttonAnalyticsId);
        eventDispatcherModule.dispatchEvent(
            type,
            new MessagingEventPayload(
                sourceMessage,
                sourceMessage.getJSON(),
                sourceMessage.getCustomPayloadInternal(),
                action,
                buttonAnalyticsId
            )
        );
    }

    // Closed is when the user explicitly closes the message
    public void onClosed() {
        if (ensureOnce("closed")) {
            return;
        }
        messagingModule.onMessageClosed(message);
        eventDispatcherModule.dispatchEvent(
            Batch.EventDispatcher.Type.MESSAGING_CLOSE,
            new MessagingEventPayload(sourceMessage, sourceMessage.getJSON(), sourceMessage.getCustomPayloadInternal())
        );
    }

    public void onClosedError(@NonNull MessagingError cause) {
        if (ensureOnce("closederror")) {
            return;
        }
        messagingModule.onMessageClosedError(message, cause);
        eventDispatcherModule.dispatchEvent(
            Batch.EventDispatcher.Type.MESSAGING_CLOSE_ERROR,
            new MessagingEventPayload(sourceMessage, sourceMessage.getJSON(), sourceMessage.getCustomPayloadInternal())
        );
    }

    //endregion

    //region View lifecycle

    public void onAutoClosedAfterDelay() {
        if (ensureOnce("autoclosed")) {
            return;
        }
        messagingModule.onMessageAutoClosed(message);
        eventDispatcherModule.dispatchEvent(
            Batch.EventDispatcher.Type.MESSAGING_AUTO_CLOSE,
            new MessagingEventPayload(sourceMessage, sourceMessage.getJSON(), sourceMessage.getCustomPayloadInternal())
        );
    }

    public void onViewShown() {
        if (ensureOnce("viewshown")) {
            return;
        }
        messagingModule.onMessageShown(message);
        if (sourceMessage instanceof BatchInAppMessage) {
            BatchInAppMessage inAppMessage = (BatchInAppMessage) sourceMessage;
            trackerModule.trackCampaignView(inAppMessage.getCampaignId(), inAppMessage.getEventData());
        }

        eventDispatcherModule.dispatchEvent(
            Batch.EventDispatcher.Type.MESSAGING_SHOW,
            new MessagingEventPayload(sourceMessage, sourceMessage.getJSON(), sourceMessage.getCustomPayloadInternal())
        );
    }

    public void onViewDismissed() {
        if (ensureOnce("viewdismissed")) {
            return;
        }
        messagingModule.onMessageDismissed(message);
    }

    //endregion

    //region State saving

    public void restoreState(@Nullable Bundle inState) {
        if (inState != null) {
            ArrayList<String> stateCalledMethods = inState.getStringArrayList(STATE_KEY_CALLED_METHODS);
            if (stateCalledMethods != null) {
                calledMethods.addAll(stateCalledMethods);
            }
        }
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArrayList(STATE_KEY_CALLED_METHODS, calledMethods);
    }
    //endregion
}
