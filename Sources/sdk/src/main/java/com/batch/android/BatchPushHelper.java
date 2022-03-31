package com.batch.android;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

/**
 * Set of helper methods shared between {@link BatchPushNotificationPresenter} and methods exposed to
 * devs via the push module
 *
 * @hide
 */
public class BatchPushHelper {

    /**
     * This method checks:
     *  - If this notification is for the current Install ID (if available)
     *
     * It used to do other stuff as in deduplicate notifications but that's been reworked.
     */
    public static synchronized boolean canDisplayPush(Context context, InternalPushData batchData) {
        /*
         * Check install ID
         */
        String installId = batchData.getInstallId();
        if (installId != null && !installIDMatchesCurrent(context, installId)) {
            Logger.warning(
                PushModule.TAG,
                "Received notification[" +
                batchData.getPushId() +
                "] for another install id[" +
                installId +
                "], aborting"
            );
            return false;
        }

        return true;
    }

    /**
     * Convert a Firebase RemoteMessage to a bundle, for compatibility with all the methods that used
     * to deal with a BroadcastReceiver started by GCM
     */
    @Nullable
    public static Bundle firebaseMessageToReceiverBundle(@Nullable RemoteMessage message) {
        if (message == null) {
            return null;
        }

        Map<String, String> data = message.getData();
        if (data == null || data.size() == 0) {
            return null;
        }

        Bundle retVal = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            retVal.putString(entry.getKey(), entry.getValue());
        }
        return retVal;
    }

    /**
     * Is the given install id mine
     *
     * @param installId
     * @return
     */
    private static boolean installIDMatchesCurrent(Context context, String installId) {
        String currentInstallId = new Install(context).getInstallID();
        return installId.equals(currentInstallId);
    }
}
