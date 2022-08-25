package com.batch.android.core;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.batch.android.BatchNotificationChannelsManagerPrivateHelper;
import com.batch.android.BatchPermissionActivity;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;

public class NotificationPermissionHelper extends BroadcastReceiver {

    private static final String TAG = "NotificationPermission";
    private static final String BASE_TARGET_LOG_MESSAGE = "App is targeting Android ";

    public static final String PERMISSION_NOTIFICATION = "android.permission.POST_NOTIFICATIONS";

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean isNotificationPermissionGranted(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        return (
            notificationManager != null &&
            notificationManager.areNotificationsEnabled() &&
            GenericHelper.checkPermission(PERMISSION_NOTIFICATION, context)
        );
    }

    /**
     * Request the Android 13 notification runtime permission
     * TODO: Test this method. Can't be done until Android T is supported in Robolectric
     * @param context context
     * @param handleTargetLowerThan13 whether we should trigger the permission request for an app targeting lower than Android 13.
     */
    public void requestPermission(
        @NonNull Context context,
        boolean handleTargetLowerThan13,
        @Nullable BroadcastReceiver receiver
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Not running on Android 13, no need for permission.
            return;
        }

        if (isNotificationPermissionGranted(context)) {
            Logger.internal(TAG, "Notifications are already enabled, not requesting permission.");
            return;
        }

        if (GenericHelper.isTargetLowerThan13(context)) {
            // App target sdk is lower than 13,
            // So we request the permission by creating the notification channel.
            Logger.internal(TAG, BASE_TARGET_LOG_MESSAGE + "12L or lower.");
            if (handleTargetLowerThan13) {
                requestPermissionFromOlderSDK(context);
            } else {
                Logger.internal(
                    TAG,
                    "Cannot request the notification permission in the context. " +
                    "To do that, please update your target sdk to 33."
                );
            }
            return;
        }
        // App is targeting Android 13 or higher
        Logger.internal(TAG, BASE_TARGET_LOG_MESSAGE + "13.");

        if (receiver == null) {
            receiver = this;
        }
        // Registering a broadcast receiver to handle the permission result.
        IntentFilter filter = new IntentFilter(BatchPermissionActivity.ACTION_PERMISSION_RESULT);
        LocalBroadcastManagerProvider.get(context).registerReceiver(receiver, filter);

        // Launching an activity to request the permission.
        Intent intent = new Intent(context, BatchPermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(BatchPermissionActivity.EXTRA_PERMISSION, PERMISSION_NOTIFICATION);
        context.startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean isPermissionAlreadyAskedFromOlderSDK(@NonNull Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return manager.getNotificationChannels().size() > 0;
    }

    /**
     * Request the permission the google way: by creating the notification channel.
     * If the App is running in background at this moment (eg: receiving a push for the first time),
     * the user will be prompted when app goes back in foreground.
     * Note: if the user has a channel id override, this will not work.
     * @param context context
     */
    public void requestPermissionFromOlderSDK(@NonNull Context context) {
        BatchNotificationChannelsManagerPrivateHelper.registerBatchChannelIfNeeded(
            BatchNotificationChannelsManagerProvider.get(),
            context
        );
    }

    /**
     * Broadcast receiver to handle the permission result
     * @param context context
     * @param intent broadcasted
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String permission = intent.getStringExtra(BatchPermissionActivity.EXTRA_PERMISSION);
        // Ensure receiver is called for the right permission
        if (PERMISSION_NOTIFICATION.equals(permission)) {
            // Permission result is accessible from extra's intent at BatchPermissionActivity.EXTRA_RESULT
            NotificationAuthorizationStatus.checkForNotificationAuthorizationChange(context);
            // Unregister receiver as it is registered when permission is requested
            LocalBroadcastManagerProvider.get(context).unregisterReceiver(this);
        }
    }
}
