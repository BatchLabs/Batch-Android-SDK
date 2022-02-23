package com.batch.android;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.DeeplinkHelper;

/**
 * Abstract class describing a deeplink interceptor.
 * An interceptor's job is to override some aspects of a deeplink that Batch wants to open.
 * See the various methods to see what you can override.
 */
@PublicSDK
public interface BatchDeeplinkInterceptor {
    /**
     * Called as a fallback when a previous task stack builder or intent could not be launched.
     * By default, use the {@link android.content.pm.PackageManager#getLaunchIntentForPackage(String)} method to get an intent.
     * If null is returned, the action is ignored.
     * <p>
     * Recommended: use this method when you want to customise the default intent for your app.
     * (not launching the LAUNCHER activity of your manifest for example)
     *
     * @param context The current context
     * @return The intent to launch
     */
    @Nullable
    default Intent getFallbackIntent(@NonNull Context context) {
        return DeeplinkHelper.getFallbackIntent(context);
    }

    /**
     * Called when a deeplink is triggered by Batch (only called from a push notification).
     * If null is returned {@link BatchDeeplinkInterceptor#getIntent(Context, String)} will be used.
     * <p>
     * Recommended: use this method when you want to add activities to the back stack when a push notification is clicked.
     *
     * @param context  The current context
     * @param deeplink The deeplink associated with the action
     * @return The task stack builder to launch
     */
    @Nullable
    TaskStackBuilder getTaskStackBuilder(@NonNull Context context, @NonNull String deeplink);

    /**
     * Called when a deeplink is triggered by Batch (could be from a push notification or an in-app message).
     * If null is returned the default behavior will be used.
     * <p>
     * Recommended: use this method when you want to customise the intent for a deeplink when a CTA/push is clicked.
     *
     * @param context  The current context
     * @param deeplink The deeplink associated with the action
     * @return The intent to launch
     */
    @Nullable
    Intent getIntent(@NonNull Context context, @NonNull String deeplink);
}
