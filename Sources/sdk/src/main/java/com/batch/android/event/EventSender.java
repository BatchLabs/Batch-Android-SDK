package com.batch.android.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.batch.android.FailReason;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.core.Reachability;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.Webservice;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.event.RetryTimer.RetryTimerListener;
import com.batch.android.runtime.RuntimeManager;
import com.batch.android.runtime.State;
import com.batch.android.webservice.listener.TrackerWebserviceListener;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class that is responsible of sending events to server
 *
 */
public abstract class EventSender implements RetryTimerListener
{
    private static final String TAG = "EventSender";

    /**
     * Broadcast receiver
     */
    private BroadcastReceiver receiver;
    /**
     * Runtime manager instance
     */
    protected RuntimeManager runtimeManager;
    /**
     * Listener
     */
    private EventSenderListener listener;
    /**
     * Atomic boolean that define if the sender is currently sending
     */
    private AtomicBoolean isSending = new AtomicBoolean(false);
    /**
     * Atomic boolean that define if the sender have new events to send
     */
    private AtomicBoolean hasNewEvents = new AtomicBoolean(false);
    /**
     * Single thread executor
     */
    private ExecutorService sendExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory());
    /**
     * Retry timer custom class
     */
    private RetryTimer retryTimer;

// -------------------------------------------->

    /**
     * @param runtimeManager
     * @param listener
     */
    public EventSender(RuntimeManager runtimeManager, EventSenderListener listener)
    {
        if (runtimeManager == null) {
            throw new NullPointerException("runtimeManager==null");
        }

        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        this.runtimeManager = runtimeManager;
        this.listener = listener;
        this.retryTimer = new RetryTimer(runtimeManager.getContext(), this);

        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (getWebserviceFinishedEvent().equals(intent.getAction())) {
                    webserviceFinished();
                } else if (Webservice.WEBSERVICE_SUCCEED_EVENT.equals(intent.getAction())) {
                    onConnectionReady();
                } else if (Reachability.CONNECTIVITY_CHANGED_EVENT.equals(intent.getAction())) {
                    boolean isConnected = intent.getBooleanExtra(Reachability.IS_CONNECTED_KEY,
                            true);
                    if (isConnected) {
                        onConnectionReady();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(getWebserviceFinishedEvent());
        filter.addAction(Webservice.WEBSERVICE_SUCCEED_EVENT);
        filter.addAction(Reachability.CONNECTIVITY_CHANGED_EVENT);

        LocalBroadcastManagerProvider.get(runtimeManager.getContext()).registerReceiver(
                receiver,
                filter);
    }

// -------------------------------------------->

    /**
     * Try to send events
     */
    private void send()
    {
        send(false);
    }

    /**
     * Try to send events
     *
     * @param fromDelay is this send triggered after a retry delay
     */
    private void send(boolean fromDelay)
    {
        if (isSending.get()) {
            return; // Already sending
        }

        if (!fromDelay && retryTimer.isWaiting()) {
            return; // Delaying
        }

        // Send only if ready
        runtimeManager.runIfReady(() -> {
            // If not already sending
            if (isSending.compareAndSet(false, true)) {
                sendExecutor.submit(() -> {
                    /*
                     * Get events to send
                     */
                    List<Event> events = listener.getEventsToSend();
                    hasNewEvents.set(false);

                    // If there's nothing to send, just stop here
                    if (events.isEmpty()) {
                        isSending.set(false);
                        return;
                    }

                    Logger.internal(TAG, "Start sending events : " + events.size());

                    getWebserviceTask(events, new TrackerWebserviceListener()
                    {
                        @Override
                        public void onSuccess(final List<Event> events)
                        {
                            // Inform retry timer of success
                            retryTimer.onSendSuccess();

                            // Call listener if SDK is not OFF
                            runtimeManager.run(state -> {
                                if (state != State.OFF) {
                                    listener.onEventsSendSuccess(events);
                                }
                            });

                        }

                        @Override
                        public void onFailure(FailReason reason, final List<Event> events)
                        {
                            // Inform retry timer of failure
                            retryTimer.onSendFail(reason);

                            // Call listener if SDK is not OFF
                            runtimeManager.run(state -> {
                                if (state != State.OFF) {
                                    listener.onEventsSendFailure(events);
                                    hasNewEvents.set(true);
                                }
                            });
                        }

                        @Override
                        public void onFinish()
                        {
                            // Set sending to false
                            isSending.set(false);

                            // Send finish broadcast if SDK is not off
                            runtimeManager.run(state -> {
                                if (state != State.OFF) {
                                    LocalBroadcastManagerProvider.get(runtimeManager.getContext())
                                            .sendBroadcast(
                                                    new Intent(getWebserviceFinishedEvent()));
                                }
                            });
                        }
                    }).run();
                });
            }
        });
    }


    /**
     * Inform the sender that new events are available to send
     */
    public void hasNewEvents()
    {
        hasNewEvents.set(true);
        send();
    }

    /**
     * Handle webservice finished
     */
    private void webserviceFinished()
    {
        if (hasNewEvents.get()) {
            send();
        }
    }

    /**
     * Handle connection ready event (when network is back)
     */
    private void onConnectionReady()
    {
        retryTimer.onInternetRetrieved();

        if (hasNewEvents.get()) {
            send();
        }
    }

// -------------------------------------------->

    @Override
    public void retry()
    {
        send(true);
    }

// -------------------------------------------->

    /**
     * Event to broadcast when webservice finish (Intent action)
     *
     * @return
     */
    protected abstract String getWebserviceFinishedEvent();

    /**
     * Should return a task runnable of the webservice to send events
     *
     * @param events
     * @param listener
     * @return
     */
    protected abstract TaskRunnable getWebserviceTask(List<Event> events,
                                                      TrackerWebserviceListener listener);

// -------------------------------------------->

    /**
     * Listener of the event sender
     *
     */
    public interface EventSenderListener
    {
        /**
         * Called when events has been sent to server
         *
         * @param events sent events
         */
        void onEventsSendSuccess(List<Event> events);

        /**
         * Called when events has failed to be sent to server
         *
         * @param events events not sent
         */
        void onEventsSendFailure(List<Event> events);

        /**
         * Datasource method that should provide a list of events to send
         *
         * @return an empty list if there's nothing to send (Should never return null!)
         */
        List<Event> getEventsToSend();
    }
}
