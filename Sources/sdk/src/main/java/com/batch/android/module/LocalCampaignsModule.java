package com.batch.android.module;

import static com.batch.android.localcampaigns.model.LocalCampaign.SyncedJITResult;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import com.batch.android.WebserviceLauncher;
import com.batch.android.compat.LocalBroadcastManager;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.persistence.PersistenceException;
import com.batch.android.localcampaigns.signal.EventTrackedSignal;
import com.batch.android.localcampaigns.signal.NewSessionSignal;
import com.batch.android.localcampaigns.signal.PublicEventTrackedSignal;
import com.batch.android.localcampaigns.signal.Signal;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.runtime.SessionManager;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Batch's Local Campaigns Messaging Module.
 *
 */
@Module
@Singleton
public class LocalCampaignsModule extends BatchModule {

    public static final String TAG = "LocalCampaigns";

    /**
     * Campaign manager instance
     */
    private final CampaignManager campaignManager;

    /**
     * Flag indicating whether we already tried to load the campaigns in cache
     */
    private boolean triedToReadSavedCampaign = false;

    /**
     * Signals in queue
     */
    private final LinkedList<Signal> signalQueue = new LinkedList<>();

    /**
     * Flag indicating whether we are ready to process a signal.
     *
     * Meaning local campaigns have been synchronized from server, else signals are enqueue.
     * This flag shouldn't be true when campaigns are loaded from cache, only after
     * a synchronisation with the server.
     */
    private final AtomicBoolean isReady = new AtomicBoolean(false);

    /**
     * Flag indicating whether we are waiting for the end of JIT synchronization.
     * All signal processed during this time will be enqueue until the end.
     */
    private final AtomicBoolean isWaitingJITSync = new AtomicBoolean(false);

