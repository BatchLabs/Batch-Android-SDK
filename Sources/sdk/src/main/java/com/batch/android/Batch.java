package com.batch.android;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.ExcludedActivityHelper;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.core.NotificationAuthorizationStatus;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskExecutor;
import com.batch.android.debug.BatchDebugActivity;
import com.batch.android.debug.FindMyInstallationHelper;
import com.batch.android.di.providers.ActionModuleProvider;
import com.batch.android.di.providers.BatchModuleMasterProvider;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.di.providers.DataCollectionModuleProvider;
import com.batch.android.di.providers.EventDispatcherModuleProvider;
import com.batch.android.di.providers.InboxFetcherInternalProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.ProfileModuleProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.eventdispatcher.PushEventPayload;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.parsing.PayloadParser;
import com.batch.android.messaging.parsing.PayloadParsingException;
import com.batch.android.module.BatchModule;
import com.batch.android.module.MessagingModule;
import com.batch.android.module.OptOutModule;
import com.batch.android.module.PushModule;
import com.batch.android.runtime.RuntimeManager;
import com.batch.android.runtime.State;
import com.batch.android.user.InstallDataEditor;
import com.google.firebase.messaging.RemoteMessage;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point of the Batch library
 */
@PublicSDK
@SuppressWarnings({ "unused" })
public final class Batch {

    /**
     * Install object build on Batch start
     */
    @Nullable
    private static Install install;

    /**
     * Broadcast receiver of Batch
     */
    private static BroadcastReceiver receiver;
    /**
     * Temp intent stored to be handled at the next start
     */
    private static Intent newIntent;

    /**
     * Helper to handle excluded activities from manifest
     */
    @NonNull
    private static final ExcludedActivityHelper excludedActivityHelper = new ExcludedActivityHelper();

    /**
     * Current session ID (changed at each start)
     */
    @Nullable
    private static String sessionID;

    /**
     * Was the user already warned about being opted-out and attempting a start
     */
    private static boolean didLogOptOutWarning = false;

    /**
     * Notification tag.
     * <p>
     * Notifications displayed by Batch all have this tag.
     * Useful if you need to cancel them, for example.
     */
    public static final String NOTIFICATION_TAG = "batch";

    /**
     * Permission suffix for broadcasts triggered by Batch.
     * The actual permission is your package name + BROADCAST_PERMISSION_SUFFIX
     * <p>
     * You can call {@link #getBroadcastPermissionName(Context)} to get the full permission name.
     */
    public static final String BROADCAST_PERMISSION_SUFFIX = ".batch.permission.INTERNAL_BROADCAST";

    /**
     * Intent broadcasted locally by the SDK when a push registration identifier has been retrieved
     * (also called "Push Token").
     * <p>
     * Registration information is defined by the {@link #EXTRA_REGISTRATION_PROVIDER_NAME} and
     * {@link #EXTRA_REGISTRATION_IDENTIFIER} string extras.
     */
    public static final String ACTION_REGISTRATION_IDENTIFIER_OBTAINED =
        "com.batch.android.intent.action.push.REGISTRATION_IDENTIFIER_OBTAINED";

    /**
     * Extra containing the Registration Identifier for a {@link #ACTION_REGISTRATION_IDENTIFIER_OBTAINED}
     * broadcast
     */
    public static final String EXTRA_REGISTRATION_IDENTIFIER = "registration_id";

    /**
     * Extra containing the Registration Provider name for a {@link #ACTION_REGISTRATION_IDENTIFIER_OBTAINED}
     * broadcast
     */
    public static final String EXTRA_REGISTRATION_PROVIDER_NAME = "provider_name";

    /**
     * Extra containing the Sender ID (if any) for a {@link #ACTION_REGISTRATION_IDENTIFIER_OBTAINED}
     * broadcast
     */
    public static final String EXTRA_REGISTRATION_SENDER_ID = "sender_id";

    /**
     * One module to rule them all
     */
    @NonNull
    private static final BatchModule moduleMaster;

    static {
        moduleMaster = BatchModuleMasterProvider.get();
    }

    private Batch() {}

    /**
     * Set the API Key of Batch.<br>
     * <br>
     * You should call this method before any other, only one time.<br>
     * Typically on the onCreate of your Application class.
     *
     * @param apiKey Your Batch API Key
     */
    public static void start(@NonNull final String apiKey) {
        RuntimeManagerProvider.get().updateConfig(config -> config.setApikey(apiKey));
    }

    /**
     * Set data migrations you want to disable.
     *
     * @param migrations EnumSet of Migrations to disable. See {@link BatchMigration}
     */
    public static void disableMigration(@NonNull EnumSet<BatchMigration> migrations) {
        //noinspection ConstantConditions
        if (migrations == null) {
            Logger.error("You cannot use disableMigration with null value.");
            return;
        }
        RuntimeManagerProvider.get().updateConfig(config -> config.setMigrations(BatchMigration.toValue(migrations)));
    }

    /**
     * Set if Batch should send its logs to an object of yours (default = null)<br>
     * <br>
     * Be careful with your implementation: setting this can impact stability and performance<br>
     * You should only use it if you know what you are doing.
     *
     * @param delegate An object implementing {@link LoggerDelegate}
     */
    public static void setLoggerDelegate(@Nullable LoggerDelegate delegate) {
        RuntimeManagerProvider.get().updateConfig(config -> config.setLoggerDelegate(delegate));
    }

    /**
     * Set the log level that Batch should use
     *
     * @param level The level of the logger to use
     */
    public static void setLoggerLevel(@NonNull LoggerLevel level) {
        //noinspection ConstantConditions
        if (level == null) {
            Logger.error("You cannot setLoggerLevel with null value");
            return;
        }
        RuntimeManagerProvider.get().updateConfig(config -> config.setLoggerLevel(level));
    }

    /**
     * Configure the SDK Automatic Data Collection.
     *
     * @param editor A callback which will be called with an instance of the data collection config.
     *               Once your callback ends, Batch will persist the changes.
     */
    public static void updateAutomaticDataCollection(BatchDataCollectionConfig.Editor editor) {
        DataCollectionModuleProvider.get().updateDataCollectionConfig(editor);
    }

