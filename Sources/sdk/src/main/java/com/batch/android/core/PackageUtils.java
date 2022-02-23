package com.batch.android.core;

import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PackageUtils {

    private PackageUtils() {}

    // Check if a package is installed and enabled
    public static boolean isPackageInstalled(@Nullable PackageManager packageManager, @NonNull String packageName) {
        if (packageManager == null) {
            return false;
        }
        try {
            return packageManager.getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }
}
