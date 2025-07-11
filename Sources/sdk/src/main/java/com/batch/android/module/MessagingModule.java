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
import com.batch.android.BatchMessagingView;
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
import com.batch.android.messaging.fragment.AlertTemplateFragment;
import com.batch.android.messaging.fragment.ImageTemplateFragment;
import com.batch.android.messaging.fragment.ModalTemplateFragment;
import com.batch.android.messaging.fragment.UniversalTemplateFragment;
import com.batch.android.messaging.fragment.WebViewTemplateFragment;
import com.batch.android.messaging.fragment.cep.CEPTemplateFragment;
import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.model.cep.CEPMessage;
import com.batch.android.messaging.model.cep.InAppComponent;
import com.batch.android.messaging.model.mep.AlertMessage;
import com.batch.android.messaging.model.mep.BannerMessage;
import com.batch.android.messaging.model.mep.ImageMessage;
import com.batch.android.messaging.model.mep.ModalMessage;
import com.batch.android.messaging.model.mep.UniversalMessage;
import com.batch.android.messaging.model.mep.WebViewMessage;
import com.batch.android.messaging.parsing.PayloadParser;
import com.batch.android.messaging.parsing.PayloadParsingException;
import com.batch.android.messaging.view.styled.mep.TextView;
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

    private boolean showForegroundLandings = false;

    private boolean automaticMode = true;

    private Messaging.LifecycleListener listener = null;

    private Messaging.InAppInterceptor interceptor = null;

    private boolean doNotDisturbMode = false;

    private BatchMessage pendingMessage = null;

    private final ActionModule actionModule;

    private final TrackerModule trackerModule;

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

    public Messaging.InAppInterceptor getInterceptor() {
        return interceptor;
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
        InAppComponent.FontDecorationComponent.setTypefaceOverride(normalTypeface);
        InAppComponent.FontDecorationComponent.setBoldTypefaceOverride(boldTypeface);
    }

    public void setLifecycleListener(Messaging.LifecycleListener listener) {
        this.listener = listener;
    }

    public void setInAppInterceptor(Messaging.InAppInterceptor interceptor) {
        this.interceptor = interceptor;
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

        Message message = null;
        try {
            message = PayloadParser.parseUnknownLandingMessage(json);
        } catch (PayloadParsingException e) {
            Logger.error(TAG, "Error while parsing push payload", e);
            throw new BatchMessagingException(e.getMessage());
        }

        if (payloadMessage instanceof BatchInAppMessage) {
            message.source = Message.Source.LOCAL;
        } else if (payloadMessage instanceof BatchLandingMessage) {
            if (((BatchLandingMessage) payloadMessage).isDisplayedFromInbox()) {
                message.source = Message.Source.INBOX_LANDING;
            } else {
                message.source = Message.Source.LANDING;
            }
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
        } else if (message instanceof CEPMessage) {
            return CEPTemplateFragment.newInstance(payloadMessage, (CEPMessage) message);
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

        Message message = null;
        try {
            message = PayloadParser.parseUnknownLandingMessage(json);
        } catch (PayloadParsingException e) {
            Logger.error(TAG, "Error while parsing push payload", e);
            throw new BatchMessagingException(e.getMessage());
        }

        if (payloadMessage instanceof BatchInAppMessage) {
            message.source = Message.Source.LOCAL;
        } else if (payloadMessage instanceof BatchLandingMessage) {
            if (((BatchLandingMessage) payloadMessage).isDisplayedFromInbox()) {
                message.source = Message.Source.INBOX_LANDING;
            } else {
                message.source = Message.Source.LANDING;
            }
        }

        if (message.isBannerMessage()) {
            return BatchBannerViewPrivateHelper.newInstance(
                payloadMessage,
                message,
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
            BatchMessagingView view = Batch.Messaging.loadMessagingView(context, message);
            if (view.getKind() == BatchMessagingView.Kind.View) {
                if (context instanceof Activity) {
                    view.showView((Activity) context);
                } else {
                    Logger.error(
                        TAG,
                        "A banner was attempted to be displayed, but the given context is not an Activity. Cannot continue."
                    );
                }
                return;
            }
        } catch (BatchMessagingException e) {
            Logger.error(
                TAG,
                "A banner was attempted to be displayed, but the given context is not an Activity. Cannot continue."
            );
        }
        // If view is not a banner fallback on the messaging activity
        // to display the message as fragment
        MessagingActivity.startActivityForMessage(context, message);
    }

    public void displayInAppMessage(@NonNull BatchInAppMessage message) {
        Activity presentedActivity = RuntimeManagerProvider.get().getActivity();
        if (presentedActivity != null) {
            boolean shouldDisplayMessage = true;

            // Give a chance to the developer to intercept and prevent the display
            // of the In-App message
            Messaging.InAppInterceptor inAppInterceptor = getInterceptor();
            if (inAppInterceptor != null) {
                shouldDisplayMessage = !inAppInterceptor.onBatchInAppMessageReady(message);
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
            case INBOX_LANDING:
                sourceName = "inbox-landing";
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

    private void trackCTAClickEvent(
        @NonNull Message message,
        @NonNull String ctaIdentifier,
        @NonNull String ctaType,
        @Nullable String action
    ) {
        Object actionName = JSONObject.NULL;
        if (action != null) {
            actionName = action;
        }

        try {
            final JSONObject parameters = generateBaseEventParameters(message, MESSAGING_EVENT_NAME_CTA);
            parameters.put("ctaId", ctaIdentifier);
            parameters.put("ctaType", ctaType);
            parameters.put("action", actionName);
            trackerModule.track(InternalEvents.MESSAGING, parameters);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while tracking CTA event", e);
        }
    }

    private void trackOldCTAClickEvent(@NonNull Message message, int ctaIndex, @Nullable String action) {
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
            listener.onBatchMessageClosed(
                message.devTrackingIdentifier,
                Messaging.LifecycleListener.MessagingCloseReason.Action
            );
        }
    }

    // Closed means that the user tapped on Back or the close button
    public void onMessageClosed(@NonNull Message message) {
        trackGenericEvent(message, MESSAGING_EVENT_NAME_CLOSE);
        if (listener != null) {
            listener.onBatchMessageClosed(
                message.devTrackingIdentifier,
                Messaging.LifecycleListener.MessagingCloseReason.User
            );
        }
    }

    // Used only for MEP messages
    public void onMessageCTAClicked(@NonNull Message message, int ctaIndex, @NonNull CTA cta) {
        trackOldCTAClickEvent(message, ctaIndex, cta.action);
        if (listener != null) {
            String ctaIdentifier = "mepCtaIndex:".concat(String.valueOf(ctaIndex)); // Old MEP ctaIndex prefixed with "mepCtaIndex"
            listener.onBatchMessageActionTriggered(
                message.devTrackingIdentifier,
                ctaIdentifier,
                new BatchMessageCTA(cta)
            );
        }
    }

    // Used only for CEP messages
    public void onMessageCTAClicked(
        @NonNull Message message,
        @NonNull String ctaId,
        @NonNull String ctaType,
        @NonNull CTA cta
    ) {
        trackCTAClickEvent(message, ctaId, ctaType, cta.action);
        if (listener != null) {
            listener.onBatchMessageActionTriggered(message.devTrackingIdentifier, ctaId, new BatchMessageCTA(cta));
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
            listener.onBatchMessageActionTriggered(
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
            listener.onBatchMessageClosed(
                message.devTrackingIdentifier,
                Messaging.LifecycleListener.MessagingCloseReason.Auto
            );
        }
    }

    // Closed for error means that the in-app could not load correctly
    public void onMessageClosedError(@NonNull Message message, @NonNull MessagingError cause) {
        trackCloseErrorEvent(message, cause);
        if (listener != null) {
            listener.onBatchMessageClosed(
                message.devTrackingIdentifier,
                Messaging.LifecycleListener.MessagingCloseReason.Error
            );
        }
    }
    //endregion
}
