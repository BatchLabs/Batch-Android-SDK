package com.batch.android.module;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.batch.android.Batch;
import com.batch.android.Batch.Messaging;
import com.batch.android.BatchBannerView;
import com.batch.android.BatchBannerViewPrivateHelper;
import com.batch.android.BatchInAppMessage;
import com.batch.android.BatchLandingMessage;
import com.batch.android.BatchMessage;
import com.batch.android.BatchMessageAction;
import com.batch.android.BatchMessageCTA;
import com.batch.android.BatchMessagingException;
import com.batch.android.MessagingActivity;
import com.batch.android.core.Logger;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.di.providers.ActionModuleProvider;
import com.batch.android.di.providers.MessagingAnalyticsDelegateProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.PayloadParser;
import com.batch.android.messaging.PayloadParsingException;
import com.batch.android.messaging.fragment.AlertTemplateFragment;
import com.batch.android.messaging.fragment.ImageTemplateFragment;
import com.batch.android.messaging.fragment.ModalTemplateFragment;
import com.batch.android.messaging.fragment.UniversalTemplateFragment;
import com.batch.android.messaging.fragment.WebViewTemplateFragment;
import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.AlertMessage;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.model.ModalMessage;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.model.WebViewMessage;
import com.batch.android.messaging.view.styled.TextView;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;

/**
 * Batch's Messaging Module.
 *
 */
@Module
@Singleton
public class MessagingModule extends BatchModule {

    public static final String TAG = "Messaging";
    //region: Constants

    /**
     * Broadcast this action using {@link LocalBroadcastManager} and MessagingActivity instances will dismiss themselves
     */
    public static final String ACTION_DISMISS_INTERSTITIAL = "com.batch.android.messaging.DISMISS_INTERSTITIAL";

    /**
     * Broadcast this action using {@link LocalBroadcastManager} and banners will dismiss themselves
     */
    public static final String ACTION_DISMISS_BANNER = "com.batch.android.messaging.DISMISS_BANNER";

    public static final double DEFAULT_IMAGE_DOWNLOAD_TIMEOUT = 30.0; // Seconds

    private static final String MESSAGING_EVENT_NAME_SHOW = "show";

    private static final String MESSAGING_EVENT_NAME_DISMISS = "dismiss";

    private static final String MESSAGING_EVENT_NAME_CLOSE = "close";

    private static final String MESSAGING_EVENT_NAME_CLOSE_ERROR = "close_error";

    private static final String MESSAGING_EVENT_NAME_AUTO_CLOSE = "auto_close";

    private static final String MESSAGING_EVENT_NAME_GLOBAL_TAP = "global_tap_action";

    private static final String MESSAGING_EVENT_NAME_CTA = "cta_action";

    private static final String MESSAGING_EVENT_NAME_WEBVIEW_CLICK = "webview_click";

    //endregion

    //region: Instance variables

    private boolean showForegroundLandings = true;

    private boolean automaticMode = true;

    private Messaging.LifecycleListener listener = null;

    private boolean doNotDisturbMode = false;

    private BatchMessage pendingMessage = null;

    private ActionModule actionModule;

    private TrackerModule trackerModule;

    private MessagingModule(ActionModule actionModule, TrackerModule trackerModule) {
        this.actionModule = actionModule;
        this.trackerModule = trackerModule;
    }

    @Provide
    public static MessagingModule provide() {
        return new MessagingModule(ActionModuleProvider.get(), TrackerModuleProvider.get());
    }

    //endregion

    //region: BatchModule

    @Override
    public String getId() {
        return "messaging";
    }

    @Override
    public int getState() {
        return 1;
    }

    //endregion

    //region: Getters

    public boolean shouldShowForegroundLandings() {
        return showForegroundLandings;
    }

    public boolean isInAutomaticMode() {
        return automaticMode;
    }

    public Messaging.LifecycleListener getListener() {
        return listener;
    }

    public boolean isDoNotDisturbEnabled() {
        return doNotDisturbMode;
    }

    //endregion

    //region: Public methods

    public void setShowForegroundLandings(boolean showForegroundLandings) {
        this.showForegroundLandings = showForegroundLandings;
    }

