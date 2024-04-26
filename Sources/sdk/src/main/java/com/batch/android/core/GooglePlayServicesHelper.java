package com.batch.android.core;

import android.content.Context;
import com.batch.android.module.PushModule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper to get GooglePlayServices data with introspection
 *
 */
public final class GooglePlayServicesHelper {

    /**
     * The first version of the Google Play Services the contain Firebase Cloud Messaging methods
     */
    private static final int FCM_ID_VERSION = 9000000;

    /**
     * Is the availability of the lib already checked (to avoid redo too much introspection)
     */
    private static boolean versionChecked = false;
    /**
     * Cache of the version of the lib that must be use if {@link #versionChecked} is true
     */
    private static Integer libVersionCached;

    // -------------------------------------------------->

    /**
     * Retrieve the google play services' isGooglePlayServicesAvailable result as a string
     *
     * @param availability The Google Play Service availability code
     * @return The availability status
     */
    public static String getGooglePlayServicesAvailabilityString(Integer availability) {
        if (availability == null) {
            return "GOOGLE_PLAY_LIBRARY_MISSING";
        }

        switch (availability) {
            case 0:
                return "SUCCESS";
            case 1:
                return "SERVICE_MISSING";
            case 3:
                return "SERVICE_DISABLED";
            case 19:
                return "SERVICE_MISSING_PERMISSION";
            case 18:
                return "SERVICE_UPDATING";
            case 2:
                return "SERVICE_VERSION_UPDATE_REQUIRED";
            default:
                return "UNKNOWN - Code: " + availability;
        }
    }

    public static Integer getGooglePlayServicesAvailabilityInteger(Context context) {
        // Don't cache the availability, it's dynamic.
        try {
            // Get the class, throws ClassNotFoundException if not available
            Class<?> clazz = Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            /*
             * Check if we are in a Google Play environment (not true for Amazon devices for exemple)
             */
            Method method = clazz.getMethod("isGooglePlayServicesAvailable", Context.class);
            return (Integer) method.invoke(null, context);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Error while retrieving Google Play Services lib availability", e);
            return null;
        }
    }

    /**
     * Retrieve the google play services version if the lib regardless if it is available
     * on runtime or not
     *
     * @return version of the lib if available, null if the lib is not here
     */
    public static Integer getGooglePlayServicesLibVersion() {
        /*
         * Check cache to avoid redo multiple time
         */
        if (versionChecked) {
            return libVersionCached;
        }

        try {
            // Get the class, throws ClassNotFoundException if not available
            Class<?> clazz = Class.forName("com.google.android.gms.common.GoogleApiAvailability");
            // Retrieve version
            Field f = clazz.getField("GOOGLE_PLAY_SERVICES_VERSION_CODE");
            libVersionCached = f.getInt(null);
            return libVersionCached;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Error while retrieving Google Play Services lib version", e);
            return null;
        } finally {
            versionChecked = true;
        }
    }

    /**
     * Check if FCM is available
     *
     * @param context Android's context
     * @return Integer errorID. The error's ID. 0 if the library is available both at integration and runtime. Can be null.
     */
    public static Integer isFCMAvailable(Context context) {
        Integer libVersion = getGooglePlayServicesLibVersion();
        if (libVersion == null || libVersion < FCM_ID_VERSION) {
            return null;
        }
        return getGooglePlayServicesAvailabilityInteger(context);
    }
}
