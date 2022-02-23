package com.batch.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import com.batch.android.annotation.PublicSDK;

/**
 * Implementation of {@link android.app.Application.ActivityLifecycleCallbacks} for managing Batch's lifecycle
 * <p>
 * Important note: While this removes the need for most lifecycle activities, you still <b>MUST</b> add Batch.onNewIntent(this, intent) in all your activities
 *
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressWarnings("unused")
@PublicSDK
public class BatchActivityLifecycleHelper implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Batch.onCreate(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Batch.onStart(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        Batch.onStop(activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        Batch.onDestroy(activity);
    }
}
