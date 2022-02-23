package com.batch.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import java.util.Date;
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
        final Locale locale = Locale.getDefault();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locale.toLanguageTag();
        }

        // Pre lollipop fallback

        String language = locale.getLanguage();

        // fix wrong android device locale
        if (language.equals("in")) {
            return "id";
        }
        if (language.equals("iw")) {
            return "he";
        }
        if (language.equals("ji")) {
            return "yi";
        }
        return language;
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
     * @return
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
            PackageInfo informations = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);

            return informations.firstInstallTime;
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Get last update date.
     *
     * @param applicationContext The application context.
     * @return Last update date or null on failure.
     */
    @SuppressLint("NewApi")
    public static Long getLastUpdateDate(Context applicationContext) {
        try {
            PackageManager packageManager = applicationContext.getPackageManager();
            PackageInfo informations = packageManager.getPackageInfo(applicationContext.getPackageName(), 0);

            return informations.lastUpdateTime;
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
            PackageInfo infos = applicationContext
                .getPackageManager()
                .getPackageInfo(getBundleName(applicationContext), 0);
            return infos.versionName;
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
            PackageInfo informations = packageManager.getPackageInfo(
                applicationContext.getApplicationContext().getPackageName(),
                0
            );

            return informations.versionCode;
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

    private static ConnectivityManager getConnectivityManager(Context applicationContext) {
        if (!GenericHelper.checkPermission("android.permission.ACCESS_NETWORK_STATE", applicationContext)) {
            return null;
        }

        return (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static TelephonyManager getTelephonyManager(Context applicationContext) {
        return (TelephonyManager) applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /*
     * SIM informations.
     */

    /**
     * Get the telephony operator name.
     *
     * @param applicationContext The application context.
     * @return Operator name or null.
     */
    public static String getSimOperatorName(Context applicationContext) {
        /*
         * Need permission READ_PHONE_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.READ_PHONE_STATE", applicationContext)) {
            return null;
        }

        try {
            return SystemParameterHelper.getTelephonyManager(applicationContext).getSimOperatorName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the operator MCC+MNC (Mobile Country Code + Mobile Network Code)
     *
     * @param applicationContext The application context.
     * @return The Mobile code or null.
     */
    public static String getSimOperator(Context applicationContext) {
        try {
            return SystemParameterHelper.getTelephonyManager(applicationContext).getSimOperator();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the operator ISO country code.
     *
     * @param applicationContext The application context.
     * @return The country code of null.
     */
    public static String getSimCountryIso(Context applicationContext) {
        /*
         * Need permission READ_PHONE_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.READ_PHONE_STATE", applicationContext)) {
            return null;
        }

        try {
            return SystemParameterHelper.getTelephonyManager(applicationContext).getSimCountryIso();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the mobile network name.
     *
     * @param applicationContext The application context.
     * @return Network name or null.
     */
    public static String getNetworkOperatorName(Context applicationContext) {
        /*
         * Need permission READ_PHONE_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.READ_PHONE_STATE", applicationContext)) {
            return null;
        }

        try {
            return SystemParameterHelper.getTelephonyManager(applicationContext).getNetworkOperatorName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the network ISO country code.
     *
     * @param applicationContext The application context.
     * @return Network country core or null.
     */
    public static String getNetworkCountryIso(Context applicationContext) {
        /*
         * Need permission READ_PHONE_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.READ_PHONE_STATE", applicationContext)) {
            return null;
        }

        try {
            return SystemParameterHelper.getTelephonyManager(applicationContext).getNetworkCountryIso();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------->

    private static NetworkInfo getNetworkInfos(Context applicationContext) {
        /*
         *  Need the ACCESS_NETWORK_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.ACCESS_NETWORK_STATE", applicationContext)) {
            return null;
        }

        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) applicationContext.getSystemService(
                Context.CONNECTIVITY_SERVICE
            );
            // noinspection AndroidLintMissingPermission
            return connectivityManager.getActiveNetworkInfo();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Tell whether the network is in roaming mode.
     *
     * @param applicationContext The application context.
     * @return true/false, null on failure.
     */
    public static Boolean isNetRoaming(Context applicationContext) {
        /*
         *  Need the ACCESS_NETWORK_STATE
         */
        if (!GenericHelper.checkPermission("android.permission.ACCESS_NETWORK_STATE", applicationContext)) {
            return null;
        }

        try {
            return SystemParameterHelper.getNetworkInfos(applicationContext).isRoaming();
        } catch (Exception e) {
            return null;
        }
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

    // ------------------------------------------------------>

    /**
     * Get the screen height
     *
     * @param context
     * @return height if found, 0 on error
     */

    public static int getScreenHeight(Context context) {
        try {
            return getScreenSize(context).y;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get screen width
     *
     * @param context
     * @return width if found, 0 on error
     */
    public static int getScreenWidth(Context context) {
        try {
            return getScreenSize(context).x;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get the screen size of the device
     *
     * @param context
     * @return
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Point size = new Point();

        display.getSize(size);

        if (size.x >= size.y) { // Always consider that width should be < to height
            int y = size.y;

            size.y = size.x;
            size.x = y;
        }

        return size;
    }

    /**
     * Return the current display orientation ( http://stackoverflow.com/a/4698003 )
     *
     * @param context
     * @return {@link Configuration#ORIENTATION_LANDSCAPE} or {@link Configuration#ORIENTATION_PORTRAIT}. {@link Configuration#ORIENTATION_UNDEFINED} on error
     */
    public static int getScreenOrientation(Context context) {
        try {
            return context.getResources().getConfiguration().orientation;
        } catch (Exception e) {
            return Configuration.ORIENTATION_UNDEFINED;
        }
    }

    /**
     * Return the connected network kind
     *
     * @param context
     * @return null if unknown (usually means that ACCESS_NETWORK_STATE isn't granted), 1 for wifi/ethernet/dummy, 0 for everything else.
     */
    public static Integer getNetworkKind(Context context) {
        try {
            ConnectivityManager connectivityMgr = getConnectivityManager(context);
            if (connectivityMgr == null) {
                return null;
            }

            // noinspection AndroidLintMissingPermission
            NetworkInfo activeNetworkInfo = connectivityMgr.getActiveNetworkInfo();
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                return null;
            }

            int type = activeNetworkInfo.getType();

            if (
                type == ConnectivityManager.TYPE_WIFI ||
                type == ConnectivityManager.TYPE_DUMMY ||
                type == ConnectivityManager.TYPE_ETHERNET ||
                type == ConnectivityManager.TYPE_WIMAX
            ) {
                return 1;
            }

            return 0;
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------>

    /**
     * Get property data from it's short parameter name.
     *
     * @param shortName
     * @return
     */
    public static String getValue(String shortName, Context context) {
        // Find short parameter in enum.
        SystemParameterShortName param = null;
        try {
            param = SystemParameterShortName.fromShortValue(shortName);
        } catch (IllegalStateException e) {
            Logger.internal(TAG, "Invalid short name : " + shortName);
            return null;
        }

        // Retrieve the value.
        String value = null;
        switch (param) {
            case BUNDLE_NAME:
                value = getBundleName(context);
                break;
            case DEVICE_TIMEZONE:
                value = getDeviceTimezone();
                break;
            case FIRST_INSTALL_DATE:
                value = Webservice.formatDate(new Date(getFirstInstallDate(context)));
                break;
            case LAST_UPDATE_DATE:
                value = Webservice.formatDate(new Date(getLastUpdateDate(context)));
                break;
            case BRAND:
                value = getDeviceBrand();
                break;
            case SDK_LEVEL:
                value = String.valueOf(Build.VERSION.SDK_INT);
                break;
            case APPLICATION_VERSION:
                value = getAppVersion(context);
                break;
            case APPLICATION_CODE:
                value = String.valueOf(getAppVersionCode(context));
                break;
            case OS_VERSION:
                value = getOSVersion();
                break;
            case SIM_OPERATOR_NAME:
                value = getSimOperatorName(context);
                break;
            case SIM_OPERATOR:
                value = getSimOperator(context);
                break;
            case SIM_COUNTRY:
                value = getSimCountryIso(context);
                break;
            case NETWORK_NAME:
                value = getNetworkOperatorName(context);
                break;
            case NETWORK_COUNTRY:
                value = getNetworkCountryIso(context);
                break;
            case ROAMING:
                value = String.valueOf(isNetRoaming(context));
                break;
            case DEVICE_LANGUAGE:
                value = getDeviceLanguage();
                break;
            case DEVICE_REGION:
                value = getDeviceCountry();
                break;
            case DEVICE_TYPE:
                value = getDeviceModel();
                break;
            case DEVICE_DATE:
                value = getDeviceDate();
                break;
            case API_LEVEL:
                value = String.valueOf(Parameters.API_LEVEL);
                break;
            case MESSAGING_API_LEVEL:
                value = String.valueOf(Parameters.MESSAGING_API_LEVEL);
                break;
            case SCREEN_HEIGHT:
                value = String.valueOf(getScreenHeight(context));
                break;
            case SCREEN_WIDTH:
                value = String.valueOf(getScreenWidth(context));
                break;
            case SCREEN_ORIENTATION:
                {
                    int orientation = getScreenOrientation(context);
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        return "L";
                    } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        return "P";
                    } else {
                        return "U";
                    }
                }
            case NETWORK_KIND:
                {
                    Integer kind = getNetworkKind(context);
                    if (kind == null) {
                        return null;
                    } else {
                        return String.valueOf(kind);
                    }
                }
            default:
                break;
        }

        return value;
    }
}
