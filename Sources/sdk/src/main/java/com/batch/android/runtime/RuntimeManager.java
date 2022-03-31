package com.batch.android.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.core.Logger;
import com.batch.android.debug.FindMyInstallationHelper;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Manager that contains library state and locks
 */
@Module
@Singleton
public class RuntimeManager {

    private static final String TAG = "RuntimeManager";

    /**
     * Application context
     */
    private Context context;
    /**
     * Handler stored to execute actions on main thread
     */
    private Handler handler = new Handler(Looper.getMainLooper());

    // ---- Context tracking variables ----

    /**
     * RefCount incremented/decremented by onServiceCreate/onServiceDestroy calls.
     * May be replaced by a Context list later.
     */
    private AtomicInteger serviceRefCount = new AtomicInteger(0);
    /**
     * Date of when Batch was last started by a service WITH userActivity = true (or an Activity)
     * Used for automatic 24h start in activities/services
     */
    private Date lastUserStartDate;
    /**
     * Activity stored
     */
    private Activity activity;

    /**
     * Foreground activity lifecycle listener.
     * Used to track if the application is in the foreground or background.
     */
    private ForegroundActivityLifecycleListener foregroundActivityLifecycleListener;

    /**
     * Session manager
     * Used to generate session ids and user activity based sessions
     * not based on Batch's state
     */
    private SessionManager sessionManager;

    // ------------------------------------

    /**
     * Date of the last stop without finishing
     */
    private Date stopDate;

    /**
     * Current state
     */
    private State state = State.OFF;
    /**
     * Lock used to modify/get the {@link #state}
     */
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * Read lock
     */
    private ReadLock r = lock.readLock();
    /**
     * Write lock
     */
    private WriteLock w = lock.writeLock();

    /**
     * Debug helper class to copy the installation id to the clipboard
     */
    private final FindMyInstallationHelper installationIdHelper = new FindMyInstallationHelper();

    // ------------------------------------->

    public RuntimeManager() {}

    // -------------------------------------->

    /**
     * Execute an action to modify the state
     *
     * @param action action that will modify the given state
     * @return true if the state has been set, false otherwise
     */
    public boolean changeState(ChangeStateAction action) {
        w.lock();
        try {
            State newState = action.run(state);
            if (newState != null) {
                state = newState;
                return true;
            }

            return false;
        } finally {
            w.unlock();
        }
    }

    /**
     * Execute an action to modify the state if the current state equals the wanted one
     *
     * @param wantedState
     * @param action
     * @return true if the state has been set, false otherwise
     */
    public boolean changeStateIf(State wantedState, ChangeStateAction action) {
        w.lock();
        try {
            if (state != wantedState) {
                return false;
            }

            State newState = action.run(state);
            if (newState != null) {
                state = newState;
                return true;
            }

            return false;
        } finally {
            w.unlock();
        }
    }

    // ------------------------------------>

    /**
     * Execute an action with read lock
     *
     * @param action action that can read the given state
     */
    public void run(StateAction action) {
        r.lock();
        try {
            action.run(state);
        } finally {
            r.unlock();
        }
    }

    /**
     * Execute an action with read lock if the current state equals the wanted state
     *
     * @param wantedState
     * @param action
     * @return true if the action has been run, false otherwise
     */
    public boolean runIf(State wantedState, StateAction action) {
        r.lock();
        try {
            if (state != wantedState) {
                return false;
            }

            action.run(state);
            return true;
        } finally {
            r.unlock();
        }
    }

    // -------------------------------------->

    /**
     * Run this action if Batch is ready
     *
     * @param action
     * @return true if the action has been runned, false otherwise
     */
    public boolean runIfReady(final Runnable action) {
        return runIf(State.READY, action);
    }

    /**
     * Run this action if Batch is at the given wanted state
     *
     * @param wantedState
     * @param action
     * @return true if the action has been runned, false otherwise
     */
    public boolean runIf(State wantedState, final Runnable action) {
        r.lock();
        try {
            if (state != wantedState) {
                return false;
            }

            action.run();

            return true;
        } finally {
            r.unlock();
        }
    }

    // ------------------------------------------->

    /**
     * Method to call when onStart is called to get the last stop timestamp (if any)<br>
     * <b>This method is NOT thread safe, should only be called on a changeState method</b>
     *
     * @return the timestamp of the last stop if any, null otherwise. If null, you should always restart
     */
    public Long onStart() {
        try {
            return stopDate != null ? stopDate.getTime() : null;
        } finally {
            stopDate = null;
        }
    }

    /**
     * Method to call when onStop is called without a finishing activity
     * <b>This method is NOT thread safe, should only be called on a changeState method</b>
     */
    public void onStopWithoutFinishing() {
        // If state is already off or finish, do nothing
        if (state != State.READY) {
            return;
        }

        stopDate = new Date();
    }

