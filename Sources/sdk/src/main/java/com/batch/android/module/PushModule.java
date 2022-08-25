package com.batch.android.module;

import static com.batch.android.core.InternalPushData.BATCH_BUNDLE_KEY;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.AdsIdentifierProvider;
import com.batch.android.Batch;
import com.batch.android.BatchActionActivity;
import com.batch.android.BatchNotificationInterceptor;
import com.batch.android.BatchPushHelper;
import com.batch.android.BatchPushNotificationPresenter;
import com.batch.android.BatchPushPayload;
import com.batch.android.BatchPushService;
import com.batch.android.IntentParser;
import com.batch.android.PushNotificationType;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.core.NotificationPermissionHelper;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.DisplayReceiptModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.push.PushRegistrationProviderFactory;
import com.batch.android.push.Registration;
import com.batch.android.runtime.State;
import com.google.firebase.messaging.RemoteMessage;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Push Module of Batch
 *
 */
@Module
@Singleton
public class PushModule extends BatchModule {

    public static final String TAG = "Push";

    /**
     * Default value for no accent color set by the developer
     */
    public static final int NO_COLOR = -1;

    // ------------------------------------------>

    /**
     * Should we refresh the token on next start?
     */
    private boolean shouldRefreshToken = true;
    /**
     * Small icon resource id (default = 0)
     */
    private int smallIconResourceId = 0;
    /**
     * Big icon resource id (default = null)
     */
    private Bitmap largeIcon;
    /**
     * GCM sender id
     */
    private String gcmSenderId;
    /**
     * Notification color set by the developer
     */
    private int notificationColor = NO_COLOR;
    /**
     * Notification sound set by the developer
     */
    private Uri notificationSoundUri = null;
    /**
     * Manual display mode activated by the developer
     */
    private boolean manualDisplay = false;

    /**
     * Temporary stored notif type set by developer, waiting for Context to store it
     */
    private EnumSet<PushNotificationType> tempNotifType;

    /**
     * Custom intent flags
     */
    private Integer customOpenIntentFlags = null;

    /**
     * Notification interceptor
     */
    private BatchNotificationInterceptor notificationInterceptor = null;

    /**
     * Push Registration Provider
     */
    private PushRegistrationProvider registrationProvider;
    private boolean didSetupRegistrationProvider = false;

    private DisplayReceiptModule displayReceiptModule;

    private PushModule(DisplayReceiptModule displayReceiptModule) {
        this.displayReceiptModule = displayReceiptModule;
    }

    @Provide
    public static PushModule provide() {
        return new PushModule(DisplayReceiptModuleProvider.get());
    }

    // ---------------------------------------->

    /**
     * Return the custom small icon resource if any.
     *
     * @return resource id if any, 0 if not.
     */
    public int getCustomSmallIconResourceId() {
        return smallIconResourceId;
    }

    /**
     * Set the custom small icon resource id.
     *
     * @param resourceId
     */
    public void setCustomSmallIconResourceId(int resourceId) {
        this.smallIconResourceId = resourceId;
    }

    /**
     * User set additional intent flags for notifications.
     * Can be null.
     */
    public Integer getAdditionalIntentFlags() {
        return this.customOpenIntentFlags;
    }

    /**
     * Sets additional intent flags for notifications.
     * Doesn't work for external deeplinks.
     *
     * @param flags Additional flags. "null" to clear.
     */
    public void setAdditionalIntentFlags(Integer flags) {
        this.customOpenIntentFlags = flags;
    }

    /**
     * Return the custom large icon if any.
     *
     * @return resource if any, null if not.
     */
    public Bitmap getCustomLargeIcon() {
        return largeIcon;
    }

    /**
     * Set the custom large icon.
     *
     * @param largeIcon
     */
    public void setCustomLargeIcon(Bitmap largeIcon) {
        this.largeIcon = largeIcon;
    }

