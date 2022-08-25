package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * @hide
 */
public class BatchNotificationChannelsManagerPrivateHelper {

    @NonNull
    public static String getChannelId(BatchNotificationChannelsManager manager) {
        return manager.getChannelId(null);
    }

    public static void registerBatchChannelIfNeeded(BatchNotificationChannelsManager manager, Context context) {
        manager.registerBatchChannelIfNeeded(context);
    }
}
