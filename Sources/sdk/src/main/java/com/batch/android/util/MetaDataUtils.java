package com.batch.android.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * Utility class to easily access the manifest meta-data info
 */
public class MetaDataUtils {

    public static final String MANIFEST_SENDER_ID_KEY = "batch_push_fcm_sender_id_override";

    public static final String MANIFEST_FORCE_FCM_IID_KEY = "batch_push_force_fcm_iid_provider";

    /**
     * Get the bundle meta data info from the AndroidManifest
     *
     * @param context Application context
     * @return the bundle meta-data
     * @throws PackageManager.NameNotFoundException exception
     */
    public static Bundle getAppMetaData(Context context) throws PackageManager.NameNotFoundException {
        return context
            .getPackageManager()
            .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
            .metaData;
    }

    /**
     * Get a boolean meta-data info value from the manifest
     *
     * @param context Application context
     * @param key     key of the meta-data info
     * @return the meta-data value in the manifest or false if key do not exist
     */
    public static boolean getBooleanMetaData(Context context, String key) {
        try {
            Bundle metaData = getAppMetaData(context);
            if (metaData != null) {
                return metaData.getBoolean(key, false);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get an integer meta-data info value from the manifest
     *
     * @param context Application context
     * @param key     key of the meta-data info
     * @return the meta-data value in the manifest or -1 if key do not exist
     */
    public static int getIntMetaData(Context context, String key) {
        try {
            Bundle metaData = getAppMetaData(context);
            if (metaData != null) {
                return metaData.getInt(key, -1);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
