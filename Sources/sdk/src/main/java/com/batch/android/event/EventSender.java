package com.batch.android.event;

import com.batch.android.FailReason;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.core.TaskRunnable;
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
 */
public class EventSender implements RetryTimerListener {

    private static final String TAG = "EventSender";

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

    /**
     * Constructor
     * @param runtimeManager runtime manager
     * @param listener event sender callback
     */
    public EventSender(RuntimeManager runtimeManager, EventSenderListener listener) {
        if (runtimeManager == null) {
            throw new NullPointerException("runtimeManager==null");
        }

        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        this.runtimeManager = runtimeManager;
        this.listener = listener;
        this.retryTimer = new RetryTimer(runtimeManager.getContext(), this);
    }

    /**
     * Try to send events
     */
    private void send() {
        send(false);
    }

    /**
     * Try to send events
     *
     * @param fromDelay is this send triggered after a retry delay
     */
    private void send(boolean fromDelay) {
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

                    getWebserviceTask(
                        events,
                        new TrackerWebserviceListener() {
                            @Override
                            public void onSuccess(final List<Event> events) {
                                // Inform retry timer of success
                                retryTimer.reset();

                                // Call listener if SDK is not OFF
                                runtimeManager.run(state -> {
                                    if (state != State.OFF) {
                                        listener.onEventsSendSuccess(events);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(FailReason reason, final List<Event> events) {
                                // Inform retry timer of failure and schedule a retry
                                retryTimer.reschedule();

                                // Call listener if SDK is not OFF
                                runtimeManager.run(state -> {
                                    if (state != State.OFF) {
                                        listener.onEventsSendFailure(events);
                                    }
                                });
                            }

                            @Override
                            public void onFinish() {
                                // Set sending to false
                                isSending.set(false);
                            }
                        }
                    )
                        .run();
                });
            }
        });
    }

    /**
     * Inform the sender that new events are available to send
     */
    public void hasNewEvents() {
        hasNewEvents.set(true);
        send();
    }

    /**
     * Retry method called in the RetryTimer when a task is rescheduled
     */
    @Override
    public void retry() {
        send(true);
    }

    /**
     * Get a task of the webservice to send the events
     *
     * @param events events to send
     * @param listener callback
     * @return Runnable task
     */
    private TaskRunnable getWebserviceTask(List<Event> events, TrackerWebserviceListener listener) {
        return WebserviceLauncher.initTrackerWebservice(runtimeManager, events, listener);
    }

    /**
     * Listener of the event sender
     */
    public interface EventSenderListener {
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
