package com.batch.android.localcampaigns;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.batch.android.LoggerLevel;
import com.batch.android.core.DateProvider;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.date.BatchDate;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SecureDateProviderProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.persistence.LocalCampaignsFilePersistence;
import com.batch.android.localcampaigns.persistence.LocalCampaignsPersistence;
import com.batch.android.localcampaigns.persistence.PersistenceException;
import com.batch.android.localcampaigns.serialization.LocalCampaignDeserializer;
import com.batch.android.localcampaigns.serialization.LocalCampaignSerializer;
import com.batch.android.localcampaigns.signal.Signal;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import com.batch.android.query.serialization.serializers.LocalCampaignsResponseSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles many local campaigns related features:
 * - Remembers campaigns got from the backend
 * - Saves/Restores campaigns from disk
 * - Checks campaign condition satisfaction and triggers them
 * - Automatically if in auto mode (if dev didn't ask Batch to delay campaigns)
 * - Broadcasts an event/callback to the developer if in manual mode  - TODO: update once decided
 */
@Module
@Singleton
public class CampaignManager {

    private static final String TAG = "CampaignManager";
    private static final String PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME = "com.batch.localcampaigns.persist.json";

    private DateProvider dateProvider = SecureDateProviderProvider.get();

    private LocalCampaignsSQLTracker viewTracker;

    private LocalCampaignsPersistence persistor = new LocalCampaignsFilePersistence();

    private final List<LocalCampaign> campaignList = new ArrayList<>();

    private final Object campaignListLock = new Object();

    /**
     * Tells if we loaded at least one time the campaigns list (an empty campains list could mean that
     * we didn't load or that the result is empty)
     */
    private AtomicBoolean campaignsLoaded = new AtomicBoolean(false);

    /**
     * Cached list of event names that can potentially triggers the display of a local campaign
     */
    private Set<String> watchedEventNames = new HashSet<>();

    public CampaignManager(@NonNull LocalCampaignsSQLTracker viewTracker) {
        this.viewTracker = viewTracker;
    }

    @Provide
    public static CampaignManager provide() {
        return new CampaignManager(new LocalCampaignsSQLTracker());
    }

    /**
     * Update the currently stored campaign list. Clear the previous list.
     * Calling this will triggers campaigns that have seen their conditions met.
     *
     * @param updatedCampaignList Updated campaign list. Can't be null
     */
    public void updateCampaignList(@NonNull List<LocalCampaign> updatedCampaignList) {
        synchronized (this.campaignListLock) {
            this.campaignList.clear();
            this.campaignList.addAll(cleanCampaignList(updatedCampaignList));
            campaignsLoaded.set(true);

            updateWatchedEventNames();

            if (Logger.shouldLogForLevel(LoggerLevel.INTERNAL)) {
                Logger.internal(TAG, "Loaded " + this.campaignList.size() + " campaign(s)");
                String publicToken;
                for (LocalCampaign localCampaign : this.campaignList) {
                    publicToken = localCampaign.publicToken;
                    if (publicToken != null) {
                        Logger.internal(TAG, publicToken);
                    } else {
                        Logger.internal(TAG, "Unknown ( " + localCampaign.id + " )");
                    }
                }

                if (watchedEventNames.size() == 0) {
                    Logger.internal(TAG, "No events to watch");
                } else {
                    Logger.internal(TAG, "Watching events: ");
                    for (String watchedEvent : watchedEventNames) {
                        Logger.internal(TAG, watchedEvent);
                    }
                }
            }
        }
    }

    /**
     * Delete all campaigns
     */
    public void deleteAllCampaigns(Context context, boolean persist) throws PersistenceException {
        synchronized (this.campaignListLock) {
            this.campaignList.clear();
            this.watchedEventNames.clear();
            campaignsLoaded.set(false);

            if (persist) {
                persistor.deleteData(context, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
            }
        }
    }

    /**
     * Get the higher priority campaign between all of those that are satisfied by the latest application event
     * This is the campaign that you'll want to display
     */
    public LocalCampaign getCampaignToDisplay(@NonNull Signal signal) {
        synchronized (this.campaignListLock) {
            List<LocalCampaign> eligibleCampaigns = new ArrayList<>();
            for (LocalCampaign campaign : campaignList) {
                boolean satisfiesTrigger = false;
                for (LocalCampaign.Trigger trigger : campaign.triggers) {
                    if (trigger != null && signal.satisfiesTrigger(trigger)) {
                        satisfiesTrigger = true;
                        break;
                    }
                }

                if (!satisfiesTrigger) {
                    continue;
                }

                if (!isCampaignDisplayable(campaign)) {
                    continue;
                }

                eligibleCampaigns.add(campaign);
            }
            Collections.sort(
                eligibleCampaigns,
                Collections.reverseOrder((o1, o2) -> {
                    int x = o1.priority;
                    int y = o2.priority;
                    //Suppress the inspection as it's not available on API 15 (IntelliJ believes it is though)
                    //noinspection UseCompareMethod
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                })
            );

            if (eligibleCampaigns.size() > 0) {
                return eligibleCampaigns.get(0);
            } else {
                Logger.internal(TAG, "No eligible campaign was found");
            }
        }

        return null;
    }

    /**
     * Checks if an event name will triggers at least one campaign, allowing for a fast pre-filter to check if it is worth
     * checking other conditions for campaigns with an event triggers
     */
    public boolean isEventWatched(@NonNull String name) {
        return watchedEventNames.contains(name.toUpperCase(Locale.US));
    }

    /**
     * Returns a copy of the loaded campaigns
     */
    public List<LocalCampaign> getCampaignList() {
        return new ArrayList<>(campaignList);
    }

    /**
     * Removes campaign that will never be ok, even in the future:
     * - Expired campaigns
     * - Campaigns that hit their capping
     * - Campaigns that have a max api level too low (min api level doesn't not mean that it is busted forever)
     */
    @VisibleForTesting
    public List<LocalCampaign> cleanCampaignList(@NonNull List<LocalCampaign> campaignsToClean) {
        final BatchDate currentDate = dateProvider.getCurrentDate();

        final List<LocalCampaign> cleanedCampaignList = new ArrayList<>();

        for (LocalCampaign campaign : campaignsToClean) {
            // Exclude campaigns that are over
            if (campaign.endDate != null && campaign.endDate.compareTo(currentDate) < 0) {
                Logger.internal(TAG, "Ignoring campaign " + campaign.id + " since it is past its end_date");
                continue;
            }

            try {
                // Exclude campaigns that are over the view capping
                if (isCampaignOverCapping(campaign, true)) {
                    Logger.internal(TAG, "Campaign " + campaign.id + " is over capping.");
                    continue;
                }
            } catch (ViewTrackerUnavailableException e) {
                Logger.internal(
                    TAG,
                    "View tracker is unavailable, campaign " + campaign.id + " capping can't be evaluated."
                );
            }

            // Exclude campaigns that are over the max api level
            if (campaign.maximumAPILevel != null && Parameters.MESSAGING_API_LEVEL > campaign.maximumAPILevel) {
                Logger.internal(TAG, "Campaign " + campaign.id + " is over max API level");
                continue;
            }

            cleanedCampaignList.add(campaign);
        }

        return cleanedCampaignList;
    }

    /**
     * Checks if a campaign is over its global capping.
     */
    @VisibleForTesting
    protected boolean isCampaignOverCapping(LocalCampaign campaign, boolean ignoreMinInterval)
        throws ViewTrackerUnavailableException {
        ViewTracker.CountedViewEvent ev = viewTracker.getViewEvent(campaign.id);

        if (campaign.capping != null && campaign.capping > 0) {
            if (ev.count >= campaign.capping) {
                return true;
            }
        }

        if (
            !ignoreMinInterval &&
            campaign.minimumDisplayInterval > 0 &&
            dateProvider.getCurrentDate().getTime() <=
            (ev.lastOccurrence + SECONDS.toMillis(campaign.minimumDisplayInterval))
        ) {
            Logger.internal(TAG, "Campaign's minimum display interval has not been reached");
            return true;
        }

        return false;
    }

    /**
     * Checks if the campaign is displayable according to general conditions:
     * - Capping checks
     * - Current date over start date
     * - Minimum API level
     * - etc...
     */
    @VisibleForTesting
    protected boolean isCampaignDisplayable(LocalCampaign campaign) {
        BatchDate currentDate = dateProvider.getCurrentDate();

        // Exclude campaigns that have not begun yet
        if (campaign.startDate != null && campaign.startDate.compareTo(currentDate) > 0) {
            Logger.internal(TAG, "Ignoring campaign " + campaign.id + " since it has not begun yet");
            return false;
        }

        // Exclude campaigns that are over
        if (campaign.endDate != null && campaign.endDate.compareTo(currentDate) < 0) {
            Logger.internal(TAG, "Ignoring campaign " + campaign.id + " since it is past its end_date");
            return false;
        }

        // Exclude campaigns that are over the view capping
        try {
            if (isCampaignOverCapping(campaign, false)) {
                Logger.internal(TAG, "Campaign " + campaign.id + " is over capping/minimum display interval.");
                return false;
            }
        } catch (ViewTrackerUnavailableException e) {
            Logger.internal(
                TAG,
                "View tracker is unavailable. Campaign " + campaign.id + " will be prevented from displaying."
            );
            return false;
        }

        // Exclude campaigns that are over the max api level
        if (campaign.maximumAPILevel != null && Parameters.MESSAGING_API_LEVEL > campaign.maximumAPILevel) {
            Logger.internal(TAG, "Campaign " + campaign.id + " is over max API level");
            return false;
        }

        // Exclude campaigns that have a too high min api level
        if (campaign.minimumAPILevel != null && Parameters.MESSAGING_API_LEVEL < campaign.minimumAPILevel) {
            Logger.internal(TAG, "Campaign " + campaign.id + " has a minimum API level too high");
            return false;
        }

        return true;
    }

    /**
     * Update the set of watched event names
     * This method is not thread safe: do not call it without some kind of lock
     */
    private void updateWatchedEventNames() {
        Set<String> newWatchedEvents = new HashSet<>();
        for (LocalCampaign campaign : campaignList) {
            for (LocalCampaign.Trigger trigger : campaign.triggers) {
                if (trigger != null && trigger instanceof EventLocalCampaignTrigger) {
                    newWatchedEvents.add(((EventLocalCampaignTrigger) trigger).name.toUpperCase(Locale.US));
                }
            }
        }
        watchedEventNames = newWatchedEvents;
    }

    public void saveCampaigns(@NonNull Context context, @NonNull List<LocalCampaign> campaigns) {
        try {
            LocalCampaignSerializer serializer = new LocalCampaignSerializer();
            JSONObject jsonData = new JSONObject();
            jsonData.put("campaigns", serializer.serializeList(campaigns));
            persistor.persistData(context, jsonData, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Can't persist local campaigns", e);
        } catch (JSONException e) {
            Logger.internal(TAG, "Can't serialize local campaigns before the save operation", e);
            e.printStackTrace();
        }
    }

    public void saveCampaignsAsync(@NonNull final Context context, @NonNull final List<LocalCampaign> campaigns) {
        TaskExecutorProvider.get(context).execute(() -> saveCampaigns(context, campaigns));
    }

    public void deleteSavedCampaigns(@NonNull final Context context) {
        try {
            persistor.deleteData(context, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Can't delete local campaigns", e);
        }
    }

    public void deleteSavedCampaignsAsync(@NonNull final Context context) {
        TaskExecutorProvider.get(context).execute(() -> deleteSavedCampaigns(context));
    }

    public boolean hasSavedCampaigns(Context context) {
        try {
            return persistor.hasSavedData(context, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Can't determine if there is saved local campaigns", e);
            return false;
        }
    }

    public boolean loadSavedCampaignResponse(@NonNull final Context context) {
        JSONObject campaignsRawData;
        try {
            campaignsRawData = persistor.loadData(context, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Can't load saved local campaigns", e);
            return false;
        }

        if (campaignsRawData == null) {
            return false;
        }

        LocalCampaignDeserializer localCampaignDeserializer = new LocalCampaignDeserializer();
        try {
            JSONArray jsonCampaigns = campaignsRawData.getJSONArray("campaigns");
            List<LocalCampaign> campaigns = localCampaignDeserializer.deserializeList(jsonCampaigns);
            updateCampaignList(campaigns);
        } catch (Exception ex) {
            Logger.internal(TAG, "Can't convert json to LocalCampaignsResponse : " + ex.toString());
            return false;
        }
        return true;
    }

    public boolean areCampaignsLoaded() {
        return campaignsLoaded.get();
    }

    public void openViewTracker() {
        if (viewTracker != null) {
            Context context = RuntimeManagerProvider.get().getContext();
            if (context != null && !viewTracker.isOpen()) {
                viewTracker.open(context);
            } else {
                // BatchModule#batchDidStart javadoc lied
            }
        }
    }

    public void closeViewTracker() {
        try {
            if (viewTracker != null && viewTracker.isOpen()) {
                viewTracker.close();
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while closing DB", e);
        }
    }

    public ViewTracker getViewTracker() {
        return viewTracker;
    }
}
