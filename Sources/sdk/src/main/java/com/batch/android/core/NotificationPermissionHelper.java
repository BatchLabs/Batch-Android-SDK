package com.batch.android.core;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import com.batch.android.BatchNotificationChannelsManagerPrivateHelper;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;

public class NotificationPermissionHelper {

    private static final String TAG = "NotificationPermission";
    private static final String BASE_TARGET_LOG_MESSAGE = "App is targeting Android ";

    public static final String PERMISSION_NOTIFICATION = "android.permission.POST_NOTIFICATIONS";

    // This will be removed once T hits stable
    // We want a bit of flexibility as we don't know what changes Google will make
    public boolean experimentalUseChannelCreationOnOldTargets = false;
    public boolean experimentalForceChannelCreation = false;

    public void requestPermission(@NonNull Context context) {
        // TODO: Test this method. Can't be done until Android T is supported in Robolectric
        Logger.internal(TAG, "Requesting notification permission.");

        // TODO: Do nothing on Android < 13.
        // As of writing, Android T betas are API level 32 (same as 12L), so allow 12L to pass this check.
        // We could implement a codename check but as calling this code on older Android does nothing,
        // it's not worth the hassle.
        // Note: if we want to implement a proper check, we could look at Build.VERSION.CODENAME == "Tiramisu"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
            return;
        }

        if (
            context.getSystemService(NotificationManager.class).areNotificationsEnabled() &&
            GenericHelper.checkPermission(PERMISSION_NOTIFICATION, context)
        ) {
            Logger.internal(TAG, "Notifications are already enabled, not requesting permission.");
            return;
        }

        if (GenericHelper.targets12LOrOlder(context)) {
            Logger.internal(TAG, BASE_TARGET_LOG_MESSAGE + "12L or lower.");
            // Android documentation and Chromium sources
            // say that you can't request the permission if you don't target T, but current testing
            // shows otherwise: as long as the notification channel isn't created, an app that doesn't
            // target android 13 can request the permission. Creating the channels shows the permission
            // popup. So, by default
            //
            // Note: we allow the caller to bypass this and request the way docs say we should, with
            // the caveat of it not working if the channel ID is overridden.
            if (experimentalUseChannelCreationOnOldTargets) {
                Logger.internal(TAG, "Requesting permission by creating channel.");
                requestPermissionFromOlderSDK(context);
                return;
            }
        } else {
            Logger.internal(TAG, BASE_TARGET_LOG_MESSAGE + "13.");
        }

        // Try to get the current activity
        // We may already have one: in that case, calling getBaseContext()
        // would make us lose it, don't call it blindly.
        // On the other hand, androidx's ContextThemeWrapper is not a superclass of Activity
        // so we need to call getBaseContext and hope to get it.
        // Otherwise, give up, we might be able to find a context by looping but lets not go down
        // that road just yet.
        if (!(context instanceof Activity)) {
            if (context instanceof androidx.appcompat.view.ContextThemeWrapper) {
                context = ((androidx.appcompat.view.ContextThemeWrapper) context).getBaseContext();
            } else if (context instanceof android.view.ContextThemeWrapper) {
                context = ((android.view.ContextThemeWrapper) context).getBaseContext();
            }
        }

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            activity.runOnUiThread(() -> {
                activity.requestPermissions(new String[] { PERMISSION_NOTIFICATION }, 0);
            });
        } else {
            // Should we have a metric here?
            Logger.internal(TAG, "Cannot request notification permission: no suitable context.");
        }
    }

    // Request the permission the google way: by creating the notification channel.
    // Note: if the user has a channel id override, this will not work unless forced, which is also
    // a controllable experiment.
    public void requestPermissionFromOlderSDK(@NonNull Context context) {
        BatchNotificationChannelsManagerPrivateHelper.registerBatchChannelIfNeeded(
            BatchNotificationChannelsManagerProvider.get(),
            context,
            experimentalForceChannelCreation
        );
    }
}
