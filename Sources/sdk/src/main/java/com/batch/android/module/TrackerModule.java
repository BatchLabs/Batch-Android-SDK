package com.batch.android.module;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.BatchPushRegistration;
import com.batch.android.FailReason;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.Promise;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.event.CollapsibleEvent;
import com.batch.android.event.Event;
import com.batch.android.event.EventSender;
import com.batch.android.event.EventSender.EventSenderListener;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.ViewTracker;
import com.batch.android.localcampaigns.ViewTrackerUnavailableException;
import com.batch.android.localcampaigns.signal.EventTrackedSignal;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.runtime.State;
import com.batch.android.tracker.TrackerDatasource;
import com.batch.android.webservice.listener.TrackerWebserviceListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event Tracker module of Batch
 *
 */
@Module
@Singleton
public final class TrackerModule extends BatchModule implements EventSenderListener {

    public static final String TAG = "Tracker";

    /**
     * The datasource instance
     */
    private TrackerDatasource datasource;

    /**
     * Memory event queue that buffers them before saving them in SQLite
     */
    private final Queue<Event> memoryStorage = new ConcurrentLinkedQueue<>();

    /**
     * Executor responsible for persisting the data in the Queue to SQLite
     */
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory());

    /**
     * Boolean that tells if the flushExecutor is working
     */
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    /**
     * Event sender instance
     */
    private EventSender sender;

    /**
     * Quantity of events to send in a row
     */
    private int batchSendQuantity;

    /**
     * Modules
     */
    private final OptOutModule optOutModule;
    private final LocalCampaignsModule localCampaignsModule;
    private final CampaignManager campaignManager;
    private final PushModule pushModule;

    // -------------------------------------------->

    private TrackerModule(
        OptOutModule optOutModule,
        LocalCampaignsModule localCampaignsModule,
        CampaignManager campaignManager,
        PushModule pushModule
    ) {
        this.optOutModule = optOutModule;
        this.localCampaignsModule = localCampaignsModule;
        this.campaignManager = campaignManager;
        this.pushModule = pushModule;
    }

    @Provide
    public static TrackerModule provide() {
        return new TrackerModule(
            OptOutModuleProvider.get(),
            LocalCampaignsModuleProvider.get(),
            CampaignManagerProvider.get(),
            PushModuleProvider.get()
        );
    }

    @Override
    public String getId() {
        return "tracker";
    }

    @Override
    public int getState() {
        return 1;
    }

    @Override
    public void batchWillStart() {
        try {
            batchSendQuantity =
                Integer.parseInt(
                    ParametersProvider
                        .get(RuntimeManagerProvider.get().getContext())
                        .get(ParameterKeys.EVENT_TRACKER_BATCH_QUANTITY)
                );

            //Start the event tracker.
            Context context = RuntimeManagerProvider.get().getContext();
            if (context == null) {
                throw new NullPointerException("Context cannot be null");
            }

            int eventsToDeleteLimit = Integer.parseInt(
                ParametersProvider.get(context).get(ParameterKeys.EVENT_TRACKER_EVENTS_LIMIT)
            );

            datasource = new TrackerDatasource(context.getApplicationContext());
            int overflowEventsDeleted = datasource.deleteOverflowEvents(eventsToDeleteLimit);
            Logger.internal(TAG, "Deleted " + overflowEventsDeleted + " overflow events");
            datasource.resetEventStatus();

            // Not reconstructed everytime to keep track of the state and only if we are in ON mode
            if (sender == null) {
                sender = new EventSender(RuntimeManagerProvider.get(), this);
            }
        } catch (Exception e) {
            Logger.error(TAG, "Error while starting tracker module", e);
        }
    }

    @Override
    public void batchDidStart() {
        // If it got events before it was started, they will be immediately written to SQLite and sent as new.
        if (!memoryStorage.isEmpty()) {
            flush();
        }
    }

    @Override
    public void batchDidStop() {
        //Stop the event tracker. It will still store events in memory, but they will not be saved in SQLite or sent to the server.
        if (!isFlushing.get()) {
            // If the flush executor is running, it will take care of cleaning up the datasource once finished.
            closeDatasource();
        }
    }

    // -------------------------------------------->

    /**
     * Track an event by its name
     *
     * @param name The name of the event
     */
    public void track(String name) {
        track(name, null);
    }

    /**
     * Track an event by its name with additional parameters
     *
     * @param name The name of the event
     * @param parameters The parameters of the event
     */
    public void track(String name, JSONObject parameters) {
        track(name, new Date().getTime(), parameters);
    }

    /**
     * Track en event by its name, timestamp and with additional parameters
     *
     * @param name The name of the event
     * @param timestamp The timestamp of the event
     * @param parameters The parameters of the event
     */
    public void track(String name, long timestamp, JSONObject parameters) {
        if (Boolean.TRUE.equals(optOutModule.isOptedOut())) {
            Logger.internal(TAG, "Batch is opted out from, refusing to track event.");
        }

        if (Parameters.ENABLE_DEV_LOGS && parameters != null) {
            Logger.internal(TAG, "Tracking event " + name + " with parameters " + parameters);
        } else {
            Logger.internal(TAG, "Tracking event " + name);
        }
        memoryStorage.add(new Event(RuntimeManagerProvider.get().getContext(), timestamp, name, parameters));
        flush();

        if (RuntimeManagerProvider.get().isReady()) {
            localCampaignsModule.sendSignal(new EventTrackedSignal(name, parameters));
        }
    }

    /**
     * Track a collapsible event by its name, timestamp and with additional parameters
     *
     * @param name The name of the event
     * @param timestamp The timestamp of the event
     * @param parameters The parameters of the event
     */
    public void trackCollapsible(String name, long timestamp, JSONObject parameters) {
        if (Boolean.TRUE.equals(optOutModule.isOptedOut())) {
            Logger.internal(TAG, "Batch is opted out from, refusing to track collapsible event.");
        }

        Logger.internal(TAG, "Tracking collapsible event : " + name);
        memoryStorage.add(new CollapsibleEvent(RuntimeManagerProvider.get().getContext(), timestamp, name, parameters));
        flush();

        localCampaignsModule.sendSignal(new EventTrackedSignal(name, parameters));
    }

    /**
     * Track an event by its name and timestamp
     *
     * @param name The name of the event
     * @param timestamp The timestamp of the event
     */
    public void track(String name, long timestamp) {
        track(name, timestamp, null);
    }

    /**
     * Track local campaign view
     *
     * @param campaignID The campaign identifier
     * @param eventData The data of the event
     */
    public void trackCampaignView(@NonNull String campaignID, @NonNull JSONObject eventData) {
        ViewTracker vt = campaignManager.getViewTracker();
        if (vt == null) {
            return;
        }
        ViewTracker.CountedViewEvent ev;
        try {
            ev = vt.trackViewEvent(campaignID);
        } catch (ViewTrackerUnavailableException e) {
            Logger.internal(TAG, "View tracker not available, not tracking view");
            return;
        }

        try {
            JSONObject params = new JSONObject();
            params.put("ed", eventData);
            params.put("count", ev.count);
            params.put("last", ev.lastOccurrence);
            params.put("id", ev.campaignID);

            track(InternalEvents.LOCAL_CAMPAIGN_VIEWED, params);
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not track " + InternalEvents.LOCAL_CAMPAIGN_VIEWED, e);
        }
    }

    /**
     * Track the opt-in event
     *
     * @param context Android's context
     * @throws JSONException Serialization exception
     */
    void trackOptInEvent(final Context context) throws JSONException {
        track(InternalEvents.OPT_IN, makeOptBaseEventData(context));
    }

    /**
     * Track the opt-out event
     *
     * @param context Android's context
     * @param name The name of the event
     * @return A promise resolving when the request has finished
     */
    Promise<Void> trackOptOutEvent(final Context context, String name) {
        // iOS has debouncing, but is it really useful?
        try {
            JSONObject data = makeOptBaseEventData(context);

            final List<Event> eventsToSend = new ArrayList<>();
            eventsToSend.add(new Event(context, new Date().getTime(), name, data));

            return new Promise<>(promise ->
                new NamedThreadFactory()
                    .newThread(
                        WebserviceLauncher.initOptOutTrackerWebservice(
                            context,
                            eventsToSend,
                            new TrackerWebserviceListener() {
                                @Override
                                public void onSuccess(List<Event> events) {
                                    promise.resolve(null);
                                }

                                @Override
                                public void onFailure(FailReason reason, List<Event> events) {
                                    promise.reject(null);
                                }

                                @Override
                                public void onFinish() {}
                            }
                        )
                    )
                    .start()
            );
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not make opt-out event data", e);
            return Promise.rejected(e);
        }
    }

    private JSONObject makeOptBaseEventData(Context context) throws JSONException {
        final JSONObject data = new JSONObject();

        Parameters params = ParametersProvider.get(context);

        String installID = params.get(ParameterKeys.INSTALL_ID_KEY);
        if (installID != null) {
            data.put("di", installID);
        }

        String customID = params.get(ParameterKeys.CUSTOM_ID);
        if (customID != null) {
            data.put("cus", customID);
        }

        BatchPushRegistration reg = pushModule.getRegistration(context);
        if (reg != null) {
            data.put("tok", reg.getToken());
            data.put("provider", reg.getProvider());
            if (reg.getSenderID() != null) {
                data.put("senderid", reg.getSenderID());
            }
            if (reg.getGcpProjectID() != null) {
                data.put("gcpproject", reg.getGcpProjectID());
            }
        }
        return data;
    }

    /**
     * Close the datasource. Make sure that you've stopped writing.
     */
    private void closeDatasource() {
        try {
            if (datasource != null) {
                datasource.close();
            }
        } catch (Exception e) {
            Logger.error(TAG, "Error while closing DB", e);
        }

        datasource = null;
    }

    private void flush() {
        /* Don't bother starting the flush executor if
         *  - Batch isn't started since we don't have SQLite
         *  - It's already running
         */

        // Flusher is already flushing :p
        if (isFlushing.get()) {
            Logger.internal(TAG, "Flush called while already flushing");
            return;
        }

        if (datasource == null) {
            Logger.internal(TAG, "Flush called in State OFF, not flushing");
            return;
        }

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state != State.OFF) {
                    if (isFlushing.compareAndSet(false, true)) { // Re-test and set
                        Logger.internal(TAG, "Starting a new flush executor");

                        flushExecutor.submit(() -> {
                            try {
                                if (datasource == null) {
                                    return;
                                }

                                while (!memoryStorage.isEmpty()) {
                                    datasource.addEvent(memoryStorage.poll());
                                }

                                // Tracker was stopped while we were flushing, release the database.
                                RuntimeManagerProvider.get().runIf(State.OFF, TrackerModule.this::closeDatasource);

                                if (sender != null) {
                                    sender.hasNewEvents();
                                }
                            } catch (Exception e) {
                                Logger.internal(TAG, "Exception while flushing", e);
                            } finally {
                                isFlushing.set(false);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public void onEventsSendSuccess(final List<Event> events) {
        Logger.internal(TAG, "onEventsSendSuccess");

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state != State.OFF) {
                    List<String> ids = new ArrayList<>();

                    for (Event event : events) {
                        ids.add(event.getId());
                    }
                    String[] idsArray = new String[ids.size()];
                    datasource.deleteEvents(ids.toArray(idsArray));

                    if (events.size() == batchSendQuantity) { // If we just sent the max number of event in a raw, there's probably more
                        sender.hasNewEvents();
                    }
                }
            });
    }

    @Override
    public void onEventsSendFailure(final List<Event> events) {
        Logger.internal(TAG, "onEventsSendFailure");

        RuntimeManagerProvider
            .get()
            .run(state -> {
                if (state != State.OFF) {
                    List<String> newIds = new ArrayList<>();
                    List<String> oldIds = new ArrayList<>();

                    for (Event event : events) {
                        if (!event.isOld()) {
                            newIds.add(event.getId());
                        } else {
                            oldIds.add(event.getId());
                        }
                    }

                    String[] newIdsArray = new String[newIds.size()];
                    if (!newIds.isEmpty()) {
                        datasource.updateEventsToNew(newIds.toArray(newIdsArray));
                    }

                    String[] oldIdsArray = new String[oldIds.size()];
                    if (!oldIds.isEmpty()) {
                        datasource.updateEventsToOld(oldIds.toArray(oldIdsArray));
                    }
                }
            });
    }

    @Override
    public List<Event> getEventsToSend() {
        final List<Event> events = new ArrayList<>();

        RuntimeManagerProvider.get().runIfReady(() -> events.addAll(datasource.extractEventsToSend(batchSendQuantity)));

        return events;
    }

    // -------------------------------------------->

    public void wipeData(Context context) {
        final Context c = context.getApplicationContext();

        flushExecutor.submit(() -> {
            try {
                memoryStorage.clear();
                if (datasource != null) {
                    datasource.clearDB();
                } else {
                    TrackerDatasource source = new TrackerDatasource(c);
                    source.clearDB();
                    source.close();
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Could not clear all pending events", e);
            }
        });
    }
}
