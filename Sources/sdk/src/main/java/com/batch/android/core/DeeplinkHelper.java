package com.batch.android.core;

import static android.content.Intent.ACTION_VIEW;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Locale;

/**
 * Simple helper for deeplinking related methods
 */
public class DeeplinkHelper {

    /**
     * Gets a VIEW intent that will result in a Custom Tab.
     * Note: On unsupported API Levels (<18), this will return null
     */
    @Nullable
    static Intent getCustomTabIntent(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return null;
        }

        final Intent i = new Intent(ACTION_VIEW);

        final Bundle b = new Bundle();
        b.putBinder("android.support.customtabs.extra.SESSION", null);
        b.putBoolean("android.support.customtabs.extra.SHARE_MENU_ITEM", true);
        b.putInt("android.support.customtabs.extra.TITLE_VISIBILITY", 1);

        i.putExtras(b);
        i.setData(uri);

        return i;
    }

    /**
     * Returns whether this URI can be potentially opened in a custom tab
     */
    static boolean customTabSupportsURI(@NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (scheme != null) {
            scheme = scheme.toLowerCase(Locale.US);
            return "http".equals(scheme) || "https".equals(scheme);
        }
        return false;
    }

    /**
     * Get the intent to open for a specified deeplink
     * <p>
     * Can throw runtime exceptions
     *
     * @param rawDeeplink            Raw deeplink to open
     * @param useCustomTabIfPossible Use a custom tab if supported
     * @param useNewTask             Open in a new task (unsupported on custom tabs)
     * @return Intent to attempt to open for the deeplink
     */
    @Nullable
    public static Intent getIntent(@NonNull String rawDeeplink, boolean useCustomTabIfPossible, boolean useNewTask) {
        Uri uri = Uri.parse(rawDeeplink);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            uri = uri.normalizeScheme();
        }

        if (useCustomTabIfPossible && customTabSupportsURI(uri)) {
            final Intent customTabIntent = getCustomTabIntent(uri);
            if (customTabIntent != null) {
                return customTabIntent;
            }
        }

        final Intent i = new Intent(ACTION_VIEW, uri);
        if (useNewTask) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        return i;
    }

    /**
     * Get the fallback intent for the current package.
     *
     * @param context
     * @return
     */
    public static Intent getFallbackIntent(Context context) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) {
            return null;
        }
        launchIntent.setAction("batch_" + Long.toString(System.currentTimeMillis()));
        return launchIntent;
    }
}
