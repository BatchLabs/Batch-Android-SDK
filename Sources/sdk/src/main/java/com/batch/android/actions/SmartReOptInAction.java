package com.batch.android.actions;

import static com.batch.android.core.NotificationPermissionHelper.PERMISSION_NOTIFICATION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.batch.android.BatchPermissionActivity;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.NotificationAuthorizationStatus;
import com.batch.android.core.NotificationPermissionHelper;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

public class SmartReOptInAction extends BroadcastReceiver implements UserActionRunnable {

    private static final String TAG = "SmartReOptInAction";

    public static final String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "android_smart_reoptin";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        if (context == null) {
            Logger.error(TAG, "Tried to perform a smart reoptin action, but no context was available");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Not running on Android 13, no need for permission.
            return;
        }
        final NotificationPermissionHelper notificationPermissionHelper = new NotificationPermissionHelper();
        if (notificationPermissionHelper.isNotificationPermissionGranted(context)) {
            Logger.internal(TAG, "Notification permission is already granted, aborting.");
            return;
        }

        // App is not targeting Android 13
        // Checking if a notification channel has already been created,
        // If yes redirect to settings else request permission by creating batch channel
        if (GenericHelper.isTargetLowerThan13(context)) {
            if (notificationPermissionHelper.isPermissionAlreadyAskedFromOlderSDK(context)) {
                redirectToNotificationSettings(context);
            } else {
                notificationPermissionHelper.requestPermissionFromOlderSDK(context);
            }
            return;
        }

        notificationPermissionHelper.requestPermission(context, false, this);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onReceive(Context context, Intent intent) {
        String permission = intent.getStringExtra(BatchPermissionActivity.EXTRA_PERMISSION);
        // Ensure receiver is called for the right permission
        if (PERMISSION_NOTIFICATION.equals(permission)) {
            boolean granted = intent.getBooleanExtra(BatchPermissionActivity.EXTRA_RESULT, false);
            boolean shouldRedirectSettings = intent.getBooleanExtra(
                BatchPermissionActivity.EXTRA_REDIRECT_SETTINGS,
                false
            );
            NotificationAuthorizationStatus.checkForNotificationAuthorizationChange(context);

            // Unregister receiver as it is registered when permission is requested
            LocalBroadcastManagerProvider.get(context).unregisterReceiver(this);

            // Redirect user to the notification settings if needed
            if (!granted && shouldRedirectSettings) {
                redirectToNotificationSettings(context);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void redirectToNotificationSettings(Context context) {
        Intent settingsIntent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        settingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        context.startActivity(settingsIntent);
    }
}
