package com.batch.android.core.systemparameters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.core.Webservice;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Helper to retrieve Android system configuration.
 */
public final class SystemParameterHelper {

    private static final String TAG = "SystemParameterHelper";

    /**
     * Return the bundle name of the application.
     *
     * @param applicationContext The application context.
     * @return The application bundle name.
     */
    public static String getBundleName(Context applicationContext) {
        return applicationContext.getPackageName();
    }

    /**
     * Get the current device timezone.
     *
     * @return timezone or null on failure.
     */
    public static String getDeviceTimezone() {
        try {
            return TimeZone.getDefault().getID();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the current device language
     *
     * @return language
     */
    public static String getDeviceLanguage() {
        return Locale.getDefault().toLanguageTag();
    }

    /**
     * Get the current device country
     *
     * @return country
     */
    public static String getDeviceCountry() {
        return Locale.getDefault().getCountry();
    }

    /**
     * Return the device date formatted with RFC 3339 format
     *
     * @return The current date
     */
    public static String getDeviceDate() {
        return Webservice.formatDate(new Date());
    }

    /**
     * Get very first installation date.
     *
     * @param applicationContext The application context.
     * @return Installation date or null on failure.
     */
    public static Long getFirstInstallDate(Context applicationContext) {
        try {
            PackageManager packageManager = applicationContext.getPackageManager();
            PackageInfo information = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);

            return information.firstInstallTime;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Get last update date.
     *
     * @param applicationContext The application context.
     * @return Last update date or null on failure.
     */
    public static Long getLastUpdateDate(Context applicationContext) {
        try {
            PackageManager packageManager = applicationContext.getPackageManager();
            PackageInfo information = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);

            return information.lastUpdateTime;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Return the Brand name of the device
     *
     * @return Brand name if found, null otherwise
     */
    public static String getDeviceBrand() {
        try {
            return Build.BRAND;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Return the device model of the phone.
     *
     * @return The device model if found, null otherwise.
     */
    public static String getDeviceModel() {
        try {
            return Build.MODEL;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Return the current version of the application.
     *
     * @param applicationContext The application context.
     * @return AppVersion if found, null otherwise
     */
    public static String getAppVersion(Context applicationContext) {
        try {
            PackageInfo info = applicationContext
                .getPackageManager()
                .getPackageInfo(getBundleName(applicationContext), 0);
            return info.versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Return the current version code of the application.
     *
     * @param applicationContext The application context.
     * @return AppVersion code if found, null otherwise
     */
    public static Integer getAppVersionCode(Context applicationContext) {
        try {
            PackageManager packageManager = applicationContext.getApplicationContext().getPackageManager();
            PackageInfo information = packageManager.getPackageInfo(
                applicationContext.getApplicationContext().getPackageName(),
                0
            );

            return information.versionCode;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Return the version of Android of the phone.
     *
     * @return Version.
     */
    public static String getOSVersion() {
        return String.format("Android %s", Build.VERSION.RELEASE);
    }

    /**
     * Get the version of the Bridge currently using Batch if available
     *
     * @return Bridge version string
     */
    public static String getBridgeVersion() {
        return System.getProperty(Parameters.BRIDGE_VERSION_ENVIRONEMENT_VAR, "");
    }

    /**
     * Get the version of the Plugin currently using Batch if available
     *
     * @return Plugin version string
     */
    public static String getPluginVersion() {
        return System.getProperty(Parameters.PLUGIN_VERSION_ENVIRONEMENT_VAR, "");
    }

    /**
     * Get the android sdk api level
     * @return The android sdk api level
     */
    public static String getOSSdkLevel() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    /**
     * Get the batch sdk api level
     * @return The batch sdk api level
     */
    public static String getSdkApiLevel() {
        return String.valueOf(Parameters.API_LEVEL);
    }

    /**
     * Get the batch sdk messaging api level
     * @return The batch sdk messaging api level
     */
    public static String getSdkMessagingApiLevel() {
        return String.valueOf(Parameters.MESSAGING_API_LEVEL);
    }

    /**
     * Serialize a list of SystemParameter to a json object.
     * <p>
     * This is used to send the payload of an _NATIVE_DATA_CHANGE event when a native data change.
     * @param parameters A list of system parameter
     * @return A JSONObject related to the given list of parameters
     * @throws JSONException parsing exception
     */
    public static JSONObject serializeSystemParameters(@NonNull List<WatchedSystemParameter> parameters)
        throws JSONException {
        JSONObject serializedParameters = new JSONObject();
        for (WatchedSystemParameter parameter : parameters) {
            String paramKey = parameter.getShortName().serializedName;
            if (paramKey == null) {
                continue;
            }
            serializedParameters.put(
                paramKey,
                parameter.getLastValue() != null ? parameter.getLastValue() : JSONObject.NULL
            );
        }
        return serializedParameters;
    }
}
