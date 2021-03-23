package com.batch.android.runtime;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.batch.android.core.Logger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity Lifecycle Listener that tracks pause/resume count to know if the app is in the foreground
 * or not
 */
public class ForegroundActivityLifecycleListener implements Application.ActivityLifecycleCallbacks
{
    private static final String TAG = "ForegroundActivityLifecycleListener";

    private AtomicInteger resumeCount = new AtomicInteger(0);

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle)
    {

    }

    @Override
    public void onActivityStarted(Activity activity)
    {

    }

    @Override
    public void onActivityResumed(Activity activity)
    {
        resumeCount.incrementAndGet();
    }

    @Override
    public void onActivityPaused(Activity activity)
    {
        resumeCount.decrementAndGet();
    }

    @Override
    public void onActivityStopped(Activity activity)
    {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle)
    {

    }

    @Override
    public void onActivityDestroyed(Activity activity)
    {

    }

    //region: Foreground helper methods

    /**
     * Checks if the service's refcount is greater than 0
     */
    public boolean isApplicationInForeground()
    {
        int count = this.resumeCount.get();

        // Fix negative resume counts
        if (count < 0) {
            Logger.error(TAG,
                    "Batch's Activity resume counter is < 0. Something went wrong at some point with the activity lifecycles.");

            // Check that it is still in an invalid state, this may have changed while logging the issue.
            // Just like AtomicInteger's incrementAndGet() works, try until we are satisfied.
            for (; ; ) {
                int current = this.resumeCount.get();
                if (count < 0) {
                    if (this.resumeCount.compareAndSet(count, 0)) {
                        return false;
                    }
                } else {
                    // This was fixed by another thread, disregard and wish the developer well
                    return current != 0;
                }
            }
        }

        return count != 0;
    }

    //endregion
}