    /**
     * Set the GCM sender id
     *
     * @param senderId
     */
    public void setGCMSenderId(String senderId) {
        this.gcmSenderId = senderId;

        shouldRefreshToken = true;
    }

    /**
     * Get the developer registered notification interceptor
     */
    public void setNotificationInterceptor(BatchNotificationInterceptor interceptor) {
        this.notificationInterceptor = interceptor;
    }

    /**
     * Set the developer registered notification interceptor
     */
    public BatchNotificationInterceptor getNotificationInterceptor() {
        return this.notificationInterceptor;
    }

    /**
     * Dismiss all app notifications
     */
    public void dismissNotifications() {
        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state == State.OFF) {
                    Logger.warning(
                        TAG,
                        "Call to dismissBatchNotifications made while SDK is not started, please call this method only after Batch.onStart."
                    );
                    return;
                }

                try {
                    (
                        (NotificationManager) RuntimeManagerProvider
                            .get()
                            .getContext()
                            .getSystemService(Context.NOTIFICATION_SERVICE)
                    ).cancelAll();
                } catch (Exception e) {
                    Logger.error(TAG, "Error while dismissing notifications", e);
                }
            });
    }

    /**
     * Check if the received push is a Batch one. If you have a custom push implementation into your app you should
     * call this method before doing anything else into the {@link BroadcastReceiver#onReceive(Context, Intent)} method.
     * If it returns true, you should not handle the push.
     *
     * @param intent
     * @return true if the push is for Batch and you shouldn't handle it, false otherwise
     */
    public boolean isBatchPush(Intent intent) {
        if (intent == null) {
            return false;
        }

        if (intent.getExtras() == null) {
            return false;
        }

        return intent.getExtras().getString(BATCH_BUNDLE_KEY) != null;
    }

    /**
     * Firebase variant
     */
    public boolean isBatchPush(RemoteMessage message) {
        if (message == null) {
            return false;
        }

        Map<String, String> data = message.getData();

        if (data == null || data.size() == 0) {
            return false;
        }

        return data.get(BATCH_BUNDLE_KEY) != null;
    }

    /**
     * Return the push token if available
     *
     * @return token if available, null otherwise
     */
    public String getRegistrationID() {
        final StringBuilder registrationIDBuilder = new StringBuilder();

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state != State.OFF) { // We need a context to get the token
                    final Registration registration = getRegistration(RuntimeManagerProvider.get().getContext());
                    if (registration != null) {
                        registrationIDBuilder.append(registration.registrationID);
                    }
                }
            });

        if (registrationIDBuilder.length() > 0) {
            return registrationIDBuilder.toString();
        }

        return null;
    }

    /**
     * Get the persisted registration
     */
    public Registration getRegistration(@NonNull Context context) {
        try {
            final Parameters parameters = ParametersProvider.get(context);
            if (parameters == null) {
                Logger.internal(TAG, "Could not fetch registration: failed to read parameters");
                return null;
            }

            final String registrationID = parameters.get(ParameterKeys.PUSH_REGISTRATION_ID_KEY);
            if (registrationID == null) {
                return null;
            }

            String registrationProvider = parameters.get(ParameterKeys.PUSH_REGISTRATION_PROVIDER_KEY);
            if (TextUtils.isEmpty(registrationProvider)) {
                registrationProvider = "UNKNOWN";
            }

            String senderID = parameters.get(ParameterKeys.PUSH_REGISTRATION_SENDERID_KEY);

            return new Registration(registrationProvider, registrationID, senderID);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while retrieving registration id", e);
            return null;
        }
    }

    /**
     * Get the enabled notification types<br>
     * Matches what you've set in setNotificationsType.
     *
     * @return Type of notifications you previously set. Be careful, as this can be null if you never used setNotificationsType() or if your context is invalid
     */
    public EnumSet<PushNotificationType> getNotificationsType(Context context) {
        if (context == null) {
            return null;
        }

        String param;

        if (tempNotifType != null) {
            // We do not return the notif type directly as we want a coherent implementation
            param = Integer.toString(PushNotificationType.toValue(tempNotifType));
        } else {
            param = ParametersProvider.get(context).get(ParameterKeys.PUSH_NOTIF_TYPE);
        }

        if (TextUtils.isEmpty(param)) {
            return null;
        }

        try {
            int intParam = Integer.parseInt(param);
            return PushNotificationType.fromValue(intParam);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading notification type", e);
            return null;
        }
    }

    /**
     * Adjust the way Batch will display notifications.<br>
     * You should use this method if you want to remove vibration, light, sound or avoid notifications for this user.
     *
     * @param types Type of notifications you want, default = ALERT + LIGHTS + VIBRATE + SOUND
     */
    public void setNotificationsType(EnumSet<PushNotificationType> types) {
        if (types == null) {
            Logger.error(TAG, "Call to setNotificationsType with null type given, aborting");
            return;
        }

        try {
            final AtomicBoolean handled = new AtomicBoolean(false);
            final int value = PushNotificationType.toValue(types);

            if (RuntimeManagerProvider.get() != null) {
                RuntimeManagerProvider
                    .get()
                    .run(state -> {
                        if (state != State.OFF) {
                            ParametersProvider
                                .get(RuntimeManagerProvider.get().getContext())
                                .set(ParameterKeys.PUSH_NOTIF_TYPE, Integer.toString(value), true);
                            handled.set(true);
                        }
                    });
            }

            if (!handled.get()) {
                tempNotifType = types;
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while storing notification types", e);
        }
    }

    /**
     * Set the notification accent color for Lollipop or later.
     * See <a href="http://developer.android.com/reference/android/app/Notification.html#color">Notification.color</a> for more details
     *
     * @param argbColor an ARGB integer like the constants in {@link Color}
     */
    public void setNotificationsColor(final int argbColor) {
        notificationColor = argbColor;
    }

    /**
     * Get the notification color set by the developer
     *
     * @return color if set, {@link #NO_COLOR} if not
     */
    public int getNotificationColor() {
        return notificationColor;
    }

    /**
     * Override the default notification sound uri
     * See <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSound(android.net.Uri)">Notification.Builder.setSound</a> for more details
     *
     * @param soundUri Sound uri to set on notifications. Null to use the default sound.
     */
    public void setSound(final Uri soundUri) {
        notificationSoundUri = soundUri;
    }

    /**
     * Get the notification sound uri set by the developer
     *
     * @return the uri override if set, null if not
     */
    public Uri getSound() {
        return notificationSoundUri;
    }

    /**
     * Set manual display mode for push notifications. <br />
     * <b>If you set manual display mode to true, no notifications will be shown automatically and you'll have to display it by yourself.
     *
     * @param manualDisplay
     */
    public void setManualDisplay(boolean manualDisplay) {
        this.manualDisplay = manualDisplay;
    }

    /**
     * Is manual display mode activated by the developer
     *
     * @return
     */
    public boolean isManualDisplayModeActivated() {
        return manualDisplay;
    }

    /**
     * Append Batch data to your open intent so that opens from this push will be tracked by Batch and displayed into your dashboard.
     * It also powers other features, such as but not limited to mobile landings.
     *
     * @param pushIntent the intent from GCM, that origined this push
     * @param openIntent the intent of the notification the will be triggered when the user clicks on it
     */
    public void appendBatchData(Intent pushIntent, Intent openIntent) {
        try {
            appendBatchData(pushIntent.getExtras(), openIntent);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while appending batch data to intent", e);
            Logger.error(TAG, "Error while appending Batch data to open intent : " + e.getLocalizedMessage());
        }
    }

    /**
     * Append Batch data to your open intent so that opens from this push will be tracked by Batch and displayed into your dashboard.
     * It also powers other features, such as but not limited to mobile landings.
     *
     * @param pushIntentExtras the intent extras from GCM, that origined this push
     * @param openIntent       the intent of the notification the will be triggered when the user clicks on it
     */
    public void appendBatchData(Bundle pushIntentExtras, Intent openIntent) {
        try {
            InternalPushData data = InternalPushData.getPushDataForReceiverBundle(pushIntentExtras);
            if (data == null) {
                Logger.error(
                    TAG,
                    "Error while appending Batch data to open intent : the pushIntentExtras seems to not be a Batch Push intent extras. Aborting"
                );
                return;
            }

            IntentParser.putPushExtrasToIntent(pushIntentExtras, data, openIntent);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while appending batch data to intent", e);
            Logger.error(TAG, "Error while appending Batch data to open intent : " + e.getLocalizedMessage());
        }
    }

    /**
     * Firebase variant of the method
     */
    public void appendBatchData(@NonNull RemoteMessage remoteMessage, Intent openIntent) {
        Bundle extras = BatchPushHelper.firebaseMessageToReceiverBundle(remoteMessage);
        if (extras == null) {
            Logger.error(TAG, "Could not read data from Firebase message");
            return;
        }
        appendBatchData(extras, openIntent);
    }

    /**
     * Make a PendingIntent suitable for notifications from a given Intent.
     * This is useful for custom receivers, or {@link BatchNotificationInterceptor} implementations.
     * <p>
     * Warning: it will override the intent's action with a unique name, to ensure that existing notifications are not updated with this PendingIntent's content.
     * If you rely on a custom action, you will have to make your own PendingIntent.
     *
     * @param intent           The intent you want to be triggered when performing the pending intent. Must be an intent compatible with {@link PendingIntent#getActivity(Context, int, Intent, int)}. Cannot be null.
     * @param pushIntentExtras Raw extras of the push intent, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
     * @return A PendingIntent instance, wrapping the given Intent.
     */
    @NonNull
    public PendingIntent makePendingIntent(
        @NonNull Context context,
        @NonNull Intent intent,
        @NonNull Bundle pushIntentExtras
    ) {
        final Intent launchIntent = new Intent(intent);
        launchIntent.setAction("batch_" + Long.toString(System.currentTimeMillis()));
        Batch.Push.appendBatchData(pushIntentExtras, launchIntent);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags | PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(context, 0, launchIntent, flags);
    }

    /**
     * Firebase variant of the method
     */
    @NonNull
    public PendingIntent makePendingIntent(
        @NonNull Context context,
        @NonNull Intent intent,
        @NonNull RemoteMessage message
    ) {
        Bundle extras = BatchPushHelper.firebaseMessageToReceiverBundle(message);
        if (extras == null) {
            extras = new Bundle();
        }

        return makePendingIntent(context, intent, extras);
    }

    /**
     * Make a PendingIntent suitable for notifications from a given deeplink. It will use Batch's builtin action activity.
     * <p>
     * This is useful for custom receivers, {@link com.batch.android.BatchNotificationInterceptor} or {@link com.batch.android.BatchDeeplinkInterceptor} implementations.
     *
     * @param deeplink         Deeplink string. Cannot be null.
     * @param pushIntentExtras Raw extras of the push intent, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
     * @return A PendingIntent set to open Batch's builtin action activity to open the specified deeplink. Can be null if the deeplink is not valid.
     */
    @Nullable
    public PendingIntent makePendingIntentForDeeplink(
        @NonNull Context context,
        @NonNull String deeplink,
        @NonNull Bundle pushIntentExtras
    ) {
        if (TextUtils.isEmpty(deeplink)) {
            return null;
        }

        final Intent launchIntent = new Intent(context, BatchActionActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.putExtra(BatchActionActivity.EXTRA_DEEPLINK_KEY, deeplink);
        launchIntent.setAction("batch_" + Long.toString(System.currentTimeMillis()));

        return makePendingIntent(context, launchIntent, pushIntentExtras);
    }

    /**
     * Firebase version of the original method
     */
    @Nullable
    public PendingIntent makePendingIntentForDeeplink(
        @NonNull Context context,
        @NonNull String deeplink,
        @NonNull RemoteMessage message
    ) {
        Bundle extras = BatchPushHelper.firebaseMessageToReceiverBundle(message);
        if (extras == null) {
            extras = new Bundle();
        }

        return makePendingIntentForDeeplink(context, deeplink, extras);
    }

    /**
     * Should the developer handle and display this push
     *
     * @param context
     * @param intent  the gcm push intent
     * @return true if the push is valid and should be handled, false otherwise
     */
    public boolean shouldDisplayPush(Context context, Intent intent) {
        try {
            return isManualDisplayModeActivated() && isBatchPush(intent);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while evaluating if should display push", e);
            return true;
        }
    }

    /**
     * Firebase variant
     */
    public boolean shouldDisplayPush(Context context, RemoteMessage message) {
        try {
            return isManualDisplayModeActivated() && isBatchPush(message);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while evaluating if should display push", e);
            return true;
        }
    }

    /**
     * Call this method to display the notification for this intent.
     *
     * @param context
     * @param intent  the gcm push intent
     */
    public void displayNotification(
        Context context,
        Intent intent,
        BatchNotificationInterceptor interceptor,
        boolean bypassManualMode
    ) {
        try {
            if (!bypassManualMode && !shouldDisplayPush(context, intent)) {
                return;
            }

            if (interceptor == null) {
                interceptor = getNotificationInterceptor();
            }

            BatchPushNotificationPresenter.presentNotification(
                context,
                intent.getExtras(),
                BatchPushPayload.payloadFromReceiverIntent(intent),
                interceptor
            );
        } catch (Exception e) {
            Logger.internal(TAG, "An error occured while handling push notification", e);
            Logger.error(TAG, "An error occured during display : " + e.getLocalizedMessage());
        }
    }

    /**
     * Call this method to display the notification for Firebase message
     */
    public void displayNotification(Context context, RemoteMessage message, BatchNotificationInterceptor interceptor) {
        try {
            if (!shouldDisplayPush(context, message)) {
                return;
            }

            if (interceptor == null) {
                interceptor = getNotificationInterceptor();
            }

            Bundle extras = BatchPushHelper.firebaseMessageToReceiverBundle(message);
            if (extras == null) {
                extras = new Bundle();
            }

            BatchPushNotificationPresenter.presentNotification(
                context,
                extras,
                BatchPushPayload.payloadFromReceiverExtras(extras),
                interceptor
            );
        } catch (Exception e) {
            Logger.internal("An error occured while handling push notification", e);
            Logger.error(TAG, "An error occurred during display : " + e.getLocalizedMessage());
        }
    }

    /**
     * Call this method when you just displayed a Batch push notification by yourself.
     *
     * @param context
     * @param intent  the gcm push intent
     */
    public void onNotificationDisplayed(Context context, Intent intent) {
        try {
            if (shouldDisplayPush(context, intent)) {
                InternalPushData pushData = InternalPushData.getPushDataForReceiverIntent(intent);

                if (pushData.getReceiptMode() == InternalPushData.ReceiptMode.DISPLAY) {
                    displayReceiptModule.scheduleDisplayReceipt(context, pushData);
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while storing push as displayed", e);
        }
    }

    /**
     * Firebase variant
     */
    public void onNotificationDisplayed(Context context, RemoteMessage message) {
        try {
            if (shouldDisplayPush(context, message)) {
                InternalPushData pushData = InternalPushData.getPushDataForFirebaseMessage(message);

                if (pushData.getReceiptMode() == InternalPushData.ReceiptMode.DISPLAY) {
                    displayReceiptModule.scheduleDisplayReceipt(context, pushData);
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while storing push as displayed", e);
        }
    }

    // -------------------------------------->

    /**
     * Should be called when push token is updated in background
     * Ex: onTokenRefresh() in a InstanceIDListenerService
     * Ex: onNewToken() in a HsmMessagingService
     */
    public void refreshRegistration() {
        final PushRegistrationProvider provider = getRegistrationProvider();
        if (provider != null) {
            requestRegistration(provider);
        }
    }

    /**
     * Get the ads identifier provider associated with the current registration provider
     *
     * @return
     */
    public AdsIdentifierProvider getAdsIdentifierProvider() {
        final PushRegistrationProvider pushProvider = getRegistrationProvider();
        if (pushProvider != null) {
            return pushProvider.getAdsIdentifierProvider();
        }
        return null;
    }

    /**
     * Get the current version of the app.<br>
     * Use context
     *
     * @return
     */
    private String getAppVersion() {
        try {
            PackageInfo packageInfo = RuntimeManagerProvider
                .get()
                .getContext()
                .getPackageManager()
                .getPackageInfo(RuntimeManagerProvider.get().getContext().getPackageName(), 0);
            return String.valueOf(packageInfo.versionCode);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while getting app version", e);
            return "";
        }
    }

    /**
     * * Request a new push registration asynchronously.
     */
    private void requestRegistration(@NonNull final PushRegistrationProvider provider) {
        final Context context = RuntimeManagerProvider.get().getContext();
        TaskExecutorProvider
            .get(context)
            .submit(
                new TaskRunnable() {
                    @Override
                    public void run() {
                        try {
                            provider.checkLibraryAvailability();
                        } catch (PushRegistrationProviderAvailabilityException e) {
                            Logger.error(
                                TAG,
                                "Provider \"" +
                                provider.getShortname() +
                                "\" could not register for push: " +
                                e.getMessage()
                            );
                            return;
                        }

                        final String registrationID = provider.getRegistration();
                        if (registrationID == null || registrationID.isEmpty()) {
                            Logger.error(TAG, "\"" + provider.getShortname() + "\" did not return a registration.");
                            return;
                        }

                        if (registrationID.length() > 4096) {
                            Logger.error(
                                TAG,
                                "\"" +
                                provider.getShortname() +
                                "\" did return a Registration ID/Push token longer than 4096, ignoring it."
                            );
                            return;
                        }

                        final Registration registration = new Registration(
                            provider.getShortname(),
                            registrationID,
                            provider.getSenderID()
                        );
                        emitRegistration(context, registration);
                    }

                    @Override
                    public String getTaskIdentifier() {
                        return "push_registration";
                    }
                }
            );
    }

    /**
     * Injects a registration ID into Batch, either from a builtin provider or the user's
     */
    private void emitRegistration(@NonNull Context context, @NonNull final Registration registration) {
        final Context appContext = context.getApplicationContext();

        // Broadcast a Batch-emitted registration intent
        final Intent i = new Intent(Batch.ACTION_REGISTRATION_IDENTIFIER_OBTAINED);
        i.putExtra(Batch.EXTRA_REGISTRATION_PROVIDER_NAME, registration.provider);
        i.putExtra(Batch.EXTRA_REGISTRATION_IDENTIFIER, registration.registrationID);
        i.putExtra(Batch.EXTRA_REGISTRATION_SENDER_ID, registration.senderID);
        i.setPackage(appContext.getPackageName());
        appContext.sendBroadcast(i, Batch.getBroadcastPermissionName(appContext));

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state != State.OFF) {
                    //TODO: Check "fromManualMode"

                    printRegistration(registration);

                    final Parameters parameters = ParametersProvider.get(appContext);
                    if (parameters != null) {
                        String currentRegistrationID = parameters.get(ParameterKeys.PUSH_REGISTRATION_ID_KEY);
                        String currentRegistrationProvider = parameters.get(
                            ParameterKeys.PUSH_REGISTRATION_PROVIDER_KEY
                        );
                        String currentSenderID = parameters.get(ParameterKeys.PUSH_REGISTRATION_SENDERID_KEY);

                        parameters.set(ParameterKeys.PUSH_APP_VERSION_KEY, getAppVersion(), true);

                        parameters.set(ParameterKeys.PUSH_REGISTRATION_PROVIDER_KEY, registration.provider, true);

                        parameters.set(ParameterKeys.PUSH_REGISTRATION_ID_KEY, registration.registrationID, true);

                        if (registration.senderID != null) {
                            parameters.set(ParameterKeys.PUSH_REGISTRATION_SENDERID_KEY, registration.senderID, true);
                        } else {
                            parameters.remove(ParameterKeys.PUSH_REGISTRATION_SENDERID_KEY);
                        }

                        if (
                            !registration.registrationID.equals(currentRegistrationID) ||
                            !registration.provider.equals(currentRegistrationProvider) ||
                            !TextUtils.equals(registration.senderID, currentSenderID)
                        ) {
                            WebserviceLauncher.launchPushWebservice(RuntimeManagerProvider.get(), registration);
                        }
                    } else {
                        Logger.internal(TAG, "Could not save push token in parameters.");
                    }
                }
            });
    }

    /**
     * Check if the Push Service is available
     *
     * @return
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isBatchPushServiceAvailable() {
        try {
            final PackageManager packageManager = RuntimeManagerProvider.get().getContext().getPackageManager();
            final Intent intent = new Intent(RuntimeManagerProvider.get().getContext(), BatchPushService.class);
            @SuppressLint("QueryPermissionsNeeded")
            List<ResolveInfo> resolveInfo = packageManager.queryIntentServices(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            );

            return resolveInfo.size() > 0;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while retrieving Push service", e);
            return false;
        }
    }

    /**
     * Check if is the app has been restricted by the user.
     * Only available on Android P and forward.
     *
     * @param context
     * @return
     */
    public static boolean isBackgroundRestricted(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                return manager.isBackgroundRestricted();
            }
        }
        return false;
    }

    private void printRegistration(@NonNull Registration registration) {
        Logger.info(TAG, "Registration ID/Push Token (" + registration.provider + "): " + registration.registrationID);
    }

    /**
     * Request the notification runtime permission
     * Required for Android 13 (api 33)
     * Do nothing if app target is lower than 13
     * @param context requesting the permission
     */
    public void requestNotificationPermission(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }
        NotificationPermissionHelper helper = new NotificationPermissionHelper();
        helper.requestPermission(context, false, null);
    }

    // ---------------------------------------->

    @Override
    public String getId() {
        return "push";
    }

    @Override
    public int getState() {
        return gcmSenderId != null ? 1 : 2;
    }

    @Override
    public void batchWillStart() {
        /*
         * Store Notif type
         */
        if (tempNotifType != null) {
            RuntimeManagerProvider
                .get()
                .run(state -> {
                    try {
                        int value = PushNotificationType.toValue(tempNotifType);
                        ParametersProvider
                            .get(RuntimeManagerProvider.get().getContext())
                            .set(ParameterKeys.PUSH_NOTIF_TYPE, Integer.toString(value), true);
                        tempNotifType = null;
                    } catch (Exception e) {
                        Logger.internal(TAG, "Error while saving temp notif type", e);
                    }
                });
        }

        if (shouldRefreshToken) {
            shouldRefreshToken = false;

            final PushRegistrationProvider provider = getRegistrationProvider();
            if (provider != null) {
                requestRegistration(provider);
            }
        }
    }

    private synchronized PushRegistrationProvider getRegistrationProvider() {
        if (!didSetupRegistrationProvider) {
            Context context = RuntimeManagerProvider.get().getContext();
            if (context != null) {
                didSetupRegistrationProvider = true;
                registrationProvider =
                    new PushRegistrationProviderFactory(context, Batch.shouldUseGoogleInstanceID(), gcmSenderId)
                        .getRegistrationProvider();

                if (registrationProvider == null) {
                    Logger.error(TAG, "Could not register for notifications.");
                }
            } else {
                Logger.internal(TAG, "No context set, cannot try to instantiate a registration provider. Will retry.");
            }
        }
        return registrationProvider;
    }
}
