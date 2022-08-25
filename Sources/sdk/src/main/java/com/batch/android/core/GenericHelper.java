package com.batch.android.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Generic helper that contains generic helpful methods
 *
 */
public class GenericHelper {

    /**
     * Check if the permission is available
     *
     * @param permission
     * @param context
     * @return
     */
    public static boolean checkPermission(String permission, Context context) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isWakeLockPermissionAvailable(@NonNull Context context) {
        try {
            return GenericHelper.checkPermission("android.permission.WAKE_LOCK", context);
        } catch (Exception e) {
            Logger.error("Error while checking android.permission.WAKE_LOCK permission", e);
            return false;
        }
    }

    public static boolean isTargetLowerThan13(@NonNull Context context) {
        // Note: any prerelease Android SDK, even older than 13, will return true here.
        // We do not care about that edge case.
        try {
            int targetSdkVersion = context.getApplicationContext().getApplicationInfo().targetSdkVersion;
            return targetSdkVersion < Build.VERSION_CODES.TIRAMISU;
        } catch (Exception e) {
            Logger.error("Could not check current target API level", e);
            return true;
        }
    }

    /**
     * Read the MD5 of a content
     *
     * @param content
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String readMD5(byte[] content) throws NoSuchAlgorithmException {
        byte[] md5 = MessageDigest.getInstance("MD5").digest(content);

        StringBuilder hex = new StringBuilder(md5.length * 2);
        for (byte b : md5) {
            int i = (b & 0xFF);
            if (i < 0x10) {
                hex.append('0');
            }

            hex.append(Integer.toHexString(i));
        }

        return hex.toString();
    }

    /**
     * Read the MD5 of a string
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String readMD5(final String toEncrypt) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("md5");
        digest.update(toEncrypt.getBytes());
        final byte[] bytes = digest.digest();
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }

        return sb.toString().toLowerCase(Locale.US);
    }

    /**
     * Return the screen density.
     *
     * @param applicationContext An activity/service context. Application context will not work.
     * @return Density if found, null otherwise
     */
    public static Float getScreenDensity(Context applicationContext) {
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager manager = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
            manager.getDefaultDisplay().getMetrics(metrics);

            return metrics.density;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert pixel value to DP
     *
     * @param px      pixel value
     * @param context
     * @return
     */
    public static float pixelToDP(int px, Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        if (px == 0) {
            return px;
        }

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    /**
     * Convert DP value to pixel
     *
     * @param dp      dp value
     * @param context
     * @return
     */
    public static int DPtoPixel(int dp, Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        if (dp == 0) {
            return dp;
        }

        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return Math.round((float) dp * metrics.density);
    }
}
