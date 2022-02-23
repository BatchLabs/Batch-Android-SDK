package com.batch.android.core;

import android.content.Context;
import com.batch.android.module.PushModule;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper to get GooglePlayServices data with introspection
 *
 */
public final class GooglePlayServicesHelper {

    /**
     * The version of the Google Play Services lib that contains advertising ID
     */
    private static final int ADVERTISING_ID_VERSION = 4030500;

    /**
     * The version of the Google Play Services lib that contains push
     */
    private static final int PUSH_ID_VERSION = 4030500;

    /**
     * The version of the Google Play Services that contain the Instance ID methods
     */
    private static final int INSTANCE_ID_VERSION = 7571000;

    /**
     * The first version of the Google Play Services the contain Firebase Cloud Messaging methods
     */
    private static final int FCM_ID_VERSION = 9000000;

    // ------------------------------------------------->

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
     * retrieve the google play services' isGooglePlayServicesAvailable result as a string
     *
     * @param availability
     * @return version of the lib if available, null if the lib is unavailable
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
             * Check if we are in a Google Play environement (not true for Amazon devices for exemple)
             */
            Method method = clazz.getMethod("isGooglePlayServicesAvailable", Context.class);
            return (Integer) method.invoke(null, context);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Error while retreiving Google Play Services lib availability", e);
            return null;
        }
    }

    /**
     * retrieve the google play services version if the lib regardless if it is available
     * on rutime or not
     *
     * @param context
     * @return version of the lib if available, null if the lib is not here
     */
    public static Integer getGooglePlayServicesLibVersion(Context context) {
        /*
         * Check cache to avoid redo multiple time
         */
        if (versionChecked) {
            return libVersionCached;
        }

        try {
            // Get the class, throws ClassNotFoundException if not available
            Class<?> clazz = Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");

            /*
             * retrieve the version
             */
            Field f = clazz.getField("GOOGLE_PLAY_SERVICES_VERSION_CODE");
            libVersionCached = f.getInt(null);
            return libVersionCached;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Error while retreiving Google Play Services lib version", e);
            return null;
        } finally {
            versionChecked = true;
        }
    }

    // ------------------------------------------------>

    /**
     * Check if the advertising ID is available by checking the version of the lib
     *
     * @param context
     * @return
     */
    public static boolean isAdvertisingIDAvailable(Context context) {
        Integer libVersion = getGooglePlayServicesLibVersion(context);
        if (libVersion == null) {
            return false;
        }

        Integer availability = getGooglePlayServicesAvailabilityInteger(context);
        if (availability == null || availability != 0) {
            return false;
        }

        return libVersion >= ADVERTISING_ID_VERSION;
    }

    // ------------------------------------------------>

    /**
     * Check if GCM is available
     *
     * @param context
     * @return Integer errorID. The error's ID. 0 if the library is available both at integration and runtime. Can be null.
     */
    public static Integer isPushAvailable(Context context) {
        Integer libVersion = getGooglePlayServicesLibVersion(context);
        if (
            libVersion == null || libVersion < PUSH_ID_VERSION || !ReflectionHelper.isGMSGoogleCloudMessagingPresent()
        ) {
            return null;
        }

        return getGooglePlayServicesAvailabilityInteger(context);
    }

    /**
     * Get the push token from GCM. Be careful, this method is synchronous and can take a long time.
     *
     * @param context Application context
     * @return Push token or null if unavailable
     */
    public static String getPushToken(Context context, String senderID) {
        try {
            final Context appContext = context.getApplicationContext();

            /*
             * Retrieve infos
             */
            Class<?> clazz = Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
            Method singletonMethod = clazz.getMethod("getInstance", Context.class);
            Method registerMethod = clazz.getMethod("register", String[].class);
            Object singleton = singletonMethod.invoke(null, appContext);

            String[] senderIds = { senderID };
            Object[] args = { senderIds };
            return (String) registerMethod.invoke(singleton, args);
        } catch (Exception e) {
            // Check for the INVALID_SENDER exception to inform the developer
            if (isInvalidSenderException(e)) {
                Logger.error(
                    PushModule.TAG,
                    "GCM sender id is invalid. Please check your GCM configuration. More info: " + Parameters.DOMAIN_URL
                );
                return null;
            } else {
                if (e.getCause() != null) {
                    Logger.error(
                        PushModule.TAG,
                        "Error while requesting push token to GCM : " + e.getCause().getLocalizedMessage()
                    );
                }
            }

            Logger.error(PushModule.TAG, "Error while registering for push", e);
            return null;
        }
    }

    // ------------------------------------------------>

    /**
     * Check if InstanceID based GCM is available
     *
     * @param context
     * @return Integer errorID. The error's ID. 0 if the library is available both at integration and runtime. Can be null.
     */
    public static Integer isInstanceIdPushAvailable(Context context) {
        Integer libVersion = getGooglePlayServicesLibVersion(context);
        if (libVersion == null || libVersion < INSTANCE_ID_VERSION || !ReflectionHelper.isGMSInstanceIDPresent()) {
            return null;
        }

        return getGooglePlayServicesAvailabilityInteger(context);
    }

    /**
     * Get the instance token (not the instance id itself), usable with GCM
     */
    public static String getInstancePushToken(Context context, String senderID) {
        try {
            final Context appContext = context.getApplicationContext();

            /*
             * Retrieve infos
             */
            Class<?> clazz = Class.forName("com.google.android.gms.iid.InstanceID");
            Method singletonMethod = clazz.getMethod("getInstance", Context.class);
            Method registerMethod = clazz.getMethod("getToken", String.class, String.class);
            Object singleton = singletonMethod.invoke(null, appContext);

            Object[] args = { senderID, "GCM" };
            return (String) registerMethod.invoke(singleton, args);
        } catch (Exception e) {
            Logger.internal(PushModule.TAG, "Error while registering for instance id push", e);

            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                String publicErrorReason;
                switch (cause.getMessage()) {
                    case "INVALID_SENDER":
                        publicErrorReason = "Sender ID is invalid";
                        break;
                    default:
                        publicErrorReason = "Unknown error";
                        break;
                }

                Logger.error(PushModule.TAG, "Could not get GCM Instance ID: " + publicErrorReason);
            }
            return null;
        }
    }

    /**
     * Check if the exception is an INVALID_SENDER one
     *
     * @param e
     * @return
     */
    private static boolean isInvalidSenderException(Exception e) {
        return (
            e.getCause() != null &&
            (e.getCause() instanceof IOException) &&
            "INVALID_SENDER".equals(e.getCause().getMessage())
        );
    }

    // ------------------------------------------------>

    /**
     * Check if FCM is available
     *
     * @param context
     * @return Integer errorID. The error's ID. 0 if the library is available both at integration and runtime. Can be null.
     */
    public static Integer isFCMAvailable(Context context) {
        Integer libVersion = getGooglePlayServicesLibVersion(context);
        if (libVersion == null || libVersion < FCM_ID_VERSION) {
            return null;
        }

        return getGooglePlayServicesAvailabilityInteger(context);
    }
}
