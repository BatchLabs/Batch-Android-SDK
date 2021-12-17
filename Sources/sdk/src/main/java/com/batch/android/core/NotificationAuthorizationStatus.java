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
import com.batch.android.di.providers.ParametersProvider;
import java.util.EnumSet;

public class NotificationAuthorizationStatus {

  public static final String TAG = "Notification Authorization";

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
  public static boolean canAppShowNotificationsForChannel(
    @NonNull Context context,
    @Nullable String channelID
  ) {
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
      canChannelShowNotification =
        canChannelShowNotifications(
          notificationManagerService,
          channelID,
          false
        );
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
      String param = ParametersProvider
        .get(context)
        .get(ParameterKeys.PUSH_NOTIF_TYPE);
      if (param == null) {
        return true;
      }

      EnumSet<PushNotificationType> types = PushNotificationType.fromValue(
        Integer.parseInt(param)
      );

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
        BatchNotificationChannelsManagerPrivateHelper.getChannelId(
          channelsManager
        ),
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
    NotificationChannel channel = notificationManagerService.getNotificationChannel(
      channelId
    );
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
        NotificationChannelGroup group = notificationManagerService.getNotificationChannelGroup(
          groupId
        );
        if (group != null && group.isBlocked()) {
          return false;
        }
      }
    }

    return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
  }
}