    public void setAutomaticMode(boolean automatic) {
        automaticMode = automatic;
    }

    public void setTypefaceOverride(@Nullable Typeface normalTypeface, @Nullable Typeface boldTypeface) {
        TextView.typefaceOverride = normalTypeface;
        TextView.boldTypefaceOverride = boldTypeface;
    }

    public void setLifecycleListener(Messaging.LifecycleListener listener) {
        this.listener = listener;
    }

    public void setDoNotDisturbEnabled(boolean enableDnd) {
        doNotDisturbMode = enableDnd;
    }

    public boolean hasPendingMessage() {
        return pendingMessage != null;
    }

    @Nullable
    public BatchMessage popPendingMessage() {
        BatchMessage retVal = pendingMessage;
        pendingMessage = null;
        return retVal;
    }

    // Check if android.fragment and androidx.appcompat are here, and publicly log the errors if asked
    // Note that this does not check if the libraries are of the correct version
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean doesAppHaveRequiredLibraries(boolean logErrors) {
        if (!ReflectionHelper.isAndroidXFragmentPresent()) {
            if (logErrors) {
                Logger.error(
                    TAG,
                    "Your app doesn't seem to have the android X fragment library, or its version is lower than the 1.0.0 minimum required by Batch.. The landing will not be displayed. More info at https://batch.com/doc ."
                );
            }
            return false;
        }

        if (!ReflectionHelper.isAndroidXAppCompatActivityPresent()) {
            if (logErrors) {
                Logger.error(
                    TAG,
                    "Your app doesn't seem to have the android X appcompat library, or its version is lower than the 1.0.0 minimum required by Batch. The landing will not be displayed. More info at https://batch.com/doc ."
                );
            }
            return false;
        }

        return true;
    }

