package com.batch.android.messaging.view.helper;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

// Theme related helpers
public class ThemeHelper {

    private ThemeHelper() {}

    // Returns the best default theme to use
    // This method will return, in order of availability
    //  - Material DayNight Theme
    //  - AppCompat DayNight Theme
    //  - AppCompat default DayNight Theme
    //  - Android default theme
    @StyleRes
    public static int getDefaultTheme(@NonNull Context context) {
        final Resources resources = context.getResources();
        final String packageName = context.getPackageName();

        int resolvedTheme;
        resolvedTheme = getThemeByName("Theme.MaterialComponents.DayNight", resources, packageName);
        if (resolvedTheme != 0) {
            return resolvedTheme;
        }
        resolvedTheme = getThemeByName("Theme.AppCompat.DayNight", resources, packageName);
        if (resolvedTheme != 0) {
            return resolvedTheme;
        }
        resolvedTheme = getThemeByName("Theme.AppCompat.Light", resources, packageName);
        if (resolvedTheme != 0) {
            return resolvedTheme;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return android.R.style.Theme_DeviceDefault_DayNight;
        }

        return android.R.style.Theme_DeviceDefault;
    }

    // Returns the best default light theme to use
    // This method will return, in order of availability
    //  - Material Light Theme
    //  - AppCompat Light Theme
    //  - Android default theme
    @StyleRes
    public static int getDefaultLightTheme(@NonNull Context context) {
        final Resources resources = context.getResources();
        final String packageName = context.getPackageName();

        int resolvedTheme;
        resolvedTheme = getThemeByName("Theme.MaterialComponents.Light", resources, packageName);
        if (resolvedTheme != 0) {
            return resolvedTheme;
        }
        resolvedTheme = getThemeByName("Theme.AppCompat.Light", resources, packageName);
        if (resolvedTheme != 0) {
            return resolvedTheme;
        }

        return android.R.style.Theme_DeviceDefault_Light;
    }

    @StyleRes
    private static int getThemeByName(
        @NonNull String themeName,
        @NonNull Resources resources,
        @NonNull String packageName
    ) {
        return resources.getIdentifier(themeName, "style", packageName);
    }
}
