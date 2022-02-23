package com.batch.android.event;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to manage retry timing for event sending
 */
public class RetryTimer {

    public static final String TAG = "RetryTimer";

    /**
     * Maximal number of retries
     */
    private static final int MAX_RETRIES = 3;
    /**
     * Number of retries done
     */
    private int retries = 0;
    /**
     * Initial delay of the first retry
     */
    private int initialRetryDelay;
    /**
     * Max delay between 2 retry
     */
    private int maxRetryDelay;
    /**
     * Time to wait before the next retry (used to schedule the Timer)
     */
    private int nextRetryDelay;
    /**
     * Timer, to schedule things
     */
    private Timer retryTimer = new Timer();
    /**
     * Saved task (used to cancel it later)
     */
    private TimerTask retryTask;
    /**
     * Listener of this retry timer
     */
    private RetryTimerListener listener;

    /**
     * Constructor
     *
     * @param context  context
     * @param listener callback
     */
    public RetryTimer(Context context, RetryTimerListener listener) {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        this.listener = listener;

        initialRetryDelay =
            Integer.parseInt(ParametersProvider.get(context).get(ParameterKeys.EVENT_TRACKER_INITIAL_DELAY));
        nextRetryDelay = initialRetryDelay;
        maxRetryDelay = Integer.parseInt(ParametersProvider.get(context).get(ParameterKeys.EVENT_TRACKER_MAX_DELAY));
    }

    /**
     * Is the retry timer currently waiting for a retry
     *
     * @return true is waiting
     */
    public boolean isWaiting() {
        return retryTask != null;
    }

    /**
     * Schedule a retry for the current task
     * Method to call when a send of events has failed
     */
    public void reschedule() {
        if (retryTask != null) {
            retryTask.cancel();
        }
        incrementDelay();
        retryTask =
            new TimerTask() {
                @Override
                public void run() {
                    retries++;
                    listener.retry();
                }
            };
        if (retries >= MAX_RETRIES) {
            Logger.internal(TAG, "The event sender has reached the max retries threshold.");
            reset();
            return;
        }
        retryTimer.schedule(retryTask, nextRetryDelay);
    }

    /**
     * Reset all flags of this retry timer
     * Method to call when a send of events succeed or when
     * this retry timer has reached the max retries threshold
     */
    public void reset() {
        if (retryTask != null) {
            retryTask.cancel();
            retryTask = null;
            retryTimer.purge();
        }
        retries = 0;
        nextRetryDelay = initialRetryDelay;
    }

    /**
     * Increment the delay before the next retry
     */
    private void incrementDelay() {
        if (nextRetryDelay == initialRetryDelay) {
            nextRetryDelay++;
            return;
        }

        nextRetryDelay = nextRetryDelay * 2;
        if (nextRetryDelay > maxRetryDelay) {
            nextRetryDelay = maxRetryDelay;
        }
    }

    /**
     * Listener of this retry Timer
     */
    public interface RetryTimerListener {
        /**
         * Called by the timer when we should retry a send
         */
        void retry();
    }
}