    // ------------------------------------------->

    /**
     * Set the activity, you should <b>NEVER</b> call this method outside of a changeState method<br>
     * <b>This method is NOT thread safe</b>
     *
     * @param activity
     */
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    /**
     * Get the activity
     *
     * @return activity or null depending on the state
     */
    public Activity getActivity() {
        return activity;
    }

    /**
     * Increment the number of calls to onServiceCreate registered
     */
    public void incrementServiceRefCount() {
        this.serviceRefCount.incrementAndGet();
    }

    /**
     * Decrement the number of services retaining Batch.
     */
    public void decrementServiceRefCount() {
        this.serviceRefCount.decrementAndGet();
    }

    /**
     * Forces the service ref count to be zero
     * Dangerous
     */
    public void resetServiceRefCount() {
        this.serviceRefCount.set(0);
    }

    /**
     * Checks if Batch is ready
     */
    public boolean isReady() {
        return state == State.READY;
    }

    /**
     * Checks if the service's refcount is greater than 0
     */
    public boolean isRetainedByService() {
        int refCount = this.serviceRefCount.get();

        // Fix negative refcounts
        if (refCount < 0) {
            Logger.error(
                TAG,
                "Batch has been under-locked. You probably called Batch.onServiceDestroy() too many times. Recovering, but this may leave Batch in an undesired state."
            );

            // Check that it is still underlocked, this may have changed while logging the issue.
            // Just like AtomicInteger's incrementAndGet() works, try until we are satisfied.
            for (;;) {
                int current = this.serviceRefCount.get();
                if (refCount < 0) {
                    if (this.serviceRefCount.compareAndSet(refCount, 0)) {
                        return false;
                    }
                } else {
                    // This was fixed by another thread, disregard and wish the developer well
                    return current != 0;
                }
            }
        }

        return refCount != 0;
    }

    /**
     * Set the last time the Batch was started for user activity to now.
     * You should <b>NEVER</b> call this method outside of a changeState method<br>
     * <b>This method is NOT thread safe</b>
     */
    public void updateLastUserStartDate() {
        this.lastUserStartDate = new Date();
    }

    /**
     * Get the last time Batch was started for user activity.
     *
     * @return last user activity start date or null depending on the state
     */
    public Date getLastUserStartDate() {
        return this.lastUserStartDate;
    }

    /**
     * Set the context, you should <b>NEVER</b> call this method outside of a changeState method<br>
     * <b>This method is NOT thread safe</b>
     *
     * @param context
     */
    public void setContext(Context context) {
        if (context != null) { // Just in case we got an activity
            context = context.getApplicationContext();
        }

        this.context = context;
    }

    /**
     * Get the context
     *
     * @return context or null
     */
    @Nullable
    public Context getContext() {
        return context;
    }

    // ----------------------------------------->

    public void registerActivityListenerIfNeeded(@NonNull Application application) {
        if (foregroundActivityLifecycleListener == null) {
            foregroundActivityLifecycleListener = new ForegroundActivityLifecycleListener();
            foregroundActivityLifecycleListener.registerAppLifecycleListener(
                new ForegroundActivityLifecycleListener.AppLifecycleListener() {
                    @Override
                    public void onEnterForeground() {
                        installationIdHelper.notifyForeground();
                    }

                    @Override
                    public void onEnterBackground() {}
                }
            );
            application.registerActivityLifecycleCallbacks(foregroundActivityLifecycleListener);
        }
    }

    public boolean isApplicationInForeground() {
        if (foregroundActivityLifecycleListener == null) {
            return false;
        }

        return foregroundActivityLifecycleListener.isApplicationInForeground();
    }

    // ----------------------------------------->

    public void registerSessionManagerIfNeeded(@NonNull Application application, boolean simulateActivityStart) {
        if (sessionManager == null) {
            sessionManager = new SessionManager();
            application.registerActivityLifecycleCallbacks(sessionManager);
            application.registerComponentCallbacks(sessionManager);

            // FIXME: Once we start asking for the application instance in setConfig, we can avoid this shit
            if (simulateActivityStart) {
                final Activity activity = getActivity();
                if (activity != null) {
                    sessionManager.onActivityCreated(activity, null);
                    sessionManager.onActivityStarted(activity);
                    sessionManager.startNewSessionIfNeeded(activity);
                } else {
                    Logger.error(
                        TAG,
                        "Could not replay activity lifecycle on the session manager, since no activity was set." +
                        " This should not happen. Sessions will NOT be tracked correctly. Please report this to Batch's support."
                    );
                }
            }
        }
    }

    public String getSessionIdentifier() {
        if (sessionManager == null) {
            return null;
        }

        return sessionManager.getSessionIdentifier();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    @VisibleForTesting
    public void clearSessionManager() {
        sessionManager = null;
    }
}
