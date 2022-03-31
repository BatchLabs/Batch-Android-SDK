package com.batch.android.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.batch.android.core.ExcludedActivityHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.localcampaigns.LocalCampaignsTracker;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks user sessions
 * A user session on Android is different than on iOS,
 * but also different from the SDK's start/stop:
 * - It starts only when an activity gets on screen
 * - It stops either:
 * - When the system pauses the last activity
 * - When the user destroys the last activity
 */
public class SessionManager implements ComponentCallbacks2, Application.ActivityLifecycleCallbacks {

    /**
     * Intent action for when a new session starts
     */
    public static final String INTENT_NEW_SESSION = Parameters.LIBRARY_BUNDLE + ".runtime.session.new";

    private static final String TAG = "BatchSessionManager";

    /**
     * Time until coming back from the background counts as a new session
     */
    private static final int BACKGROUNDED_SESSION_EXPIRATION_SEC = 300;

    private AtomicInteger createCount = new AtomicInteger(0);

    /**
     * Target uptime at which the background session will expire
     */
    private Long backgroundSessionExpirationUptime = null;

    private boolean sessionActive = false;

    /**
     * Unique session identifier
     */
    private String sessionIdentifier;

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    private synchronized void invalidateSessionIfNeeded() {
        if (
            sessionActive &&
            backgroundSessionExpirationUptime != null &&
            SystemClock.elapsedRealtime() >= backgroundSessionExpirationUptime
        ) {
            sessionActive = false;
            sessionIdentifier = null;
        }
    }

    synchronized void startNewSessionIfNeeded(@NonNull Context c) {
        invalidateSessionIfNeeded();
        if (!sessionActive) {
            sessionActive = true;
            sessionIdentifier = UUID.randomUUID().toString();

            // Reset the view tracker session count
            LocalCampaignsTracker tracker = (LocalCampaignsTracker) CampaignManagerProvider.get().getViewTracker();
            tracker.resetSessionViewsCount();

            // Broadcast the new session intent
            Logger.internal(TAG, "Starting a new session, id: '" + sessionIdentifier + "'");
            LocalBroadcastManagerProvider.get(c).sendBroadcast(new Intent(INTENT_NEW_SESSION));
        }
    }

    /**
     * Checks if the service's refcount is greater than 0
     */
    @VisibleForTesting
    protected boolean areAllActivitiesDestroyed() {
        int count = this.createCount.get();

        // Fix negative create counts
        if (count < 0) {
            Logger.error(
                TAG,
                "Batch's Activity create counter is < 0. Something went wrong at some point with the activity lifecycles."
            );

            // Check that it is still in an invalid state, this may have changed while logging the issue.
            // Just like AtomicInteger's incrementAndGet() works, try until we are satisfied.
            for (;;) {
                int current = this.createCount.get();
                if (count < 0) {
                    if (this.createCount.compareAndSet(count, 0)) {
                        return true;
                    }
                } else {
                    // This was fixed by another thread, disregard and wish the developer well
                    return current == 0;
                }
            }
        }

        return count == 0;
    }

    @VisibleForTesting
    protected long getUptime() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            backgroundSessionExpirationUptime = getUptime() + BACKGROUNDED_SESSION_EXPIRATION_SEC * 1000;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {}

    @Override
    public void onLowMemory() {
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
            return;
        }
        createCount.incrementAndGet();
    }

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
        if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
            return;
        }
        startNewSessionIfNeeded(activity.getApplicationContext());
        backgroundSessionExpirationUptime = null;
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (ExcludedActivityHelper.activityIsExcludedFromManifest(activity)) {
            return;
        }
        createCount.decrementAndGet();

        if (areAllActivitiesDestroyed()) {
            sessionActive = false;
            Logger.internal(TAG, "Finishing session since the last activity has been destroyed");
        }
    }
}