    public DialogFragment loadFragment(
        @NonNull Context context,
        @NonNull BatchMessage payloadMessage,
        @NonNull JSONObject json
    ) throws BatchMessagingException {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        //noinspection ConstantConditions
        if (payloadMessage == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        if (!doesAppHaveRequiredLibraries(true)) {
            throw new BatchMessagingException(
                "Integration problem: your app must bundle the android x fragment and appcompat libraries, and their version must be higher than 1.0.0."
            );
        }

        Message message;

        try {
            message = PayloadParser.parsePayload(json);

            if (payloadMessage instanceof BatchInAppMessage) {
                message.source = Message.Source.LOCAL;
            } else if (payloadMessage instanceof BatchLandingMessage) {
                message.source = Message.Source.LANDING;
            }
        } catch (PayloadParsingException e) {
            Logger.error(TAG, "Error while parsing push payload", e);
            throw new BatchMessagingException(e.getMessage());
        }

        if (message instanceof AlertMessage) {
            return AlertTemplateFragment.newInstance(payloadMessage, (AlertMessage) message);
        } else if (message instanceof UniversalMessage) {
            return UniversalTemplateFragment.newInstance(payloadMessage, (UniversalMessage) message);
        } else if (message instanceof ModalMessage) {
            return ModalTemplateFragment.newInstance(payloadMessage, (ModalMessage) message);
        } else if (message instanceof ImageMessage) {
            return ImageTemplateFragment.newInstance(payloadMessage, (ImageMessage) message);
        } else if (message instanceof WebViewMessage) {
            return WebViewTemplateFragment.newInstance(payloadMessage, (WebViewMessage) message);
        } else if (message instanceof BannerMessage) {
            throw new BatchMessagingException(
                "This message is a banner. Please use the dedicated loadBanner() method."
            );
        } else {
            throw new BatchMessagingException("Internal error (code 10)");
        }
    }

    public BatchBannerView loadBanner(
        @NonNull Context context,
        @NonNull BatchMessage payloadMessage,
        @NonNull JSONObject json
    ) throws BatchMessagingException {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        //noinspection ConstantConditions
        if (payloadMessage == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        if (!doesAppHaveRequiredLibraries(true)) {
            throw new BatchMessagingException(
                "Integration problem: your app must bundle the android x fragment and appcompat libraries, and their version must be higher than 1.0.0."
            );
        }

        Message message;

        try {
            message = PayloadParser.parsePayload(json);

            if (payloadMessage instanceof BatchInAppMessage) {
                message.source = Message.Source.LOCAL;
            } else if (payloadMessage instanceof BatchLandingMessage) {
                message.source = Message.Source.LANDING;
            }
        } catch (PayloadParsingException e) {
            Logger.error(TAG, "Error while parsing push payload", e);
            throw new BatchMessagingException(e.getMessage());
        }

        if (message instanceof BannerMessage) {
            return BatchBannerViewPrivateHelper.newInstance(
                payloadMessage,
                (BannerMessage) message,
                MessagingAnalyticsDelegateProvider.get(message, payloadMessage)
            );
        } else {
            throw new BatchMessagingException("The BatchMessage instance does not represent a banner.");
        }
    }

    public void performAction(Context c, BatchMessage payloadMessage, Action a) {
        if (!TextUtils.isEmpty(a.action)) {
            actionModule.performAction(c, a.action, a.args, payloadMessage);
        }
    }

    public void displayMessage(@NonNull Context context, @NonNull BatchMessage message, boolean bypassDnD) {
        if (!bypassDnD && doNotDisturbMode) {
            pendingMessage = message;
            Logger.info(TAG, "Enqueuing a message, as it should have been displayed but Do Not Disturb is enabled.");
            return;
        }

        // Try to load the banner corresponding to the message. If it's not a banner, fallback on the interstitial activity

        try {
            BatchBannerView banner = Batch.Messaging.loadBanner(context, message);
            if (context instanceof Activity) {
                banner.show((Activity) context);
            } else {
                Logger.error(
                    TAG,
                    "A banner was attempted to be displayed, but the given context is not an Activity. Cannot continue."
                );
            }
            return;
        } catch (BatchMessagingException e) {
            // Don't log: it's simply not a banner
        }

        MessagingActivity.startActivityForMessage(context, message);
    }

    public void displayInAppMessage(@NonNull BatchInAppMessage message) {
        Activity presentedActivity = RuntimeManagerProvider.get().getActivity();
        if (presentedActivity != null) {
            boolean shouldDisplayMessage = true;

            // Give a chance to the developer to intercept and prevent the display
            // of the In-App message
            Messaging.LifecycleListener listener = getListener();
            if (listener instanceof Messaging.LifecycleListener2) {
                shouldDisplayMessage = !((Messaging.LifecycleListener2) listener).onBatchInAppMessageReady(message);
            }

            if (shouldDisplayMessage) {
                displayMessage(presentedActivity, message, false);
            } else {
                Logger.internal(LocalCampaignsModule.TAG, "Developer prevented automatic In-App display");
            }
        } else {
            Logger.error(
                LocalCampaignsModule.TAG,
                "Could not find an activity to display on. Does the RuntimeManager ever had one?"
            );
        }
    }

    //endregion

    //region: Events

    private JSONObject generateBaseEventParameters(@NonNull Message message, @NonNull String type)
        throws JSONException {
        final JSONObject params = new JSONObject();

        String sourceName;
        switch (message.source) {
            case LOCAL:
                sourceName = "local";
                break;
            case LANDING:
                sourceName = "landing";
                break;
            default:
                sourceName = "unknown";
                break;
        }

        params.put("s", sourceName);
        params.put("id", message.messageIdentifier);
        if (message.eventData != null) {
            params.put("ed", message.eventData);
        }
        params.put("type", type);

        return params;
    }

    private void trackGenericEvent(@NonNull Message message, @NonNull String type) {
        try {
            trackerModule.track(InternalEvents.MESSAGING, generateBaseEventParameters(message, type));
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking event", e);
        }
    }

    private void trackCloseErrorEvent(@NonNull Message message, @NonNull MessagingError cause) {
        try {
            final JSONObject parameters = generateBaseEventParameters(message, MESSAGING_EVENT_NAME_CLOSE_ERROR);
            if (cause != null) {
                parameters.put("cause", cause.code);
            }
            trackerModule.track(InternalEvents.MESSAGING, parameters);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking event", e);
        }
    }

    private void trackCTAClickEvent(@NonNull Message message, int ctaIndex, @Nullable String action) {
        Object actionName = JSONObject.NULL;
        if (action != null) {
            actionName = action;
        }

        try {
            final JSONObject parameters = generateBaseEventParameters(message, MESSAGING_EVENT_NAME_CTA);
            parameters.put("ctaIndex", ctaIndex);
            parameters.put("action", actionName);
            trackerModule.track(InternalEvents.MESSAGING, parameters);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking CTA event", e);
        }
    }

    private void trackWebViewClickEvent(
        @NonNull Message message,
        @NonNull String actionName,
        @Nullable String buttonAnalyticsID
    ) {
        try {
            final JSONObject parameters = generateBaseEventParameters(message, MESSAGING_EVENT_NAME_WEBVIEW_CLICK);
            parameters.put("actionName", buttonAnalyticsID);
            if (!TextUtils.isEmpty(buttonAnalyticsID)) {
                parameters.put("analyticsID", buttonAnalyticsID);
            }
            trackerModule.track(InternalEvents.MESSAGING, parameters);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking a webview trackClick event", e);
        }
    }

    //endregion

    //region: Lifecycle listener

    public void onMessageShown(@NonNull Message message) {
        trackGenericEvent(message, MESSAGING_EVENT_NAME_SHOW);
        if (listener != null) {
            listener.onBatchMessageShown(message.devTrackingIdentifier);
        }
    }

    public void onMessageDismissed(@NonNull Message message) {
        trackGenericEvent(message, MESSAGING_EVENT_NAME_DISMISS);
        if (listener != null) {
            listener.onBatchMessageClosed(message.devTrackingIdentifier);
        }
    }

    // Closed means that the user tapped on Back or the close button
    public void onMessageClosed(@NonNull Message message) {
        trackGenericEvent(message, MESSAGING_EVENT_NAME_CLOSE);
        if (listener != null) {
            listener.onBatchMessageCancelledByUser(message.devTrackingIdentifier);
        }
    }

    public void onMessageCTAClicked(@NonNull Message message, int ctaIndex, @NonNull CTA cta) {
        trackCTAClickEvent(message, ctaIndex, cta.action);
        if (listener != null) {
            listener.onBatchMessageActionTriggered(message.devTrackingIdentifier, ctaIndex, new BatchMessageCTA(cta));
        }
    }

    // Used only for WebView message
    public void onWebViewMessageClickTracked(
        @NonNull Message message,
        @NonNull Action action,
        @Nullable String buttonAnalyticsId
    ) {
        trackWebViewClickEvent(message, action.action, buttonAnalyticsId);

        if (listener != null) {
            listener.onBatchMessageWebViewActionTriggered(
                message.devTrackingIdentifier,
                buttonAnalyticsId,
                new BatchMessageAction(action)
            );
        }
    }

    public void onMessageGlobalTap(@NonNull Message message, @NonNull Action action) {
        Object actionName = action.action;
        if (actionName == null) {
            actionName = JSONObject.NULL;
        }

        try {
            final JSONObject parameters = generateBaseEventParameters(message, MESSAGING_EVENT_NAME_GLOBAL_TAP);
            parameters.put("action", actionName);
            trackerModule.track(InternalEvents.MESSAGING, parameters);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking CTA event", e);
        }

        if (listener != null) {
            listener.onBatchMessageActionTriggered(
                message.devTrackingIdentifier,
                Messaging.GLOBAL_TAP_ACTION_INDEX,
                new BatchMessageAction(action)
            );
        }
    }

    public void onMessageAutoClosed(@NonNull Message message) {
        trackGenericEvent(message, MESSAGING_EVENT_NAME_AUTO_CLOSE);
        if (listener != null) {
            listener.onBatchMessageCancelledByAutoclose(message.devTrackingIdentifier);
        }
    }

    // Closed for error means that the in-app could not load correctly
    public void onMessageClosedError(@NonNull Message message, @NonNull MessagingError cause) {
        trackCloseErrorEvent(message, cause);
        if (listener != null) {
            listener.onBatchMessageCancelledByError(message.devTrackingIdentifier);
        }
    }
    //endregion
}
