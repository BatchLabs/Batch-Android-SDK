package com.batch.android.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

public class RedirectNotificationSettingsAction implements UserActionRunnable {

    private static final String TAG = "RedirectSettingsAction";

    public static final String IDENTIFIER =
        ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "android_redirect_settings";
    public static final String IDENTIFIER_CEP = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "redirect_settings";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        if (context == null) {
            Logger.error(TAG, "Tried to perform a redirect settings action, but no context was available");
            return;
        }

        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        }
        context.startActivity(intent);
    }
}
