package com.batch.android.actions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.core.NotificationPermissionHelper;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

public class NotificationPermissionActionRunnable implements UserActionRunnable {

    private static final String TAG = "NotificationPermissionAction";
    public static final String IDENTIFIER =
        ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "android_request_notifications";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        if (context == null) {
            Logger.error(TAG, "Tried to perform a notif. permission request action, but no context was available");
            return;
        }
        final NotificationPermissionHelper notificationPermissionHelper = new NotificationPermissionHelper();
        notificationPermissionHelper.requestPermission(context, true, null);
    }
}
