package com.batch.android.core;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationManagerCompat;
import com.batch.android.BatchNotificationChannelsManager;
import com.batch.android.BatchNotificationChannelsManagerPrivateHelper;
import com.batch.android.PushNotificationType;
import com.batch.android.WebserviceLauncher;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.push.Registration;
import com.batch.android.runtime.RuntimeManager;
import java.util.EnumSet;

public class NotificationAuthorizationStatus {

    public static final String TAG = "Notification Authorization";

    /**
     * Last notification authorization status
     */
    private static Boolean lastNotificationAuthorizationStatus = null;

    /**
     * Check whether the notification authorization status change
     * @param context context
     */
    public static void checkForNotificationAuthorizationChange(@NonNull Context context) {
        // Get the current notification authorization
        boolean hasNotificationAuthorization = canAppShowNotifications(
            context,
            BatchNotificationChannelsManagerProvider.get()
        );

        // Check if the notification settings changed between the last time we sent them to the server and now
        boolean shouldTrack = shouldTrackNotificationStatusChangeEvent(context, hasNotificationAuthorization);
        if (shouldTrack) {
            JSONObject params = new JSONObject();
            try {
                // Send the event to the server, and store it so that future calls of this method don't send the same
                params.put("has_notification_authorization", hasNotificationAuthorization);
                TrackerModuleProvider.get().track(InternalEvents.NOTIFICATION_STATUS_CHANGE, params);
                ParametersProvider
                    .get(context)
                    .set(
                        ParameterKeys.PUSH_NOTIF_LAST_AUTH_STATUS_SENT,
                        String.valueOf(hasNotificationAuthorization),
                        true
                    );
            } catch (JSONException e) {
                Logger.internal("Cannot track event NOTIFICATION_STATUS_CHANGE.");
            }
        }

        // First time, store it, as the start will send it
        // If it changes, force a Push query
        if (lastNotificationAuthorizationStatus == null) {
            lastNotificationAuthorizationStatus = hasNotificationAuthorization;
            return;
        }

        if (lastNotificationAuthorizationStatus != hasNotificationAuthorization) {
            lastNotificationAuthorizationStatus = hasNotificationAuthorization;
            Logger.internal(
                "Notification Authorization changed (is now " + (hasNotificationAuthorization ? "true" : "false") + ")"
            );

            RuntimeManager runtimeManager = RuntimeManagerProvider.get();
            boolean sdkReady = runtimeManager.runIfReady(() -> {
                // Trigger a push token webservice
                Registration registration = PushModuleProvider.get().getRegistration(context);
                if (registration == null) {
                    Logger.internal(
                        "Notif. Authorization changed but no registration is available. Not sending update to the server."
                    );
                    return;
                }
                WebserviceLauncher.launchPushWebservice(runtimeManager, registration);
            });

            if (!sdkReady) {
                Logger.internal("Notif. Authorization changed but SDK isn't ready. Not sending update to the server.");
            }
        }
    }

    /**
     * Check if the notification settings changed between the last time we sent them to the server and now
     * @param context to get the last event sent
     * @param hasNotificationAuthorization current notification authorization
     */
    @VisibleForTesting
    static boolean shouldTrackNotificationStatusChangeEvent(
        @NonNull Context context,
        boolean hasNotificationAuthorization
    ) {
        String lastNotificationAuthorizationSend = ParametersProvider
            .get(context)
            .get(ParameterKeys.PUSH_NOTIF_LAST_AUTH_STATUS_SENT);
        boolean hasChanged;
        if (lastNotificationAuthorizationSend == null) {
            // First time we get the notification authorization
            hasChanged = true;
        } else {
            // Check if the notification authorization has changed since the last time we sent it.
            hasChanged = Boolean.parseBoolean(lastNotificationAuthorizationSend) != hasNotificationAuthorization;
        }
        return hasChanged;
    }

    // Returns whether the app can potentially show a notification using batch
    // Note: false negatives are possible if the developer uses a notification
    // interceptor to change the channel
    public static boolean canAppShowNotifications(
        @NonNull Context context,
        @NonNull BatchNotificationChannelsManager channelsManager
    ) {
        NotificationManager notificationManagerService = (NotificationManager) context.getSystemService(
            Context.NOTIFICATION_SERVICE
        );
        if (notificationManagerService == null) {
            return false;
        }

        return (
            areBatchNotificationsEnabled(context) &&
            areAppNotificationsEnabled(context, notificationManagerService) &&
            isDefaultChannelEnabled(notificationManagerService, channelsManager)
        );
    }

    // Returns whether the app can potentially show a notification on a given channel
    public static boolean canAppShowNotificationsForChannel(@NonNull Context context, @Nullable String channelID) {
        NotificationManager notificationManagerService = (NotificationManager) context.getSystemService(
            Context.NOTIFICATION_SERVICE
        );
        if (notificationManagerService == null) {
            return false;
        }

        boolean canChannelShowNotification = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channelID == null) {
                return false;
            }
            canChannelShowNotification = canChannelShowNotifications(notificationManagerService, channelID, false);
        }

        return (
            areBatchNotificationsEnabled(context) &&
            areAppNotificationsEnabled(context, notificationManagerService) &&
            canChannelShowNotification
        );
    }

    @VisibleForTesting
    static boolean areAppNotificationsEnabled(
        @NonNull Context context,
        @NonNull NotificationManager notificationManagerService
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return notificationManagerService.areNotificationsEnabled();
        } else if (Build.VERSION.SDK_INT >= 19) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }

        return true;
    }

    @VisibleForTesting
    static boolean areBatchNotificationsEnabled(@NonNull Context context) {
        try {
            String param = ParametersProvider.get(context).get(ParameterKeys.PUSH_NOTIF_TYPE);
            if (param == null) {
                return true;
            }

            EnumSet<PushNotificationType> types = PushNotificationType.fromValue(Integer.parseInt(param));

            // Notifications are disabled if types does not contain alert
            // They are often disabled by setting them to "NONE", but in that case
            // they also don't contain ALERT, so we're good.
            // Having both ALERT and NONE isn't supported.
            return types.contains(PushNotificationType.ALERT);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while getting Batch notification type", e);
        }

        return true;
    }

    @VisibleForTesting
    static boolean isDefaultChannelEnabled(
        @NonNull NotificationManager notificationManagerService,
        @NonNull BatchNotificationChannelsManager channelsManager
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return canChannelShowNotifications(
                notificationManagerService,
                BatchNotificationChannelsManagerPrivateHelper.getChannelId(channelsManager),
                true
            );
        }
        return true;
    }

    // Checks whether a channel can show notifications
    // Also checks for its enclosing group
    @RequiresApi(api = Build.VERSION_CODES.O)
    @VisibleForTesting
    static boolean canChannelShowNotifications(
        @NonNull NotificationManager notificationManagerService,
        @NonNull String channelId,
        boolean autoCreatedChannel
    ) {
        NotificationChannel channel = notificationManagerService.getNotificationChannel(channelId);
        if (channel == null && autoCreatedChannel) {
            // As Batch's channel is lazily created, it not existing will result in a notification
            // being shown
            return true;
        } else if (channel == null) {
            // The channel won't be created by Batch, so return false if it doesn't exist
            return false;
        }

        // See if the channel has a group, as complete groups can be disabled
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            String groupId = channel.getGroup();
            if (!TextUtils.isEmpty(groupId)) {
                NotificationChannelGroup group = notificationManagerService.getNotificationChannelGroup(groupId);
                if (group != null && group.isBlocked()) {
                    return false;
                }
            }
        }

        return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
}
