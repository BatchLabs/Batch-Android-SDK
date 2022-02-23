package com.batch.android.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import com.batch.android.WebserviceLauncher;
import com.batch.android.compat.LocalBroadcastManager;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.persistence.PersistenceException;
import com.batch.android.localcampaigns.signal.CampaignsLoadedSignal;
import com.batch.android.localcampaigns.signal.EventTrackedSignal;
import com.batch.android.localcampaigns.signal.NewSessionSignal;
import com.batch.android.localcampaigns.signal.PublicEventTrackedSignal;
import com.batch.android.localcampaigns.signal.Signal;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.runtime.SessionManager;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Batch's Local Campaigns Messaging Module.
 *
 */
@Module
@Singleton
public class LocalCampaignsModule extends BatchModule {

    public static final String TAG = "LocalCampaigns";

    private CampaignManager campaignManager;
    private boolean triedToReadSavedCampaign = false;

    /**
     * Executor responsible for handling the campaign event based trigger
     */
    private ExecutorService triggerExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory());

    private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onLocalBroadcast(intent);
        }
    };

    private boolean broadcastReceiverRegistered = false;

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

    public void sendSignal(@NonNull Signal signal) {
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

        displayMessage(signal);
    }

    public void wipeData(@NonNull Context context) {
        try {
            campaignManager.deleteAllCampaigns(context, true);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Could not delete persisted campaigns", e);
        }
    }

    /**
     * Displays the campaign for the specified signal and track view using the ViewTracker
     */
    private void displayMessage(final @NonNull Signal signal) {
        triggerExecutor.submit(() -> {
            LocalCampaign campaign = campaignManager.getCampaignToDisplay(signal);
            if (campaign != null) {
                campaign.generateOccurrenceID();
                campaign.displayMessage();
            }
        });
    }

    private void onLocalBroadcast(Intent intent) {
        if (SessionManager.INTENT_NEW_SESSION.equals(intent.getAction())) {
            sendSignal(new NewSessionSignal());

            WebserviceLauncher.launchLocalCampaignsWebservice(RuntimeManagerProvider.get());
        }
    }

    @Override
    public void batchDidStart() {
        campaignManager.openViewTracker();

        if (!broadcastReceiverRegistered) {
            LocalBroadcastManager lbm = LocalBroadcastManagerProvider.getSingleton();
            if (lbm != null) {
                broadcastReceiverRegistered = true;
                IntentFilter filter = new IntentFilter();
                filter.addAction(SessionManager.INTENT_NEW_SESSION);
                lbm.registerReceiver(localBroadcastReceiver, filter);
            }
        }

        // Load saved campaigns and call webservice
        final Context context = RuntimeManagerProvider.get().getContext();
        SessionManager sessionManager = RuntimeManagerProvider.get().getSessionManager();

        if (
            context != null && sessionManager != null && sessionManager.isSessionActive() && !triedToReadSavedCampaign
        ) {
            TaskExecutorProvider
                .get(context)
                .submit(() -> {
                    // Try loading via local file
                    if (campaignManager.hasSavedCampaigns(context)) {
                        if (campaignManager.loadSavedCampaignResponse(context)) {
                            sendSignal(new CampaignsLoadedSignal());
                        }
                        triedToReadSavedCampaign = true;
                    }

                    int delay = 0;

                    try {
                        delay =
                            Integer.valueOf(
                                ParametersProvider.get(context).get(ParameterKeys.LOCAL_CAMPAIGNS_WS_INITIAL_DELAY)
                            );
                    } catch (NumberFormatException ignored) {}

                    // Try loading via webservice
                    new Timer()
                        .schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    WebserviceLauncher.launchLocalCampaignsWebservice(RuntimeManagerProvider.get());
                                }
                            },
                            delay * 1000
                        );
                });
        }
    }

    @Override
    public void batchDidStop() {
        campaignManager.closeViewTracker();
    }
}
