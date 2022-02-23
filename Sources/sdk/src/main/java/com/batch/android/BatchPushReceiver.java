package com.batch.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.compat.WakefulBroadcastReceiver;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;

/**
 * Batch's legacy implementation of GCM's Push BroadcastReceiver
 *
 */
@PublicSDK
public class BatchPushReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.internal(
            PushModule.TAG,
            "BatchPushReceiver called. Disabling the broadcast receiver from the PackageManager."
        );
        // Ask the package manager to disable ourselves
        try {
            final ComponentName selfComponent = new ComponentName(context, BatchPushReceiver.class);
            context
                .getPackageManager()
                .setComponentEnabledSetting(
                    selfComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                );
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not disable BatchPushReceiver.");
            Logger.internal(PushModule.TAG, "Could not disable BatchPushReceiver.", e);
        }
    }
}