    /**
     * Get the id of the current session, random uuid used internally by Batch to identify the app session.
     *
     * @return session id if any, null otherwise
     */
    @Nullable
    public static String getSessionID() {
        final StringBuilder sessionID = new StringBuilder();

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (Batch.sessionID != null) {
                    sessionID.append(Batch.sessionID);
                }
            });

        if (sessionID.length() > 0) {
            return sessionID.toString();
        }

        return null;
    }

    /**
     * Copy Batch's internal data from an intent to another.
     * This is useful if you've got an activity that will not get a chance to start Batch before closing itself,
     * but don't want to break features relying on data put in the intent extras, such as direct open tracking
     * or mobile landings.
     *
     * @param from Intent to read Batch's data from
     * @param to   Intent to copy Batch's data to
     */
    public static void copyBatchExtras(@Nullable Intent from, @Nullable Intent to) {
        IntentParser.copyExtras(from, to);
    }

    /**
     * Copy Batch's internal data from intent extras to another bundle.
     * This is useful if you've got an activity that will not get a chance to start Batch before closing itself,
     * but don't want to break features relying on data put in the intent extras, such as direct open tracking
     * or mobile landings.
     *
     * @param from Intent to read Batch's data from
     * @param to   Intent to copy Batch's data to
     */
    public static void copyBatchExtras(@Nullable Bundle from, @Nullable Bundle to) {
        IntentParser.copyExtras(from, to);
    }

    /**
     * Get the broadcast permission name.
     * <p>
     * Useful if you want to register your broadcast receiver at runtime
     */
    @NonNull
    public static String getBroadcastPermissionName(@NonNull Context context) {
        return context.getPackageName() + BROADCAST_PERMISSION_SUFFIX;
    }

    /**
     * Opt Out from Batch SDK Usage. Requires Batch to be started.
     * <p>
     * Note that calling the SDK when opted out is discouraged: Some modules might behave unexpectedly
     * when the SDK is opted-out from.
     * <p>
     * Opting out will:
     * - Prevent {@link Batch#onStart(Activity)} or {@link Batch#onServiceCreate(Context, boolean)} from doing anything at all
     * - Disable any network capability from the SDK
     * - Disable all In-App campaigns
     * - Make the Inbox module return an error immediately when used
     * - Make the SDK reject any BatchUserProfile or {@link BatchProfileAttributeEditor#save()} calls
     * - Make the SDK reject calls to {@link Batch.Profile#trackEvent(String)}, {@link Batch.Profile#trackLocation(Location)} and any related methods
     * <p>
     * Even if you opt in afterwards, data that has been generated while opted out WILL be lost.
     * <p>
     * If you're also looking at deleting user data, please use {@link Batch#optOutAndWipeData(Context)}
     * <p>
     * Note that calling this method will stop Batch, effectively simulating calls to {@link Batch#onStop(Activity)}, {@link Batch#onDestroy(Activity)} and {@link Batch#onServiceDestroy(Context)}.
     * Your app should be prepared to handle these cases.
     * Some features might not be disabled until the next app start.
     */
    public static void optOut(@NonNull Context context) {
        _optOut(context, false, null);
    }

    /**
     * Opt Out from Batch SDK Usage. Requires Batch to be started.
     * <p>
     * Same as {@link Batch#optOut(Context)}, with a completion listener.
     * <p>
     * Use the listener to be informed about whether the opt-out request has been successfully sent to the server or not.
     * You'll also be able to control what to do in case of failure.
     * <p>
     * Note: if the SDK has already been opted-out from, this method will instantly call the listener with a *failure* state.
     *
     * @see Batch#optOut(Context)
     */
    public static void optOut(@NonNull Context context, @Nullable BatchOptOutResultListener listener) {
        _optOut(context, false, listener);
    }

    /**
     * Opt Out from Batch SDK Usage. Requires Batch to be started.
     * <p>
     * Same as {@link Batch#optOut(Context)} but also wipes data.
     * <p>
     * Note that calling this method will stop Batch, effectively simulating calls to {@link Batch#onStop(Activity)}, {@link Batch#onDestroy(Activity)} and {@link Batch#onServiceDestroy(Context)}.
     * Your app should be prepared to handle these cases.
     *
     * @see Batch#optOut(Context)
     */
    public static void optOutAndWipeData(@NonNull Context context) {
        _optOut(context, true, null);
    }

    /**
     * Opt Out from Batch SDK Usage. Requires Batch to be started.
     * <p>
     * Same as {@link Batch#optOut(Context)} but also wipes data.
     * <p>
     * Note that calling this method will stop Batch, effectively simulating calls to {@link Batch#onStop(Activity)}, {@link Batch#onDestroy(Activity)} and {@link Batch#onServiceDestroy(Context)}.
     * Your app should be prepared to handle these cases.
     * <p>
     * Use the listener to be informed about whether the opt-out request has been successfully sent to the server or not.
     * You'll also be able to control what to do in case of failure.
     * <p>
     * Note: if the SDK has already been opted-out from, this method will instantly call the listener with a *failure* state.
     *
     * @see Batch#optOutAndWipeData(Context)
     */
    public static void optOutAndWipeData(@NonNull Context context, @Nullable BatchOptOutResultListener listener) {
        _optOut(context, true, listener);
    }

    private static void _optOut(
        final @NonNull Context context,
        boolean wipeData,
        @Nullable final BatchOptOutResultListener listener
    ) {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }

        OptOutModuleProvider
            .get()
            .optOut(context, wipeData, listener)
            .then(value -> {
                RuntimeManager rm = RuntimeManagerProvider.get();
                rm.resetServiceRefCount();
                Activity a = rm.getActivity();
                Batch.onStop(a != null ? a : context.getApplicationContext(), false, true);
            })
            .catchException(e -> {
                if (listener != null) {
                    listener.onError();
                }
            });
    }

    /**
     * Opt In to Batch SDK Usage.
     * <p>
     * This method will be taken into account on next full application start (full process restart)
     * <p>
     * Only useful if you called {@link Batch#optOut(Context)} or {@link Batch#optOutAndWipeData(Context)} or opted out by default in the manifest
     * <p>
     * Some features might not be disabled until the next app start if you call this late into the application's life. It is strongly
     * advised to restart the application (or at least the current activity) after opting in.
     */
    public static void optIn(@NonNull Context context) {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }
        OptOutModuleProvider.get().optIn(context);
    }

    /**
     * Returns whether Batch has been opted out from or not
     * <p>
     * Warning: This method might perform some quick I/O in the caller thread.
     */
    public static boolean isOptedOut(@NonNull Context context) {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("Context can't be null");
        }
        return OptOutModuleProvider.get().isOptedOutSync(context);
    }

    /**
     * Control whether Batch should enables the Find My Installation feature (default = true)
     * <p>
     * If enabled Batch will copy the current installation id in the clipboard when the application
     * is foregrounded 5 times within 12 seconds.
     *
     * @param enabled Whether to enable the find my installation feature.
     */
    public static void setFindMyInstallationEnabled(boolean enabled) {
        FindMyInstallationHelper.isEnabled = enabled;
    }

    // ---------------------------------------------------->

    /**
     * Batch Debug module
     */
    @PublicSDK
    public static final class Debug {

        private Debug() {}

        /**
         * Start the debug activity. Requires Batch to be started.
         * You should not leave this method in your application as it is just a tool allowing developer to debug Batch info.
         * <p>
         * Warning: This method might expose critical data.
         *
         * @param context Android's context
         */
        public static void startDebugActivity(@NonNull Context context) {
            Intent intent = new Intent(context, BatchDebugActivity.class);
            context.startActivity(intent);
        }
    }

    // ---------------------------------------------------->

    /**
     * Batch Inbox module
     */
    @PublicSDK
    public static final class Inbox {

        private Inbox() {}

        /**
         * Get an inbox fetcher based on the current installation ID.
         * Batch must be started for all of the fetcher's features to work, but you can call this method before starting Batch (such as in your activity's onCreate)
         *
         * @param context A valid context. Note that the fetcher will hold on to it during its lifetime, so the application context might be more appropriate depending on your usage.
         * @return an instance of BatchInboxFetcher with the wanted configuration
         */
        @NonNull
        public static BatchInboxFetcher getFetcher(@NonNull Context context) {
            //noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            final String installID = new Install(context).getInstallID();
            return new BatchInboxFetcher(InboxFetcherInternalProvider.get(context, installID));
        }

        /**
         * Get an inbox fetcher for the specified user identifier.
         * Batch must be started for all of the fetcher's features to work, but you can call this method before starting Batch (such as in your activity's onCreate)
         *
         * @param context           A valid context. Note that the fetcher will hold on to it during its lifetime, so the application context might be more appropriate depending on your usage.
         * @param userIdentifier    User identifier for which you want the notifications
         * @param authenticationKey Secret authentication key: it should be computed your backend and given to this method
         * @return an instance of BatchInboxFetcher with the wanted configuration
         */
        @NonNull
        public static BatchInboxFetcher getFetcher(
            @NonNull Context context,
            @NonNull String userIdentifier,
            @NonNull String authenticationKey
        ) {
            //noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            return new BatchInboxFetcher(InboxFetcherInternalProvider.get(context, userIdentifier, authenticationKey));
        }
    }

    // ---------------------------------------------------->

    /**
     * Batch Push module
     */
    @PublicSDK
    public static final class Push {

        private Push() {}

        /**
         * Key to retrieve the body of a push message
         */
        public static final String BODY_KEY = "msg";
        /**
         * Key to retrieve the Alert message
         */
        public static final String ALERT_KEY = BODY_KEY;
        /**
         * Key to retrieve the Title of the message (optional)
         */
        public static final String TITLE_KEY = "title";
        /**
         * Key to retrieve the GCM payload from the activity launch intent
         */
        public static final String PAYLOAD_KEY = "batchPushPayload";

        // ------------------------------------------------>

        /**
         * Set a custom small icon resource that push notifications will use.<br>
         *
         * @param resourceId id of the resource (for example : R.drawable.push_small_icon)
         */
        public static void setSmallIconResourceId(@DrawableRes int resourceId) {
            PushModuleProvider.get().setCustomSmallIconResourceId(resourceId);
        }

        /**
         * Set a custom sound uri that push notifications will use.<br>
         * On Android 8.0, this setting will be applied to the default {@link android.app.NotificationChannel} that
         * Batch registers, meaning that you won't be able to change it after the first notification.
         * If you use your own channel, this method will have no effect.
         *
         * @param uri uri of the resource (see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSound(android.net.Uri)">Notification.Builder.setSound</a> for more details)<br>
         *            null if you want to use the default notification sound
         */
        public static void setSound(Uri uri) {
            PushModuleProvider.get().setSound(uri);
        }

        /**
         * Set a custom large icon that push notifications will use.<br>
         *
         * @param largeIcon bitmap of the large icon
         */
        public static void setLargeIcon(Bitmap largeIcon) {
            PushModuleProvider.get().setCustomLargeIcon(largeIcon);
        }

        /**
         * Get the channels manager, allowing you to tweak how notifications will behave regarding
         * the channels feature introduced in Android 8.0 (API 26)
         */
        @NonNull
        public static BatchNotificationChannelsManager getChannelsManager() {
            return BatchNotificationChannelsManagerProvider.get();
        }

        /**
         * Dismiss all notifications shown by the application.<br>
         * <br>
         * You should call this method after {@link Batch#onStart(Activity))<br>
         * <br>
         * NB : This method will dismiss even not Batch notifications,
         * this is a convenience method that you may not use if you
         * have other notifications in your app.
         */
        public static void dismissNotifications() {
            PushModuleProvider.get().dismissNotifications();
        }

        /**
         * Whether Batch should show notifications or not.
         * <p>
         * Default: true if you never used setShowNotifications() or if your context is invalid.
         * @param context Android's context
         * @return Whether Batch should show notifications or not
         */
        public static boolean shouldShowNotifications(@NonNull Context context) {
            // noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            return PushModuleProvider.get().shouldShowNotifications(context);
        }

        /**
         * Adjust the way Batch will display notifications.
         * <p>
         * You should use this method if you want avoid notifications for this user.
         * Note that Batch will remember this value, even if your Application reboots.
         * @param show Whether Batch should show notifications or not
         */
        public static void setShowNotifications(boolean show) {
            PushModuleProvider.get().setShowNotifications(show);
        }

        /**
         * Check if the received push is a Batch one. If you have a custom push implementation into your app you should
         * call this method before doing anything else into the {@link BroadcastReceiver#onReceive(Context, Intent)} method.
         * If it returns true, you should not handle the push.
         *
         * @param intent Android's intent that hold the push
         * @return true if the push is for Batch and you shouldn't handle it, false otherwise
         */
        public static boolean isBatchPush(Intent intent) {
            return PushModuleProvider.get().isBatchPush(intent);
        }

        /**
         * Check if the received push is a Batch one. If you have a custom push implementation into your app you should
         * call this method before doing anything else.
         * If it returns true, you should not handle the push.
         *
         * @param message Firebase RemoteMessage
         * @return true if the push is for Batch and you shouldn't handle it, false otherwise
         */
        public static boolean isBatchPush(RemoteMessage message) {
            return PushModuleProvider.get().isBatchPush(message);
        }

        /**
         * Set the notification accent color for Lollipop or later.
         * See <a href="http://developer.android.com/reference/android/app/Notification.html#color">Notification.color</a> for more details
         *
         * @param argbColor an ARGB integer like the constants in {@link Color}
         */
        public static void setNotificationsColor(@ColorInt int argbColor) {
            PushModuleProvider.get().setNotificationsColor(argbColor);
        }

        /**
         * Get manual display mode for push notifications. <br />
         */
        public static boolean isManualDisplayModeActivated() {
            return PushModuleProvider.get().isManualDisplayModeActivated();
        }

        /**
         * Set manual display mode for push notifications. <br />
         * <b>If you set manual display mode to true, no notifications will be shown automatically and you'll have to display it by yourself.
         *
         * @param manualDisplay Whether manual display mode is enabled or not.
         */
        public static void setManualDisplay(boolean manualDisplay) {
            PushModuleProvider.get().setManualDisplay(manualDisplay);
        }

        /**
         * Append Batch data to your open intent so that opens from this push will be tracked by Batch and displayed into your dashboard.
         * It also powers other features, such as but not limited to mobile landings.
         *
         * @param pushIntent the intent from GCM/FCM, that originated this push
         * @param openIntent the intent of the notification the will be triggered when the user clicks on it
         */
        public static void appendBatchData(Intent pushIntent, Intent openIntent) {
            PushModuleProvider.get().appendBatchData(pushIntent, openIntent);
        }

        /**
         * Append Batch data to your open intent so that opens from this push will be tracked by Batch and displayed into your dashboard.
         * It also powers other features, such as but not limited to mobile landings.
         *
         * @param pushIntentExtras the extras of the intent coming from GCM/FCM, that originated this push
         * @param openIntent       the intent of the notification the will be triggered when the user clicks on it
         */
        public static void appendBatchData(@NonNull Bundle pushIntentExtras, @NonNull Intent openIntent) {
            PushModuleProvider.get().appendBatchData(pushIntentExtras, openIntent);
        }

        /**
         * Append Batch data to your open intent so that opens from this push will be tracked by Batch and displayed into your dashboard.
         * It also powers other features, such as but not limited to mobile landings.
         *
         * @param remoteMessage the FCM message content
         * @param openIntent    the intent of the notification the will be triggered when the user clicks on it
         */
        public static void appendBatchData(@NonNull RemoteMessage remoteMessage, @NonNull Intent openIntent) {
            PushModuleProvider.get().appendBatchData(remoteMessage, openIntent);
        }

        /**
         * Make a PendingIntent suitable for notifications from a given Intent.
         * This is useful for custom receivers, or {@link BatchNotificationInterceptor} implementations.
         * <p>
         * Warning: it will override the intent's action with a unique name, to ensure that existing notifications are not updated with this PendingIntent's content.
         * If you rely on a custom action, you will have to make your own PendingIntent.
         *
         * @param context          Context. Cannot be null.
         * @param intent           The intent you want to be triggered when performing the pending intent. Must be an intent compatible with {@link PendingIntent#getActivity(Context, int, Intent, int)}. Cannot be null.
         * @param pushIntentExtras Raw extras of the push intent, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
         *                         If these extras don't have valid Batch data in it, a valid PendingIntent will still be returned, but some features might not work correctly.
         * @return A PendingIntent instance, wrapping the given Intent.
         */
        @NonNull
        @SuppressWarnings("ConstantConditions")
        public static PendingIntent makePendingIntent(
            @NonNull Context context,
            @NonNull Intent intent,
            @NonNull Bundle pushIntentExtras
        ) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            if (intent == null) {
                throw new IllegalArgumentException("Intent cannot be null");
            }

            if (pushIntentExtras == null) {
                throw new IllegalArgumentException("PushIntentExtras cannot be null");
            }

            return PushModuleProvider.get().makePendingIntent(context, intent, pushIntentExtras);
        }

        /**
         * Make a PendingIntent suitable for notifications from a given Intent.
         * This is useful for custom receivers, or {@link BatchNotificationInterceptor} implementations.
         * <p>
         * Warning: it will override the intent's action with a unique name, to ensure that existing notifications are not updated with this PendingIntent's content.
         * If you rely on a custom action, you will have to make your own PendingIntent.
         *
         * @param context       Context. Cannot be null.
         * @param intent        The intent you want to be triggered when performing the pending intent. Must be an intent compatible with {@link PendingIntent#getActivity(Context, int, Intent, int)}. Cannot be null.
         * @param remoteMessage Raw Firebase message, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
         *                      If these extras don't have valid Batch data in it, a valid PendingIntent will still be returned, but some features might not work correctly.
         * @return A PendingIntent instance, wrapping the given Intent.
         */
        @NonNull
        @SuppressWarnings("ConstantConditions")
        public static PendingIntent makePendingIntent(
            @NonNull Context context,
            @NonNull Intent intent,
            @NonNull RemoteMessage remoteMessage
        ) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            if (intent == null) {
                throw new IllegalArgumentException("Intent cannot be null");
            }

            if (remoteMessage == null) {
                throw new IllegalArgumentException("RemoteMessage cannot be null");
            }

            return PushModuleProvider.get().makePendingIntent(context, intent, remoteMessage);
        }

        /**
         * Make a PendingIntent suitable for notifications from a given deeplink. It will use Batch's builtin action activity.
         * <p>
         * This is useful for custom receivers, or {@link BatchNotificationInterceptor} implementations.
         *
         * @param context          Context. Cannot be null.
         * @param deeplink         Deeplink string. Cannot be null.
         * @param pushIntentExtras Raw extras of the push intent, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
         *                         If these extras don't have valid Batch data in it, a valid PendingIntent will still be returned, but some features might not work correctly.
         * @return A PendingIntent set to open Batch's builtin action activity to open the specified deeplink. Can be null if the deeplink is not valid.
         */
        @Nullable
        @SuppressWarnings("ConstantConditions")
        public static PendingIntent makePendingIntentForDeeplink(
            @NonNull Context context,
            @NonNull String deeplink,
            @NonNull Bundle pushIntentExtras
        ) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            if (deeplink == null) {
                throw new IllegalArgumentException("Deeplink cannot be null");
            }

            if (pushIntentExtras == null) {
                throw new IllegalArgumentException("PushIntentExtras cannot be null");
            }

            return PushModuleProvider.get().makePendingIntentForDeeplink(context, deeplink, pushIntentExtras);
        }

        /**
         * Make a PendingIntent suitable for notifications from a given deeplink. It will use Batch's builtin action activity.
         * <p>
         * This is useful for custom receivers, or {@link BatchNotificationInterceptor} implementations.
         *
         * @param context       Context. Cannot be null.
         * @param deeplink      Deeplink string. Cannot be null.
         * @param remoteMessage Raw Firebase message content, used to copy data used by Batch to power features such as direct opens, or mobile landings. Cannot be null.
         *                      If these extras don't have valid Batch data in it, a valid PendingIntent will still be returned, but some features might not work correctly.
         * @return A PendingIntent set to open Batch's builtin action activity to open the specified deeplink. Can be null if the deeplink is not valid.
         */
        @Nullable
        @SuppressWarnings("ConstantConditions")
        public static PendingIntent makePendingIntentForDeeplink(
            @NonNull Context context,
            @NonNull String deeplink,
            @NonNull RemoteMessage remoteMessage
        ) {
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            if (deeplink == null) {
                throw new IllegalArgumentException("Deeplink cannot be null");
            }

            if (remoteMessage == null) {
                throw new IllegalArgumentException("RemoteMessage cannot be null");
            }

            return PushModuleProvider.get().makePendingIntentForDeeplink(context, deeplink, remoteMessage);
        }

        /**
         * Should the developer handle and display this push, or will Batch display it?
         * Use this method to know if Batch will ignore this push, and that displaying it is your responsibility
         *
         * @return true if the push will not be processed by Batch and should be handled, false otherwise
         */
        public static boolean shouldDisplayPush(Context context, Intent intent) {
            return PushModuleProvider.get().shouldDisplayPush(context, intent);
        }

        /**
         * Should the developer handle and display this push, or will Batch display it?
         * Use this method to know if Batch will ignore this push, and that displaying it is your responsibility
         *
         * @param context Android's context
         * @param remoteMessage The Firebase message
         * @return true if the push will not be processed by Batch and should be handled, false otherwise
         */
        public static boolean shouldDisplayPush(Context context, RemoteMessage remoteMessage) {
            return PushModuleProvider.get().shouldDisplayPush(context, remoteMessage);
        }

        /**
         * Call this method to display the notification for this intent.
         *
         * @param context Android's context
         * @param intent Android's intent
         */
        public static void displayNotification(Context context, Intent intent) {
            PushModuleProvider.get().displayNotification(context, intent, null, false);
        }

        /**
         * Call this method to display the notification for this intent.
         *
         * @param context Android's context
         * @param intent Android's intent
         * @param bypassManualMode If true,  This method will ignore the manual mode value and always display the notification
         */
        public static void displayNotification(Context context, Intent intent, boolean bypassManualMode) {
            PushModuleProvider.get().displayNotification(context, intent, null, bypassManualMode);
        }

        /**
         * Call this method to display the notification for this intent.
         * Allows an interceptor to be set for this call, overriding the global one set using {@link Batch.Push#setNotificationInterceptor(BatchNotificationInterceptor)}
         *
         * @param context Android's context
         * @param intent Android's intent
         */
        public static void displayNotification(
            @NonNull Context context,
            @NonNull Intent intent,
            @Nullable BatchNotificationInterceptor interceptor
        ) {
            PushModuleProvider.get().displayNotification(context, intent, interceptor, false);
        }

        /**
         * Call this method to display the notification for this intent.
         * Allows an interceptor to be set for this call, overriding the global one set using {@link Batch.Push#setNotificationInterceptor(BatchNotificationInterceptor)}
         *
         * @param context Android's context
         * @param intent Android's intent
         * @param interceptor An optional notification interceptor
         * @param bypassManualMode If true,  This method will ignore the manual mode value and always display the notification
         */
        public static void displayNotification(
            @NonNull Context context,
            @NonNull Intent intent,
            @Nullable BatchNotificationInterceptor interceptor,
            boolean bypassManualMode
        ) {
            PushModuleProvider.get().displayNotification(context, intent, interceptor, bypassManualMode);
        }

        /**
         * Call this method to display the notification for this message.
         */
        public static void displayNotification(Context context, RemoteMessage remoteMessage) {
            PushModuleProvider.get().displayNotification(context, remoteMessage, null);
        }

        /**
         * Call this method to display the notification for this message.
         * Allows an interceptor to be set for this call, overriding the global one set using {@link Batch.Push#setNotificationInterceptor(BatchNotificationInterceptor)}
         */
        public static void displayNotification(
            @NonNull Context context,
            @NonNull RemoteMessage remoteMessage,
            @Nullable BatchNotificationInterceptor interceptor
        ) {
            PushModuleProvider.get().displayNotification(context, remoteMessage, interceptor);
        }

        /**
         * Sets additional intent flags for notifications.
         * Doesn't work for external deeplinks.
         *
         * @param flags Additional flags. "null" to clear.
         */
        public static void setAdditionalIntentFlags(Integer flags) {
            PushModuleProvider.get().setAdditionalIntentFlags(flags);
        }

        /**
         * Call this method when you just displayed a Batch push notification by yourself.
         *
         * @param context Android's context
         * @param intent  the gcm push intent
         */
        public static void onNotificationDisplayed(Context context, Intent intent) {
            PushModuleProvider.get().onNotificationDisplayed(context, intent);
        }

        /**
         * Call this method when you just displayed a Batch push notification by yourself.
         *
         * @param context Android's context
         * @param remoteMessage The Firebase message
         */
        public static void onNotificationDisplayed(Context context, RemoteMessage remoteMessage) {
            PushModuleProvider.get().onNotificationDisplayed(context, remoteMessage);
        }

        /**
         * Get the current push registration.
         * <p>
         * The returned registration might be outdated and invalid if this method is called
         * too early in your application lifecycle.
         * <p>
         * Batch <b>MUST</b> be started in order to use this method.
         *
         * @return A push registration, null if unavailable.
         */
        @Nullable
        public static BatchPushRegistration getRegistration() {
            return PushModuleProvider.get().getRegistration();
        }

        /**
         * Set a notification interceptor. It allows you to tweak various parts of a notification that Batch generates before displaying it.
         *
         * @param interceptor A {@link BatchNotificationInterceptor} subclass. Null to remove a previously set one.
         */
        public static void setNotificationInterceptor(@Nullable BatchNotificationInterceptor interceptor) {
            PushModuleProvider.get().setNotificationInterceptor(interceptor);
        }

        /**
         * Force Batch to renew the push token.
         * You should not be calling this method unless we told you to.
         */
        public static void refreshRegistration() {
            PushModuleProvider.get().refreshRegistration();
        }

        /**
         * Request the notification runtime permission.
         * <p>
         * Android 13 (API 33) introduced a new runtime permission for notifications called POST_NOTIFICATIONS.
         * Without this permission, apps on Android 13 cannot show notifications.
         * <p>
         * Note: This method does nothing on Android 12 and lower, or if your application does not target API 33 or higher.
         * <p>
         * @param context requesting the permission
         */
        public static void requestNotificationPermission(@NonNull Context context) {
            PushModuleProvider.get().requestNotificationPermission(context, null);
        }

        /**
         * Request the notification runtime permission.
         * <p>
         * Android 13 (API 33) introduced a new runtime permission for notifications called POST_NOTIFICATIONS.
         * Without this permission, apps on Android 13 cannot show notifications.
         * <p>
         * Note: This method does nothing on Android 12 and lower, or if your application does not target API 33 or higher.
         * <p>
         * @param context requesting the permission
         * @param listener Callback notifying whether the permission has been granted or not. Note that the permission will be considered as granted on Android 12 and lower. Listener will not be triggered if your application does not target API 33 or higher.
         */
        public static void requestNotificationPermission(
            @NonNull Context context,
            @Nullable BatchPermissionListener listener
        ) {
            PushModuleProvider.get().requestNotificationPermission(context, listener);
        }
    }

    // ---------------------------------------------------->

    /**
     * Batch EventDispatcher module
     */
    @PublicSDK
    public static final class EventDispatcher {

        private EventDispatcher() {}

        /**
         * Add an event dispatcher.
         * The Batch SDK must be opt-in for the dispatcher to receive events.
         *
         * @param dispatcher The Batch Event Dispatcher to add
         */
        public static void addDispatcher(BatchEventDispatcher dispatcher) {
            EventDispatcherModuleProvider.get().addEventDispatcher(dispatcher);
        }

        /**
         * Remove an event dispatcher.
         *
         * @param dispatcher The Batch Event Dispatcher to remove
         */
        public static boolean removeDispatcher(BatchEventDispatcher dispatcher) {
            return EventDispatcherModuleProvider.get().removeEventDispatcher(dispatcher);
        }

        /**
         * Represents the type of the dispatched event in {@link BatchEventDispatcher#dispatchEvent(Type, Payload)}.
         * Declared under Batch.EventDispatcher to avoid ambiguity with {@link BatchEventAttributes}.
         */
        @PublicSDK
        public enum Type {
            NOTIFICATION_DISPLAY,
            NOTIFICATION_DISMISS,
            NOTIFICATION_OPEN,
            MESSAGING_SHOW,
            MESSAGING_CLOSE,
            MESSAGING_AUTO_CLOSE,
            MESSAGING_CLOSE_ERROR,
            MESSAGING_CLICK,
            MESSAGING_WEBVIEW_CLICK;

            public boolean isNotificationEvent() {
                return this == NOTIFICATION_OPEN || this == NOTIFICATION_DISPLAY || this == NOTIFICATION_DISMISS;
            }

            public boolean isMessagingEvent() {
                return !isNotificationEvent();
            }
        }

        /**
         * Accessor to the payload of the dispatched event in {@link BatchEventDispatcher#dispatchEvent(Type, Payload)}.
         * Declared under Batch.EventDispatcher to avoid ambiguity with {@link BatchEventAttributes} and {@link BatchPushPayload}.
         */
        @PublicSDK
        public interface Payload {
            /**
             * Get the tracking ID associated with the event.
             * Only set for in-app, see {@link Type#isNotificationEvent()}.
             *
             * @return the tracking id or null if none is set.
             */
            @Nullable
            String getTrackingId();

            /**
             * Get the button analytics ID associated with the event.
             * Only used for messages of types {@link Type#MESSAGING_WEBVIEW_CLICK} or
             * {@link Type#MESSAGING_CLOSE}.
             * Matches the "analyticsID" parameter of various methods of the JavaScript SDK.
             *
             * @return the button analytics ID or null if none is available.
             */
            @Nullable
            String getWebViewAnalyticsID();

            /**
             * Get the deeplink url associated with the event.
             * Only set for {@link Type#MESSAGING_CLICK}, {@link Type#NOTIFICATION_OPEN} and {@link Type#NOTIFICATION_DISPLAY}
             *
             * @return the deeplink for the event or null if none is available.
             */
            @Nullable
            String getDeeplink();

            /**
             * Indicate if the action associated with the event is positive.
             * A positive action is :
             * - An Open for a push campaign
             * - A CTA click or Global tap containing a deeplink or a custom action for a messaging campaign
             *
             * @return Whether it is a positive action or not
             */
            boolean isPositiveAction();

            /**
             * Get a value from a key within the custom payload associated with the event.
             *
             * @param key The key of the value to get
             * @return the corresponding value or null if none is set.
             */
            @Nullable
            String getCustomValue(@NonNull String key);

            /**
             * Get the raw payload associated with the event
             *
             * @return the payload or null if the event is not a message
             */
            @Nullable
            BatchMessage getMessagingPayload();

            /**
             * Get the raw payload associated with the event
             *
             * @return the payload or null if the event is not a push notification
             */
            @Nullable
            BatchPushPayload getPushPayload();
        }
    }

    // ---------------------------------------------------->

    /**
     * Batch User module
     */
    @PublicSDK
    public static final class User {

        private User() {}

        /**
         * Get the unique installation ID, generated by Batch. Batch must be started to read it.
         *
         * @return Batch-generated installation ID. Might be null if Batch isn't started.
         */
        @Nullable
        public static String getInstallationID() {
            // Store as a local variable so that install can't change between the null check and return.
            Install install = Batch.install;
            if (install != null) {
                return install.getInstallID();
            }

            return null;
        }

        /**
         * Read the language.
         *
         * @return The custom language set with {@link InstallDataEditor}. Returns null by default.
         */
        @Nullable
        public static String getLanguage(@NonNull Context context) {
            //noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            return UserModuleProvider.get().getLanguage(context);
        }

        /**
         * Read the region.
         *
         * @return The custom region set with {@link InstallDataEditor}. Returns null by default.
         */
        @Nullable
        public static String getRegion(@NonNull Context context) {
            //noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }
            return UserModuleProvider.get().getRegion(context);
        }

        /**
         * Read the custom identifier.
         *
         * @return The custom identifier set with {@link InstallDataEditor}. Returns null by default.
         */
        @Nullable
        public static String getIdentifier(@NonNull Context context) {
            //noinspection ConstantConditions
            if (context == null) {
                throw new IllegalArgumentException("Context cannot be null");
            }

            return UserModuleProvider.get().getCustomID(context);
        }

        /**
         * Read the saved attributes. Reading is asynchronous so as not to interfere with saving operations.
         *
         * @param context Android's context
         * @param listener Pass a listener to be notified of the fetch result.
         */
        public static void fetchAttributes(
            @NonNull final Context context,
            @Nullable final BatchAttributesFetchListener listener
        ) {
            UserDataAccessor.fetchAttributes(context, listener, true);
        }

        /**
         * Read the saved tag collections. Reading is asynchronous so as not to interfere with saving operations.
         *
         * @param context Android's context
         * @param listener Pass a listener to be notified of the fetch result.
         */
        public static void fetchTagCollections(
            @NonNull final Context context,
            @Nullable final BatchTagCollectionsFetchListener listener
        ) {
            UserDataAccessor.fetchTagCollections(context, listener, true);
        }

        /**
         * Clear all tags and attributes set on an installation and their local cache returned by fetchAttributes and
         * fetchTagCollections. This doesn't affect data set on profiles using Batch.Profile.
         */
        public static void clearInstallationData() {
            UserModuleProvider.get().clearInstallationData();
        }
    }

    /**
     * Batch Profile module
     */
    @PublicSDK
    public static final class Profile {

        private Profile() {}

        /**
         * Identify the user's installation with an omnichannel profile.
         *
         * @param identifier the custom user identifier or null to erase
         */
        public static void identify(@Nullable String identifier) {
            ProfileModuleProvider.get().identify(identifier);
        }

        /**
         * Get a profile attribute editor.
         * <p>
         * Batch must be started to save it.
         * Note that you should chain calls to the returned editor.
         * If you call this method again, you will get another editor that's not aware of changes made elsewhere that have not been saved.
         *
         * @return A BatchProfileAttributeEditor instance.
         */
        @NonNull
        public static BatchProfileAttributeEditor editor() {
            return new BatchProfileAttributeEditor();
        }

        /**
         * Track an event.
         * You can call this method from any thread. Batch must be started at some point, or events won't be sent to the server.
         *
         * @param event The event name.
         */
        public static void trackEvent(@NonNull String event) {
            ProfileModuleProvider.get().trackPublicEvent(event, null);
        }

        /**
         * Track an event.
         * You can call this method from any thread. Batch must be started at some point, or events won't be sent to the server.
         *
         * @param event The event name.
         * @param attributes  The event attributes. Can be null.
         */
        public static void trackEvent(@NonNull String event, @Nullable BatchEventAttributes attributes) {
            ProfileModuleProvider.get().trackPublicEvent(event, attributes);
        }

        /**
         * Track a location update.
         * You can call this method from any thread. Batch must be started at some point, or location updates won't be sent to the server.<br>
         * The location object usually comes from the location system service, or the Fused Location API, but can also manually create it.<br>
         * If you manually create it, please make sure to fill the accuracy and date if you have that data: both of these values are used
         * to improve the targeting.
         *
         * @param location The location. Can't be null.
         */
        public static void trackLocation(@NonNull Location location) {
            UserModuleProvider.get().trackLocation(location);
        }
    }

    /**
     * Batch Messaging module
     */
    @PublicSDK
    public static final class Messaging {

        /**
         * CTA Index representing a global tap action
         */
        public static final String GLOBAL_TAP_ACTION_INDEX = "mepCtaIndex:-1"; // was -1

        private Messaging() {}

        //region: Listener

        /**
         * Listener interface for messaging views lifecycle events.
         * <p>
         * Implement this if you want to be notified of what happens to the messaging view (for example, perform some analytics on show/hide).
         * You're also <b>required</b> to implement this if you want to add actions with a "callback" type.
         */
        @PublicSDK
        public interface LifecycleListener {
            /**
             * Enum for the different reasons why an In-App message can be closed.
             */
            @PublicSDK
            enum MessagingCloseReason {
                /**
                 * The message was closed automatically from auto dismiss feature.
                 */
                Auto,

                /**
                 * The message was closed by the user (like clicking on the close or back button).
                 */
                User,

                /**
                 * The message was closed because the user clicked on a CTA.
                 */
                Action,

                /**
                 * The message was closed because of an error
                 * (for example, a message with only one image in it that fails to be downloaded).
                 */
                Error,
            }

            /**
             * Called when the message view appeared on screen.
             *
             * @param messageIdentifier Analytics message identifier string. Can be null.
             */
            void onBatchMessageShown(@Nullable String messageIdentifier);

            /**
             * Called when the message view will be dismissed due to the user pressing a CTA or the global tap action.
             *
             * @param messageIdentifier Analytics message identifier string. Can be null.
             * @param ctaIdentifier     Identifier of the action/CTA.
             *                          If the action comes from the "global tap action", the identifier will be {@link #GLOBAL_TAP_ACTION_INDEX} (for MEP messages only).
             *                          If the identifier isn't null, you can cast the action to {@link BatchMessageCTA} to get the CTA's label.
             *                          If the action comes from a WebView, the identifier will be the Analytics identifier. Matches the "analyticsID" parameter of the Javascript call,
             *                          or the 'batchAnalyticsID' get parameter of a link.
             * @param action            Action that will be performed. Fields can be null if the action was only to dismiss the message on tap.
             *                          DO NOT run the action yourself: the SDK will automatically do it.
             */
            default void onBatchMessageActionTriggered(
                @Nullable String messageIdentifier,
                @Nullable String ctaIdentifier,
                @NonNull BatchMessageAction action
            ) {}

            /**
             * Called when the message view disappeared from the screen.
             *
             * @param messageIdentifier Analytics message identifier string. Can be null.
             * @param reason Reason why the message was closed.
             */
            void onBatchMessageClosed(@Nullable String messageIdentifier, MessagingCloseReason reason);
        }

        /**
         * Interface to intercept In-App Messages before they are displayed.
         * <p>
         * This interface can be used to cancel the automatic display of a message
         * and allow manual handling. It is called when the SDK is about to
         * display an In-App message.
         */
        @PublicSDK
        public interface InAppInterceptor {
            /**
             * Called when an In-App Message is about to be displayed, giving a chance to cancel it and
             * handle it manually.
             * <p>
             * This method can be called from any thread.
             *
             * @param message The In-App message about to be displayed
             * @return true if you handled the message, false if you want to let Batch continue processing it.
             * Returning true will prevent this message from being automatically displayed: it then
             * becomes your responsibility to do so, if you wish.
             */
            boolean onBatchInAppMessageReady(@NonNull BatchInAppMessage message);
        }

        //endregion

        //region: Activity display hint

        enum DisplayHintStrategy {
            TRANSVERSE_HIERARCHY,
            EMBED,
        }

        @PublicSDK
        public interface DisplayHintProvider {
            DisplayHint getBatchMessageDisplayHint(BatchMessage message);
        }

        @PublicSDK
        public static class DisplayHint {

            DisplayHintStrategy strategy;
            View view;

            private DisplayHint(@NonNull View view, @NonNull DisplayHintStrategy strategy) {
                this.strategy = strategy;
                this.view = view;
            }

            /**
             * Automatically find an appropriate container:
             * Batch will use the given view as a base to walk up the view tree until it reaches the window's content view, or a CoordinatorLayout.
             */
            @SuppressWarnings("ConstantConditions")
            public static DisplayHint findUsingView(@NonNull View view) {
                if (view == null) {
                    throw new IllegalArgumentException("view cannot be null");
                }

                return new DisplayHint(view, DisplayHintStrategy.TRANSVERSE_HIERARCHY);
            }

            /**
             * Embed display hint:
             * Batch will embed the banner in the given FrameLayout, no questions asked.
             */
            @SuppressWarnings("ConstantConditions")
            public static DisplayHint embed(@NonNull FrameLayout layout) {
                if (layout == null) {
                    throw new IllegalArgumentException("layout cannot be null");
                }

                return new DisplayHint(layout, DisplayHintStrategy.EMBED);
            }
        }

        //endregion

        //region: Public methods

        /**
         * Toggle whether mobile landings should be shown directly rather than displaying a notification
         * when the app is in foreground.
         * <p>
         * Default is false.
         *
         * @param showForegroundLandings True to enable show landings, false to display a notification like when the application is in the background.
         */
        public static void setShowForegroundLandings(boolean showForegroundLandings) {
            MessagingModuleProvider.get().setShowForegroundLandings(showForegroundLandings);
        }

        /**
         * Toggle this module's automatic mode. By default, this value is "true".
         * <p>
         * If you disable automatic mode, you will have to implement {@link #loadMessagingView(Context, BatchMessage)}
         *
         * @param automatic True to enable the automatic mode, false to disable it.
         */
        public static void setAutomaticMode(boolean automatic) {
            MessagingModuleProvider.get().setAutomaticMode(automatic);
        }

        /**
         * Override the {@link Typeface} (aka font) used by Batch's messaging views.
         * You'll need to provide both the normal and bold typefaces. If you only provide one, the result might be inconsistent.<br/>
         * <p>
         * In order to revert to the system font, set the typefaces to null.
         *
         * @param normalTypeface Typeface for normal text.
         * @param boldTypeface   Typeface for bold text.
         */
        public static void setTypefaceOverride(@Nullable Typeface normalTypeface, @Nullable Typeface boldTypeface) {
            MessagingModuleProvider.get().setTypefaceOverride(normalTypeface, boldTypeface);
        }

        /**
         * Set a lifecycle listener. For more information about what a lifecycle listener is useful for, look at {@link LifecycleListener}'s documentation.
         *
         * @param listener A {@link LifecycleListener} implementation. null to remove a previously set one.
         */
        public static void setLifecycleListener(@Nullable LifecycleListener listener) {
            MessagingModuleProvider.get().setLifecycleListener(listener);
        }

        /**
         * Set an In-App interceptor. For more information about what an In-App interceptor is useful for, look at {@link InAppInterceptor}'s documentation.
         *
         * @param interceptor An {@link InAppInterceptor} implementation. null to remove a previously set one.
         */
        public static void setInAppInterceptor(@Nullable InAppInterceptor interceptor) {
            MessagingModuleProvider.get().setInAppInterceptor(interceptor);
        }

        /**
         * Load the {@link BatchMessagingView} corresponding to the message payload.
         * <p>
         * This method should be called from your UI thread.
         * <p>
         * Note that this method will not display the message.
         * You will have to call {@link BatchMessagingView#showView(Activity)}
         * or {@link BatchMessagingView#showFragment(FragmentActivity, String)}
         * according to the {@link BatchMessagingView#kind}.
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
         * @param context Your activity's context. Can't be null.
         * @param message Message to display. Can't be null.
         * @return A BatchMessagingView instance.
         * @throws BatchMessagingException When loading fail
         */
        @NonNull
        public static BatchMessagingView loadMessagingView(@NonNull Context context, @NonNull BatchMessage message)
            throws BatchMessagingException {
            try {
                JSONObject json = message.getJSON();
                MessagingModule messagingModule = MessagingModuleProvider.get();
                Message msg = PayloadParser.parseUnknownLandingMessage(json);
                if (msg.isBannerMessage()) {
                    return new BatchMessagingView(
                        BatchMessagingView.Kind.View,
                        messagingModule.loadBanner(context, message, json)
                    );
                }
                return new BatchMessagingView(
                    BatchMessagingView.Kind.Fragment,
                    messagingModule.loadFragment(context, message, json)
                );
            } catch (PayloadParsingException e) {
                throw new BatchMessagingException("Could not read base payload from message");
            }
        }

        /**
         * Asynchronously show the {@link BatchMessage}. It will be displayed in a dedicated activity ({@link MessagingActivity}).
         * <p>
         * Note that if this method will work even if Batch is in do not disturb mode.
         * <p>
         * The given context should be an Activity instance to enable support for the banner format, as it
         * has to be attached to an activity. If you want to tweak how the SDK displays a banner, you can implement
         * {@link Batch.Messaging.DisplayHintProvider} in your activity.
         *
         * @param context Your activity's context, Can't be null.
         * @param message Message to show. Can't be null.
         */
        @SuppressWarnings("ConstantConditions")
        public static void show(@NonNull Context context, @NonNull BatchMessage message) {
            if (context == null) {
                throw new IllegalArgumentException("context can't be null");
            }
            if (message == null) {
                throw new IllegalArgumentException("message can't be null");
            }
            MessagingModuleProvider.get().displayMessage(context, message, true);
        }

        /**
         * Toggles whether Batch.Messaging should enter its "do not disturb" (DnD) mode, or exit it.
         * <p>
         * While in DnD, Batch will not display landings, not matter if they've been triggered by notifications or an In-App Campaign, even in automatic mode.
         * </p><p>
         * This mode is useful for times where you don't want Batch to interrupt your user, such as during a splashscreen, a video or an interstitial ad.
         * </p><p>
         * If a message should have been displayed during DnD, Batch will enqueue it, overwriting any previously enqueued message.<br/>
         * When exiting DnD, Batch will not display the message automatically: you'll have to call the queue management methods to display the message, if you want to.<br/>
         * See {@link #hasPendingMessage()}, {@link #popPendingMessage()} to manage the enqueued message, if any.
         * </p><p>
         * Note: This is only supported if automatic mode is disabled. Messages will not be enqueued, as they will be delivered to your {@link InAppInterceptor} implementation.
         * </p>
         *
         * @param enableDnd true to enable DnD, false to disable it
         */
        public static void setDoNotDisturbEnabled(boolean enableDnd) {
            MessagingModuleProvider.get().setDoNotDisturbEnabled(enableDnd);
        }

        /**
         * Check if Batch Messaging is currently in Do Not Disturb mode
         */
        public static boolean isDoNotDisturbEnabled() {
            return MessagingModuleProvider.get().isDoNotDisturbEnabled();
        }

        /**
         * Check if Batch currently has a pending message, without forgetting it.
         *
         * @return true if a message is pending, false otherwise.
         */
        public static boolean hasPendingMessage() {
            return MessagingModuleProvider.get().hasPendingMessage();
        }

        /**
         * Gets the currently enqueued message.
         * <p>
         * Note: Calling this removes the pending message from Batch's queue. Further calls to this
         * method will return null, until another message gets enqueues.
         *
         * @return The enqueued message, if any.
         */
        @Nullable
        public static BatchMessage popPendingMessage() {
            return MessagingModuleProvider.get().popPendingMessage();
        }
        //endregion
    }

    /**
     * Batch Action manager
     */
    @PublicSDK
    public static final class Actions {

        private Actions() {}

        /**
         * Register an action with Batch.
         * <br/>
         * If an action already exists for that identifier, it will be replaced. Identifiers are not case-sensitive.
         * Note that the action identifier cannot start with "batch.", as they are reserved by the SDK.
         * Trying to register such an action will throw an exception.
         *
         * @param userAction The action to register
         */
        public static void register(@NonNull UserAction userAction) {
            ActionModuleProvider.get().registerAction(userAction);
        }

        /**
         * Unregister an action from Batch.
         * <br/>
         * Trying to unregister an action that has not be unregistered will silently fail.
         * Note that trying to unregister an action that starts with "batch." will throw an exception.
         *
         * @param identifier The action identifier. Identifiers are not case-sensitive.
         */
        public static void unregister(@NonNull String identifier) {
            ActionModuleProvider.get().unregisterAction(identifier);
        }

        /**
         * Add an alias to a drawable that can be referenced remotely.
         * <br/>
         * This is used for CTAs (buttons) showing in the push notifications themselves. While you can directly set a drawable resource when making your notification,
         * it is recommended that you add a set of aliases. That way, you can change the drawable file name between versions without having to think about that when
         * pushing different versions of your applications. You can also use aliases to make the icon names more readable or useful to anybody sending notifications to your app.
         * <br/>
         * Note that since Android N, notifications with button will not display icons anymore.
         *
         * @param alias      Drawable alias. Not case sensitive
         * @param drawableID Drawable resource ID to use
         */
        public static void addDrawableAlias(@NonNull String alias, @DrawableRes int drawableID) {
            ActionModuleProvider.get().addDrawableAlias(alias, drawableID);
        }

        /**
         * Perform an action by its identifier.
         * <br/>
         * Note: The action will have a null source.
         *
         * @param actionIdentifier Action identifier. Batch internal actions cannot be called using this method.
         * @param arguments        Action arguments. Optional.
         * @return true if an action was registered for this identifier and performed, false otherwise.
         */
        public static boolean performAction(
            @NonNull Context context,
            @NonNull String actionIdentifier,
            @Nullable JSONObject arguments
        ) {
            return ActionModuleProvider.get().performUserAction(context, actionIdentifier, arguments);
        }

        /**
         * Set a deeplink interceptor. It allows you to tweak how Batch will open your activity.
         *
         * @param interceptor A {@link BatchDeeplinkInterceptor} interface. Null to remove a previously set one.
         */
        public static void setDeeplinkInterceptor(@Nullable BatchDeeplinkInterceptor interceptor) {
            ActionModuleProvider.get().setDeeplinkInterceptor(interceptor);
        }
    }

    /**
     * Method to call on your main activity {@link Activity#onCreate(Bundle)}.
     *
     * @param activity Created activity
     */
    public static void onCreate(final Activity activity) {
        if (activity == null) {
            return;
        }

        // Check if the activity should be excluded from batch's lifecycle and save the intent if needed
        if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
            excludedActivityHelper.saveIntentIfNeeded(activity);
            Logger.internal("Created activity has exclusion meta-data");
        }
    }

    /**
     * Method to call on your main activity {@link Activity#onStart()} to start Batch and support URL scheme events.<br>
     * You must call this method before any other on Batch.<br>
     * <br>
     * Will fail and log an Error if <ul>
     * <li>Given {@code activity} is null</li>
     * <li>You call it before calling {@link Batch#start(String)}</li>
     * <li>Your app doesn't have {@code android.permission.INTERNET} permission</li>
     * </ul>
     *
     * @param activity The activity that's starting
     */
    public static void onStart(final Activity activity) {
        doBatchStart(activity, false, true);
    }

    /**
     * Method to call on your service {@link Service#onCreate()} to start Batch.<br>
     * You must call this method before any other on Batch.<br>
     * Note that all Batch functionality is not available in this mode. See the documentation for more info.<br>
     * <br>
     * Using this method, you'll also be able to control whether this start should count as user activity or not.
     * This might impact Analytics.
     * <br>
     * Will fail and log an Error if <ul>
     * <li>Given {@code context} is null</li>
     * <li>You call it before calling {@link Batch#start(String)}</li>
     * <li>Your app doesn't have {@code android.permission.INTERNET} permission</li>
     * </ul>
     *
     * @param context      The service or application context
     * @param userActivity If the start comes from user activity or for background use only
     */
    public static void onServiceCreate(final Context context, final boolean userActivity) {
        doBatchStart(context, true, userActivity);
    }

    /**
     * Method to call on your service {@link Service#onDestroy()} to stop Batch.<br>
     * Calling this method if Batch is already stopped or not started will do nothing<br>
     * <br>
     *
     * @param context The service or application context
     */
    public static void onServiceDestroy(final Context context) {
        onStop(context, true, true);
    }

    /**
     * Method to call on your main activity {@link Activity#onNewIntent(Intent)}<br>
     * Calling this method if Batch is already stopped or not started will do nothing
     *
     * @param intent Android's intent
     */
    public static void onNewIntent(final Activity activity, final Intent intent) {
        newIntent = intent;
        doBatchStart(activity, false, true);
    }

    /**
     * Method to call on your main activity {@link Activity#onStop()}<br>
     * Calling this method if Batch is already stopped or not started will do nothing
     *
     * @param activity the activity that generate the onStop event
     */
    public static void onStop(Activity activity) {
        onStop(activity, false, false);
    }

    /**
     * Method to call on your main activity {@link Activity#onDestroy()}<br>
     * Calling this method if Batch is already stopped or not started will do nothing
     *
     * @param activity the activity that generate the onDestroy event
     */
    public static void onDestroy(Activity activity) {
        onStop(activity, false, true);
    }

    /**
     * Start Batch with a context
     */
    private static void doBatchStart(final Context context, final boolean bumpRetainCount, final boolean userActivity) {
        final AtomicBoolean fromPush = new AtomicBoolean(false);
        final StringBuilder pushId = new StringBuilder();
        final RuntimeManager runtimeManager = RuntimeManagerProvider.get();

        boolean hasStarted = RuntimeManagerProvider
            .get()
            .changeState((state, config) -> {
                if (config == null) {
                    Logger.error(
                        "You must set the configuration before starting Batch. Please call setConfig on onCreate of your Application subclass"
                    );
                    return null;
                }
                // Get the last stop if any (if we were background, not killed)
                Long lastStop = runtimeManager.onStart();
                boolean shouldStart = true;

                Date lastUserStart = runtimeManager.getLastUserStartDate();

                if (!userActivity || lastUserStart != null) { // Force a start if Batch was started but without user activity and is now started with userActivity
                    if (lastStop != null && lastStop > new Date().getTime() - 30 * 1000) { // If last stop is less than 30s old, don't call start WS (we can come from a subactivity)
                        shouldStart = false;
                    }
                    // If no last stop but already running, it's a subactivity so don't start
                    // Except if the last user active start happened more than 23h ago (if one happened at all)
                    else if (lastStop == null && state == State.READY) {
                        if (lastUserStart != null) {
                            // Don't make that into a single if with &&, because if this one is false,
                            // shouldStart will be true
                            if (lastUserStart.getTime() > new Date().getTime() - 82800 * 1000) {
                                shouldStart = false;
                            }
                        } else {
                            shouldStart = false;
                        }
                    }
                }

                // Simulate last stop
                if (lastStop != null && shouldStart) {
                    TrackerModuleProvider.get().track(InternalEvents.STOP, lastStop);
                }

                Logger.internal("onStart called on state " + state + ", should start : " + shouldStart);

                if (context == null) {
                    Logger.error("Batch start called with null context, aborting start");
                    return null;
                }

                if (OptOutModuleProvider.get().isOptedOutSync(context)) {
                    if (!didLogOptOutWarning) {
                        didLogOptOutWarning = true;
                        Logger.error("Batch was opted out from: refusing to start.");
                    }
                    return null;
                }

                // At this point, retain should count, even if we don't fire a WS
                if (bumpRetainCount) {
                    runtimeManager.incrementServiceRefCount();
                }

                IntentParser intentParser = null;
                if (context instanceof Activity) {
                    final Activity activity = (Activity) context;

                    if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
                        Logger.internal("Started activity has exclusion meta-data, aborting start.");
                        return null;
                    }

                    /*
                     * If we've already been started by an Activity, and the new one is translucent or floating
                     * (which means that the underlying activity's onStart and onStop will not be called)
                     * then do not set this activity as the retained one.
                     * This fixes an issue where transparent activities would make Batch incorrectly stop.
                     *
                     * If a translucent or floating activity is the first one, disregard this.
                     * This introduces a edge case where if the transparent activity is the first one, Batch will stop when returning on it.
                     *
                     * TODO (arnaud): This is a quick hack and must be reworked in a future version
                     */
                    boolean shouldRetainActivityInstance = true;

                    try {
                        if (state != State.OFF && runtimeManager.getActivity() != null) {
                            if (activity.getWindow().isFloating() || activity instanceof MessagingActivity) {
                                shouldRetainActivityInstance = false;
                            } else {
                                TypedArray a = activity
                                    .getTheme()
                                    .obtainStyledAttributes(new int[] { android.R.attr.windowIsTranslucent });
                                shouldRetainActivityInstance = !a.getBoolean(0, false); // <=> !isTranslucent. Condition is inverted since a translucent activity should NOT be retained
                                a.recycle();
                            }
                        }
                    } catch (Exception e) {
                        Logger.internal(
                            "Error while trying to check if the current activity is transparent/floating. Reverting to default behaviour.",
                            e
                        );
                    }

                    /*
                     * Set activity & listeners
                     */
                    if (shouldRetainActivityInstance) {
                        runtimeManager.setActivity(activity);
                    }

                    /*
                     * List the possible intents with push payload by priority :
                     * 1 - Intent from onNewIntent()
                     * 2 - Intent from the starting activity
                     * 3 - Intent saved from an activity with exclusion meta-data
                     */
                    List<IntentParser> intentParsers = new ArrayList<>();
                    intentParser = new IntentParser(activity);
                    if (newIntent != null) {
                        Logger.internal("Adding intent from onNewIntent");
                        intentParsers.add(new IntentParser(newIntent));
                    }
                    intentParsers.add(intentParser);
                    if (excludedActivityHelper.hasIntent()) {
                        Logger.internal("Adding intent from an activity with exclusion meta-data");
                        intentParsers.add(new IntentParser(excludedActivityHelper.popIntent()));
                    }
                    // Use the first intent with push payload found
                    for (IntentParser parser : intentParsers) {
                        if (parser.hasPushPayload()) {
                            intentParser = parser;
                            break;
                        }
                    }
                    if (intentParser.hasPushPayload()) {
                        Logger.internal("Activity has a push payload");
                    } else {
                        Logger.internal("Activity does not have a push payload");
                    }
                    fromPush.set(intentParser.comesFromPush());
                    if (fromPush.get()) {
                        shouldStart = true; // if we comes from push, always start

                        String intentPushId = intentParser.getPushId();
                        if (intentPushId != null) {
                            pushId.append(intentPushId);
                        }
                    }

                    if (MessagingModuleProvider.get().isInAutomaticMode()) {
                        if (intentParser.hasLanding()) {
                            if (intentParser.isLandingAlreadyShown()) {
                                Logger.internal("Trying to display an already shown landing message");
                            } else {
                                final BatchMessage message = intentParser.getLanding();
                                if (message != null) {
                                    intentParser.markLandingAsAlreadyShown();
                                    MessagingModuleProvider.get().displayMessage(context, message, false);
                                }
                            }
                        }
                    }

                    // Check if the notification authorization status changed from a previous value
                    NotificationAuthorizationStatus.checkForNotificationAuthorizationChange(context);

                    newIntent = null;
                }

                /*
                 * If we were already ready, do nothing
                 */
                if (state == State.READY && !shouldStart) {
                    return null;
                }

                final Context applicationContext = context.getApplicationContext();

                /*
                 * Init stuff, if we were stopped
                 */
                if (state == State.OFF) {
                    /*
                     * Set context
                     */
                    runtimeManager.setContext(applicationContext);

                    /*
                     * Warm up the local broadcast manager
                     */
                    LocalBroadcastManagerProvider.get(applicationContext);

                    // Tell the modules about the context
                    moduleMaster.batchContextBecameAvailable(applicationContext);

                    /*
                     * Check for update migration stuff
                     */
                    updateVersionManagement();

                    /*
                     * Check that we have mandatory permissions
                     */
                    if (!GenericHelper.checkPermission("android.permission.INTERNET", applicationContext)) {
                        Logger.error(
                            "Batch needs android.permission.INTERNET, please update your manifest, aborting start"
                        );
                        return null;
                    }

                    /*
                     * Get API Key
                     */
                    if (config.getApikey() == null) {
                        Logger.error("API key provided in Batch.start is null, aborting start");
                        return null;
                    }

                    /*
                     * Init device/install data
                     */
                    if (Batch.install == null) {
                        Batch.install = new Install(applicationContext);
                    }

                    /*
                     * Create new session id
                     */
                    Batch.sessionID = UUID.randomUUID().toString();

                    /*
                     * Init & register broadcast receiver
                     */
                    if (Batch.receiver == null) {
                        Batch.receiver = new InternalBroadcastReceiver();
                        IntentFilter filter = new IntentFilter();
                        filter.addAction(TaskExecutor.INTENT_WORK_FINISHED);
                        filter.addAction(OptOutModule.INTENT_OPTED_OUT);
                        LocalBroadcastManagerProvider.get(applicationContext).registerReceiver(receiver, filter);
                    }

                    // Check if we have a pending opt-in event
                    OptOutModuleProvider.get().trackOptinEventIfNeeded(context);
                }

                /*
                 * Register the messaging lifecycle listener and session manager if we can
                 */
                if (applicationContext instanceof Application) {
                    final android.app.Application app = (Application) applicationContext;
                    runtimeManager.registerSessionManagerIfNeeded(app, true);
                    runtimeManager.registerActivityListenerIfNeeded(app);
                } else {
                    // This should never happen, androidx relies on this
                    // https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-master-dev/lifecycle/lifecycle-process/src/main/java/androidx/lifecycle/ProcessLifecycleOwner.java
                    Logger.error("Context isn't an Application, could not register the session manager.");
                }

                /*
                 * Call modules
                 */
                moduleMaster.batchWillStart();

                /*
                 * Load event dispatcher from manifest meta-data
                 */
                EventDispatcherModuleProvider.get().loadDispatcherFromContext(context);

                try {
                    JSONObject startParams = new JSONObject();
                    if (userActivity) {
                        runtimeManager.updateLastUserStartDate();
                    } else {
                        startParams.put("silent", true);
                    }
                    startParams.putOpt(
                        "dispatchers",
                        EventDispatcherModuleProvider.get().getDispatchersAnalyticRepresentation()
                    );
                    if (startParams.length() == 0) {
                        startParams = null;
                    }
                    TrackerModuleProvider.get().track(InternalEvents.START, startParams);
                } catch (JSONException e) {
                    Logger.internal("Could not track _START", e);
                }

                if (intentParser != null && fromPush.get()) {
                    if (intentParser.isOpenAlreadyTracked()) {
                        Logger.internal("Already tracked open");
                    } else {
                        InternalPushData internalPushData = intentParser.getPushData();

                        JSONObject params;
                        if (internalPushData != null) {
                            params = new JSONObject(internalPushData.getExtraParameters());
                        } else {
                            params = new JSONObject();
                        }

                        TrackerModuleProvider.get().track(InternalEvents.OPEN_FROM_PUSH, params);
                        intentParser.markOpenAsAlreadyTracked();

                        try {
                            Bundle pushBundle = intentParser.getPushBundle();
                            if (pushBundle != null) {
                                BatchPushPayload pushPayload = BatchPushPayload.payloadFromReceiverExtras(pushBundle);
                                EventDispatcher.Payload payload = new PushEventPayload(pushPayload, true);
                                EventDispatcherModuleProvider
                                    .get()
                                    .dispatchEvent(EventDispatcher.Type.NOTIFICATION_OPEN, payload);
                            } else {
                                Logger.internal("Could not get the push bundle.");
                            }
                        } catch (BatchPushPayload.ParsingException | IllegalArgumentException e) {
                            Logger.internal("Could not dispatch NOTIFICATION_OPEN", e);
                        }
                        Logger.info("Activity was opened from a push");
                    }
                }

                /*
                 * Finally, ready to go !
                 */
                return State.READY;
            });

        // Call modules
        if (hasStarted) {
            moduleMaster.batchDidStart();
        }

        /*
         * Run the WS and actions if we have started (= if we were not already started)
         */
        if (hasStarted) {
            runtimeManager.runIf(
                State.READY,
                state -> {
                    /*
                     * Call start webservice
                     */
                    WebserviceLauncher.launchStartWebservice(
                        runtimeManager,
                        fromPush.get(),
                        pushId.toString(),
                        userActivity
                    );
                }
            );

            // Log if dev mode to warn the dev
            runtimeManager.readConfig(config -> {
                if (config.getApikey() != null && config.getApikey().toLowerCase(Locale.US).startsWith("dev")) {
                    Logger.warning(
                        "Batch (" + Parameters.SDK_VERSION + ") is running in dev mode (your API key is a dev one)"
                    );
                }
            });

            final String installID = Batch.User.getInstallationID();
            if (installID != null) {
                Logger.info("Installation ID: " + installID);
            }

            if (PushModule.isBackgroundRestricted(context)) {
                Logger.info("The app is running in restricted backgrounding mode");
            }
        }
    }

    /**
     * Perform onStop actions
     *
     * @param force should we force stop even if the activity is not finishing
     */
    private static void onStop(final Context _context, final boolean fromService, final boolean force) {
        /*
         * Stop action if we are ready, do nothing otherwise
         */
        boolean hasChanged = RuntimeManagerProvider
            .get()
            .changeStateIf(
                State.READY,
                (state, config) -> {
                    Logger.internal("onStop called with state " + state);

                    if (fromService) {
                        RuntimeManagerProvider.get().decrementServiceRefCount();
                    }

                    final Activity currentActivity = RuntimeManagerProvider.get().getActivity();

                    Activity activity = null;

                    if (_context instanceof Activity) {
                        activity = (Activity) _context;
                    }

                    /*
                     * If we are closing another activity that is not the one shown
                     */
                    if (activity != null) {
                        if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
                            Logger.internal("Closing an excluded activity");
                            return null;
                        }

                        if (activity != currentActivity) {
                            Logger.internal("Closing a sub activity");
                            return null;
                        }
                    }

                    /*
                     * If the activity is not finishing && we are not forcing (=onDestroy), we only save the date
                     */
                    if (!fromService && !force && currentActivity != null && !currentActivity.isFinishing()) {
                        Logger.internal("onStop called but activity is not finishing... saving date");
                        RuntimeManagerProvider.get().onStopWithoutFinishing();
                        return null;
                    }

                    if (!fromService) {
                        /*
                         * Release activity & listener to free memory
                         */
                        RuntimeManagerProvider.get().setActivity(null);
                    }

                    /*
                     * Is a service or activity still holding Batch?
                     */
                    if (
                        RuntimeManagerProvider.get().getActivity() != null ||
                        RuntimeManagerProvider.get().isRetainedByService()
                    ) {
                        Logger.internal("onStop called, but Batch is retained by a Service or Activity");
                        return null;
                    }

                    /*
                     * Call modules
                     */
                    moduleMaster.batchIsFinishing();

                    /*
                     * Set state to finishing
                     */
                    return State.FINISHING;
                }
            );

        if (hasChanged) {
            // Directly stop if we can
            if (!TaskExecutorProvider.get(RuntimeManagerProvider.get().getContext()).isBusy()) {
                Logger.internal("onStop, should stop directly : true");
                doStop();
            } else {
                Logger.internal("onStop, should stop directly : false");
            }
        }
    }

    /**
     * This method will be called everytime the {@link TaskExecutor} have finished all its tasks
     */
    private static void onWebserviceExecutorWorkFinished() {
        // Should we stop right now
        final AtomicBoolean shouldStop = new AtomicBoolean(false);

        // If we are in finishing state, we should stop now
        RuntimeManagerProvider.get().runIf(State.FINISHING, state -> shouldStop.set(true));

        Logger.internal("onWebserviceExecutorWorkFinished called, should stop " + shouldStop);

        // Stop if we can
        if (shouldStop.get()) {
            doStop();
        }
    }

    /**
     * Finish the Batch library runtime
     */
    private static void doStop() {
        /*
         * Avoid stopping if we are ready or already stopped
         */
        boolean hasChanged = RuntimeManagerProvider
            .get()
            .changeStateIf(
                State.FINISHING,
                (state, config) -> {
                    Logger.internal("doStop, called with state " + state);

                    // Call modules
                    moduleMaster.batchWillStop();
                    TrackerModuleProvider.get().track("_STOP");

                    // Free all object to clean memory
                    RuntimeManagerProvider.get().setContext(null);
                    receiver = null;

                    // Set state to OFF
                    return State.OFF;
                }
            );

        // Call modules
        if (hasChanged) {
            moduleMaster.batchDidStop();
            /*
             * Free all dependencies and re-set current module master
             */
            // Disable this: modules are not designed to be reset on stop
            // This causes issues with various modules, such as the PushModule
            // which will lose its configuration: Small Icon, notification interceptor, ...
            // See ch18557
            //DI.reset();
            //moduleMaster = BatchModuleMasterProvider.get();
        }
    }

    /**
     * Remove cached data such as installation id, device info, user, ...
     */
    private static void clearCachedInstallData() {
        Logger.internal(OptOutModule.TAG, "Clearing cached install data");
        install = null;
    }

    // --------------------------------------------->

    /**
     * Return the install object if available
     *
     * @return the install if available, null otherwise
     */
    @Nullable
    static Install getInstall() {
        return install;
    }

    // ----------------------------------------------->

    /**
     * Method call at every start to check if the lib has been updated
     */
    private static void updateVersionManagement() {
        try {
            String currentVersion = Parameters.SDK_VERSION;
            String savedVersion = ParametersProvider
                .get(RuntimeManagerProvider.get().getContext())
                .get(ParameterKeys.LIB_CURRENTVERSION_KEY);
            if (savedVersion == null) { // First launch case
                ParametersProvider
                    .get(RuntimeManagerProvider.get().getContext())
                    .set(ParameterKeys.LIB_CURRENTVERSION_KEY, currentVersion, true);
            } else if (!savedVersion.equals(currentVersion)) { // new version
                ParametersProvider
                    .get(RuntimeManagerProvider.get().getContext())
                    .set(ParameterKeys.LIB_CURRENTVERSION_KEY, currentVersion, true);
                ParametersProvider
                    .get(RuntimeManagerProvider.get().getContext())
                    .set(ParameterKeys.LIB_PREVIOUSVERSION_KEY, savedVersion, true);
            }
        } catch (Exception e) {
            Logger.internal("Error on updateVersionManagement", e);
        }
    }

    private static class InternalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();

            if (action == null) {
                return;
            }

            switch (action) {
                case TaskExecutor.INTENT_WORK_FINISHED:
                    Batch.onWebserviceExecutorWorkFinished();
                    break;
                case OptOutModule.INTENT_OPTED_OUT:
                    clearCachedInstallData();
                    break;
            }
        }
    }
}
