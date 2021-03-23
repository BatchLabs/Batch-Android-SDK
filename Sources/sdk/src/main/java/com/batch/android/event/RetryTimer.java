package com.batch.android.event;

import android.content.Context;

import com.batch.android.FailReason;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to manage retry timing for event sending
 *
 */
public class RetryTimer
{
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
     * Reason of the scheduling
     */
    private FailReason reason;

// --------------------------------------->

    /**
     * @param context
     * @param listener
     */
    public RetryTimer(Context context, RetryTimerListener listener)
    {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        this.listener = listener;

        initialRetryDelay = Integer.parseInt(ParametersProvider.get(context).get(
                ParameterKeys.EVENT_TRACKER_INITIAL_DELAY));
        nextRetryDelay = initialRetryDelay;
        maxRetryDelay = Integer.parseInt(ParametersProvider.get(context).get(
                ParameterKeys.EVENT_TRACKER_MAX_DELAY));
    }

// --------------------------------------->

    /**
     * Is the retry timer currently waiting for a retry
     *
     * @return
     */
    public boolean isWaiting()
    {
        return retryTask != null;
    }

    /**
     * Method to call when a send of events fail
     *
     * @param reason
     */
    public void onSendFail(FailReason reason)
    {
        if (retryTask != null) {
            retryTask.cancel();
        }

        this.reason = reason;

        incrementDelay();
        retryTask = new TimerTask()
        {
            @Override
            public void run()
            {
                listener.retry();
            }
        };

        retryTimer.schedule(retryTask, nextRetryDelay);
    }

    /**
     * Method to call when a send of events succeed
     */
    public void onSendSuccess()
    {
        if (retryTask != null) {
            retryTask.cancel();
            retryTask = null;
            retryTimer.purge();
        }

        nextRetryDelay = initialRetryDelay;
    }

    /**
     * Method to call when internet is back
     */
    public void onInternetRetrieved()
    {
        if (reason == FailReason.NETWORK_ERROR) // Retry now only if we were waiting for network
        {
            onSendSuccess();
        }
    }

    /**
     * Increment the delay before the next retry
     */
    private void incrementDelay()
    {
        if (nextRetryDelay == initialRetryDelay) {
            nextRetryDelay++;
            return;
        }

        nextRetryDelay = nextRetryDelay * 2;
        if (nextRetryDelay > maxRetryDelay) {
            nextRetryDelay = maxRetryDelay;
        }
    }

// ---------------------------------------->

    /**
     * Listener of this retry Timer
     *
     */
    public interface RetryTimerListener
    {
        /**
         * Called by the timer when we should retry a send
         */
        void retry();
    }
}