    /**
     * Executor responsible for handling the campaign event based trigger
     */
    private final ExecutorService triggerExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory());

    /**
     * Flag indicating whether the new session broadcast receiver is registered
     */
    private boolean isNewSessionBroadcastReceiverRegistered = false;

    private LocalCampaignsModule(CampaignManager campaignManager) {
        this.campaignManager = campaignManager;
    }

    @Provide
    public static LocalCampaignsModule provide() {
        return new LocalCampaignsModule(CampaignManagerProvider.get());
    }

    //region: BatchModule methods

    @Override
    public String getId() {
        return "localcampaigns";
    }

    @Override
    public int getState() {
        return 1;
    }

    //endregion

    /**
     * Start sending a signal
     * If another one is already processing, signal is added to queue.
     * @param signal signal to send
     */
    public void sendSignal(@NonNull Signal signal) {
        if (isReady.get()) {
            processSignal(signal);
        } else {
            enqueueSignal(signal);
        }
    }

    /**
     * Add a signal to the queue
     * @param signal signal to enqueue
     */
    private void enqueueSignal(@NonNull Signal signal) {
        synchronized (signalQueue) {
            // Ensure we are still processing the signal or synchronizing campaigns
            if (isReady.get() && !isWaitingJITSync.get()) {
                sendSignal(signal);
            } else {
                Logger.internal(
                    TAG,
                    "Local Campaign module isn't ready, enqueueing signal: " + signal.getClass().getSimpleName()
                );
                signalQueue.add(signal);
            }
        }
    }

    /**
     * Process a signal
     * @param signal signal to process
     */
    private void processSignal(@NonNull Signal signal) {
        if (signal instanceof EventTrackedSignal) {
            // Skip processing the signal if the event is not watched to avoid useless work
            // Otherwise, transform the signal in a more specialized one for public events,
            // if applicable.
            final EventTrackedSignal castedSignal = (EventTrackedSignal) signal;
            if (!campaignManager.isEventWatched(castedSignal.name)) {
                Logger.internal(
                    TAG,
                    "Skipping event signal processing as the event named '" + castedSignal.name + "'is not watched."
                );
                return;
            }

            if (PublicEventTrackedSignal.isPublic(castedSignal)) {
                signal = new PublicEventTrackedSignal(castedSignal);
            }
        }

        // Ensure we are not over global in-app cappings
        if (campaignManager.isOverGlobalCappings()) {
            return;
        }

        Signal finalSignal = signal;
        triggerExecutor.submit(() -> {
            if (isWaitingJITSync.get()) {
                Logger.internal(TAG, "JIT sync in progress, enqueue signal.");
                enqueueSignal(finalSignal);
            } else {
                electCampaignForSignal(finalSignal);
            }
        });
    }

    /**
     * Elect the right campaign for a given signal and display it.
     *
     *  Election process is the following :
     *  - Get all eligible campaigns sorted by priority for a signal:
     *      - If no eligible campaigns found:
     *          Do nothing
     *      - Else: Look if the first one is requiring a JIT sync :
     *          - Yes: Check if we need to make a new JIT sync (meaning last call for this campaign is older than {@link CampaignManager#JIT_CAMPAIGN_CACHE_PERIOD})
     *              - Yes: Check if JIT service is available :
     *                  - Yes: Sync all campaigns requiring a JIT sync limited by {@link CampaignManager#MAX_CAMPAIGNS_JIT_THRESHOLD}) and stopping at the first campaign that not requiring JIT:
     *                      - If server respond with no eligible campaigns :
     *                          - Display the first campaign not requiring a JIT sync (if there's one else do noting)
     *                      - else :
     *                          - Display the first campaign verified by the server
     *                  - No: Display the first campaign not requiring a JIT sync (if there's one else do noting)
     *              -No: Display it
     *          - No: Display it
     */
    private void electCampaignForSignal(final @NonNull Signal signal) {
        // Get all eligible campaigns (sorted by priority) regardless of the JIT sync
        List<LocalCampaign> eligibleCampaigns = campaignManager.getEligibleCampaignsSortedByPriority(signal);

        if (!eligibleCampaigns.isEmpty()) {
            // Get the first elected campaign
            LocalCampaign firstElectedCampaign = eligibleCampaigns.get(0);
            if (firstElectedCampaign.requiresJustInTimeSync && signal instanceof EventTrackedSignal) {
                SyncedJITResult.State syncedCampaignState = campaignManager.getSyncedJITCampaignState(
                    firstElectedCampaign
                );
                if (syncedCampaignState == SyncedJITResult.State.ELIGIBLE) {
                    // Last succeed JIT sync for this campaign is NOT older than 30 sec, considering eligibility up to date.
                    Logger.internal(TAG, "Skipping JIT sync since this campaign has been already synced recently.");
                    displayMessage(firstElectedCampaign);
                } else if (
                    syncedCampaignState == SyncedJITResult.State.REQUIRES_SYNC &&
                    campaignManager.isJITServiceAvailable()
                ) {
                    // JIT available, getting all campaigns to sync
                    List<LocalCampaign> eligibleCampaignsRequiringSync = campaignManager.getFirstEligibleCampaignsRequiringSync(
                        eligibleCampaigns
                    );
                    LocalCampaign fallbackCampaign = campaignManager.getFirstCampaignNotRequiringJITSync(
                        eligibleCampaigns
                    );
                    isWaitingJITSync.set(true);
                    campaignManager.verifyCampaignsEligibilityFromServer(
                        eligibleCampaignsRequiringSync,
                        electedCampaign -> {
                            if (electedCampaign != null) {
                                Logger.internal(TAG, "Elected campaign has been synchronized with JIT.");
                                displayMessage(electedCampaign);
                            } else if (fallbackCampaign != null) {
                                Logger.internal(
                                    TAG,
                                    "JIT respond with no eligible campaigns or with error. Fallback on offline campaign."
                                );
                                displayMessage(fallbackCampaign);
                            } else {
                                Logger.info(TAG, "Ne eligible campaigns found after the JIT sync.");
                            }
                            isWaitingJITSync.set(false);
                            dequeueSignals();
                        }
                    );
                } else {
                    // JIT not available or campaign is cached and not eligible, fallback on the first eligible campaign not requiring a JIT sync
                    LocalCampaign firstEligibleCampaignNotRequiringJITSync = campaignManager.getFirstCampaignNotRequiringJITSync(
                        eligibleCampaigns
                    );
                    if (firstEligibleCampaignNotRequiringJITSync != null) {
                        Logger.internal(
                            TAG,
                            "JIT not available or campaign is cached and not eligible, fallback on offline campaign."
                        );
                        displayMessage(firstEligibleCampaignNotRequiringJITSync);
                    }
                }
            } else {
                // First elected campaign is not requiring a JIT sync, display it !
                Logger.internal(TAG, "Elected campaign not requiring a sync, display it.");
                displayMessage(firstElectedCampaign);
            }
        } else {
            Logger.internal(TAG, "No eligible campaigns found.");
        }
    }

    /**
     * Display the local campaign message
     * @param campaign to display
     */
    private void displayMessage(@NonNull LocalCampaign campaign) {
        campaign.generateOccurrenceID();
        campaign.displayMessage();
    }

    /**
     * Make this module ready to process signals enqueued.
     */
    private void makeReady() {
        isReady.set(true);
        dequeueSignals();
    }

    /**
     * Dequeue all signals
     */
    private void dequeueSignals() {
        synchronized (signalQueue) {
            LinkedList<Signal> enqueuedSignals = new LinkedList<>(signalQueue);
            signalQueue.clear();

            if (!enqueuedSignals.isEmpty()) {
                Logger.info(TAG, "Replaying " + enqueuedSignals.size() + " local campaign signals");
            }

            for (Signal signal : enqueuedSignals) {
                processSignal(signal);
            }
        }
    }

    /**
     * Release the signal queue when the local campaigns webservice is finished
     */
    public void onLocalCampaignsWebserviceFinished() {
        makeReady();
    }

    /**
     * Delete all campaigns from the manager
     * @param context context
     */
    public void wipeData(@NonNull Context context) {
        try {
            campaignManager.deleteAllCampaigns(context, true);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Could not delete persisted campaigns", e);
        }
    }

    /**
     * Broadcast receiver listening on new_session intent.
     */
    private final BroadcastReceiver newSessionBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SessionManager.INTENT_NEW_SESSION.equals(intent.getAction())) {
                isReady.set(false);
                sendSignal(new NewSessionSignal());
                WebserviceLauncher.launchLocalCampaignsWebservice(RuntimeManagerProvider.get());
            }
        }
    };

    /**
     * Register the broadcast receiver for "new_session" intent if needed.
     * @param context used to instantiate the LocalBroadcastManager singleton if its not.
     */
    public void registerBroadcastReceiverIfNeeded(@NonNull Context context) {
        if (!isNewSessionBroadcastReceiverRegistered) {
            LocalBroadcastManager lbm = LocalBroadcastManagerProvider.get(context);
            isNewSessionBroadcastReceiverRegistered = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(SessionManager.INTENT_NEW_SESSION);
            lbm.registerReceiver(newSessionBroadcastReceiver, filter);
        }
    }

    /**
     * Load the saved campaigns from cache
     * @param context used to instantiate the TaskExecutor singleton if its not.
     */
    private void loadSavedCampaigns(@NonNull Context context) {
        campaignManager.openViewTracker();
        if (!triedToReadSavedCampaign) {
            TaskExecutorProvider
                .get(context)
                .submit(() -> {
                    if (campaignManager.hasSavedCampaigns(context)) {
                        campaignManager.loadSavedCampaignResponse(context);
                    }
                });
            triedToReadSavedCampaign = true;
        }
    }

    @Override
    public void batchContextBecameAvailable(@NonNull Context applicationContext) {
        registerBroadcastReceiverIfNeeded(applicationContext);
        loadSavedCampaigns(applicationContext);
    }

    @Override
    public void batchDidStop() {
        campaignManager.closeViewTracker();
    }
}
