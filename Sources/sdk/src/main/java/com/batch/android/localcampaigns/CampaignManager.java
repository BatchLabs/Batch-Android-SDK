package com.batch.android.localcampaigns;

import static com.batch.android.localcampaigns.model.LocalCampaign.SyncedJITResult;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.LoggerLevel;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.DateProvider;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.core.SystemDateProvider;
import com.batch.android.core.Webservice;
import com.batch.android.date.BatchDate;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.DayOfWeek;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.persistence.LocalCampaignsFilePersistence;
import com.batch.android.localcampaigns.persistence.LocalCampaignsPersistence;
import com.batch.android.localcampaigns.persistence.PersistenceException;
import com.batch.android.localcampaigns.signal.Signal;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import com.batch.android.query.serialization.serializers.LocalCampaignsResponseSerializer;
import com.batch.android.webservice.listener.LocalCampaignsJITWebserviceListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    /**
     * Delay to wait before calling the jit webservice again after a fail
     */
    private static final int DEFAULT_RETRY_AFTER = 60_000; //ms

    /**
     * Delay before deleting local campaigns cache if there is no update from server. (15 days)
     */
    private static final long CACHE_EXPIRATION_DELAY = DAYS.toMillis(15);

    /**
     * Max number of campaigns to send to the server for JIT sync.
     */
    private static final int MAX_CAMPAIGNS_JIT_THRESHOLD = 10;

    /**
     * Min delay between two JIT sync (in ms)
     */
    private static final int MIN_DELAY_BETWEEN_JIT_SYNC = 15_000;

    /**
     * Period during cached local campaign requiring a JIT sync is considered as up-to-date.
     */
    private static final int JIT_CAMPAIGN_CACHE_PERIOD = 30_000;

    /**
     * Date provider
     */
    private final DateProvider dateProvider = new SystemDateProvider();

    /**
     * View tracker
     */
    private final LocalCampaignsTracker viewTracker;

    /**
     * Local campaigns persistor
     */
    private final LocalCampaignsPersistence persistor = new LocalCampaignsFilePersistence();

    /**
     * List of campaigns
     */
    private final List<LocalCampaign> campaignList = new ArrayList<>();

    /**
     * Global in-app cappings  (MEP only)
     */
    private LocalCampaignsResponse.GlobalCappings cappings;

    /**
     * Version of the local campaigns (MEP or CEP)
     */
    @Nullable
    private LocalCampaignsResponse.Version campaignsVersion;

    private final Object campaignListLock = new Object();

    /**
     * Timestamp to wait before JIT service will be available again
     * Is set when JIT succeed or when server respond with HTTP code 429 and specify a retry-after header.
     */
    private long nextAvailableJITTimestamp;

    /**
     * Tells if we loaded at least one time the campaigns list (an empty campains list could mean that
     * we didn't load or that the result is empty)
     */
    private final AtomicBoolean campaignsLoaded = new AtomicBoolean(false);

    /**
     * Cached list of event names that can potentially triggers the display of a local campaign
     */
    private Set<String> watchedEventNames = new HashSet<>();

    /**
     * Cached list of synced JIT campaigns
     */
    @NonNull
    private final Map<String, SyncedJITResult> syncedJITCampaignsCached = new HashMap<>();

    public CampaignManager(@NonNull LocalCampaignsTracker viewTracker) {
        this.viewTracker = viewTracker;
    }

    @Provide
    public static CampaignManager provide() {
        return new CampaignManager(new LocalCampaignsTracker());
    }

    /**
     * Handle the response from the local campaigns webservice
     *
     * @param response Local campaigns response
     */
    public void handleLocalCampaignsResponse(@NonNull LocalCampaignsResponse response) {
        synchronized (this.campaignListLock) {
            this.campaignsVersion = response.getVersion();
            this.cappings = response.getCappings();
            updateCampaignList(response.getCampaigns(), true);
        }
    }

    /**
     * Update the currently stored campaign list. Clear the previous list.
     * Calling this will triggers campaigns that have seen their conditions met.
     *
     * @param updatedCampaignList Updated campaign list. Can't be null
     * @param upToDate Whether the campaigns are up-to-date (meaning just been sync from server) or not
     */
    public void updateCampaignList(@NonNull List<LocalCampaign> updatedCampaignList, boolean upToDate) {
        synchronized (this.campaignListLock) {
            this.campaignList.clear();
            this.campaignList.addAll(cleanCampaignList(updatedCampaignList));

            if (upToDate) {
                List<String> ids = new ArrayList<>();
                for (LocalCampaign campaign : this.campaignList) {
                    ids.add(campaign.id);
                }
                updateSyncedJITCampaignsCached(this.campaignList, ids, false);
            }

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

                if (watchedEventNames.isEmpty()) {
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

    public interface JITElectionCampaignListener {
        void onCampaignElected(@Nullable LocalCampaign electedCampaign);
    }

    /**
     * Get all campaign between all of those that are satisfied by the latest application event
     * and sort them by priority
     * This is the campaign that you'll want to display
     */
    @NonNull
    public List<LocalCampaign> getEligibleCampaignsSortedByPriority(@NonNull Signal signal) {
        synchronized (this.campaignListLock) {
            List<LocalCampaign> eligibleCampaigns = new ArrayList<>();

            // Getting campaign eligible for the given signal
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

            // Sorting eligible campaigns by server priority
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
            return eligibleCampaigns;
        }
    }

    /**
     * Get eligible campaigns requiring a JIT sync
     * @param eligibleCampaigns regardless of the JIT sync
     * @return all eligible campaigns requiring a JIT sync (max: {@link CampaignManager#MAX_CAMPAIGNS_JIT_THRESHOLD})
     */
    @NonNull
    public List<LocalCampaign> getFirstEligibleCampaignsRequiringSync(List<LocalCampaign> eligibleCampaigns) {
        List<LocalCampaign> eligibleCampaignsRequiringSync = new ArrayList<>();
        int i = 0;
        for (LocalCampaign campaign : eligibleCampaigns) {
            if (i >= MAX_CAMPAIGNS_JIT_THRESHOLD) {
                break;
            }
            if (campaign.requiresJustInTimeSync) {
                eligibleCampaignsRequiringSync.add(campaign);
            } else {
                break;
            }
            i++;
        }
        return eligibleCampaignsRequiringSync;
    }

    /**
     * Get the first eligible campaign not requiring a JIT sync
     * @param eligibleCampaigns regardless of the JIT sync
     * @return the first eligible campaign not requiring a JIT sync
     */
    @Nullable
    public LocalCampaign getFirstCampaignNotRequiringJITSync(@NonNull List<LocalCampaign> eligibleCampaigns) {
        for (LocalCampaign campaign : eligibleCampaigns) {
            if (!campaign.requiresJustInTimeSync) {
                return campaign;
            }
        }
        return null;
    }

    /**
     * Checking with server if campaigns are still eligible
     * @param eligibleCampaignsRequiringSync campaigns to check
     * @param listener callback
     */
    public void verifyCampaignsEligibilityFromServer(
        @NonNull List<LocalCampaign> eligibleCampaignsRequiringSync,
        @NonNull JITElectionCampaignListener listener
    ) {
        // Assert campaign list are not empty
        if (eligibleCampaignsRequiringSync.isEmpty()) {
            listener.onCampaignElected(null);
            return;
        }

        if (!isJITServiceAvailable()) {
            listener.onCampaignElected(null);
            return;
        }

        WebserviceLauncher.launchLocalCampaignsJITWebservice(
            RuntimeManagerProvider.get(),
            eligibleCampaignsRequiringSync,
            campaignsVersion,
            new LocalCampaignsJITWebserviceListener() {
                @Override
                public void onSuccess(@NonNull List<String> eligibleCampaignIds) {
                    // Saving next jit available timestamp
                    setNextAvailableJITTimestampWithDefaultDelay();

                    // Handling jit response
                    if (eligibleCampaignIds.isEmpty()) {
                        listener.onCampaignElected(null);
                    } else {
                        updateSyncedJITCampaignsCached(eligibleCampaignsRequiringSync, eligibleCampaignIds, true);
                        if (eligibleCampaignsRequiringSync.isEmpty()) {
                            listener.onCampaignElected(null);
                        } else {
                            listener.onCampaignElected(eligibleCampaignsRequiringSync.get(0));
                        }
                    }
                }

                @Override
                public void onFailure(@Nullable Webservice.WebserviceError error) {
                    if (error != null) {
                        // Saving next jit available timestamp
                        setNextAvailableJITTimestampWithCustomDelay(error.getRetryAfterInMillis());
                    }
                    listener.onCampaignElected(null);
                }
            }
        );
    }

    @VisibleForTesting
    @NonNull
    protected Map<String, SyncedJITResult> getSyncedJITCampaignsCached() {
        synchronized (syncedJITCampaignsCached) {
            return syncedJITCampaignsCached;
        }
    }

    /**
     * Update the synced JIT campaigns cache after a LocalCampaign WS Sync or a JIT sync
     *
     * @param syncedCampaigns The synced JIT campaigns
     * @param eligibleCampaignIds The eligible campaign ids
     * @param removeNonEligibleCampaigns Whether to remove non-eligible campaigns
     */
    @VisibleForTesting
    protected void updateSyncedJITCampaignsCached(
        @NonNull List<LocalCampaign> syncedCampaigns,
        @NonNull List<String> eligibleCampaignIds,
        boolean removeNonEligibleCampaigns
    ) {
        synchronized (syncedJITCampaignsCached) {
            Iterator<LocalCampaign> iterator = syncedCampaigns.iterator();
            long now = dateProvider.getCurrentDate().getTime();
            while (iterator.hasNext()) {
                LocalCampaign campaign = iterator.next();
                if (campaign.requiresJustInTimeSync) {
                    boolean eligible = eligibleCampaignIds.contains(campaign.id);
                    if (!eligible && removeNonEligibleCampaigns) {
                        iterator.remove();
                    }
                    syncedJITCampaignsCached.put(campaign.id, new SyncedJITResult(now, eligible));
                }
            }
        }
    }

    /**
     * Clear the synced JIT campaigns cache
     */
    public void clearSyncedJITCampaignsCached() {
        synchronized (syncedJITCampaignsCached) {
            syncedJITCampaignsCached.clear();
        }
    }

    /**
     * Check if JIT sync is available
     * <p>
     * Meaning MIN_DELAY_BETWEEN_JIT_SYNC or last 'retryAfter' time respond by server is passed.
     * @return true if JIT service is available
     */
    public synchronized boolean isJITServiceAvailable() {
        return dateProvider.getCurrentDate().getTime() >= nextAvailableJITTimestamp;
    }

    /**
     * Check if the given campaign has been already synced recently
     * @param campaign to check
     * @return a {@link SyncedJITResult.State}
     */
    public SyncedJITResult.State getSyncedJITCampaignState(LocalCampaign campaign) {
        synchronized (syncedJITCampaignsCached) {
            if (!campaign.requiresJustInTimeSync) {
                //Should not happen but ensure we do not sync for a non-jit campaign
                return SyncedJITResult.State.ELIGIBLE;
            }

            if (!syncedJITCampaignsCached.containsKey(campaign.id)) {
                return SyncedJITResult.State.REQUIRES_SYNC;
            }

            SyncedJITResult syncedJITResult = syncedJITCampaignsCached.get(campaign.id);
            if (syncedJITResult == null) {
                return SyncedJITResult.State.REQUIRES_SYNC;
            }

            if (dateProvider.getCurrentDate().getTime() >= (syncedJITResult.timestamp + JIT_CAMPAIGN_CACHE_PERIOD)) {
                return SyncedJITResult.State.REQUIRES_SYNC;
            }
            return syncedJITResult.eligible ? SyncedJITResult.State.ELIGIBLE : SyncedJITResult.State.NOT_ELIGIBLE;
        }
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
     * Get the global in-app cappings
     *
     * @return The global in-app cappings
     */
    @Nullable
    public LocalCampaignsResponse.GlobalCappings getCappings() {
        synchronized (this.campaignListLock) {
            return cappings;
        }
    }

    /**
     * Get the campaigns version
     *
     * @return The campaigns version
     */
    @Nullable
    public LocalCampaignsResponse.Version getCampaignsVersion() {
        synchronized (this.campaignListLock) {
            return campaignsVersion;
        }
    }

    /**
     * Removes campaign that will never be ok, even in the future:
     * - Expired campaigns
     * - Campaigns that hit their capping
     * - Campaigns that have a max api level too low (min api level doesn't not mean that it is busted forever)
     */
    @VisibleForTesting
    @NonNull
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
            if (
                campaign.maximumAPILevel != null &&
                campaign.maximumAPILevel > 0 &&
                Parameters.MESSAGING_API_LEVEL > campaign.maximumAPILevel
            ) {
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
        ViewTracker.CountedViewEvent ev;
        if (campaignsVersion == LocalCampaignsResponse.Version.CEP) {
            Context context = RuntimeManagerProvider.get().getContext();
            if (context == null) {
                return false;
            }
            String customUserId = UserModuleProvider.get().getCustomID(context);
            ev = viewTracker.getViewEventByCampaignIdAndCustomId(campaign.id, customUserId);
        } else {
            ev = viewTracker.getViewEventByCampaignId(campaign.id);
        }

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
        if (
            campaign.maximumAPILevel != null &&
            campaign.maximumAPILevel > 0 &&
            Parameters.MESSAGING_API_LEVEL > campaign.maximumAPILevel
        ) {
            Logger.internal(TAG, "Campaign " + campaign.id + " is over max API level");
            return false;
        }

        // Exclude campaigns that have a too high min api level
        if (campaign.minimumAPILevel != null && Parameters.MESSAGING_API_LEVEL < campaign.minimumAPILevel) {
            Logger.internal(TAG, "Campaign " + campaign.id + " has a minimum API level too high");
            return false;
        }

        if (campaign.quietHours != null && isCampaignWithinQuietHours(campaign)) {
            Logger.internal(TAG, "Campaign " + campaign.id + " is within quiet hours");
            return false;
        }

        return true;
    }

    /**
     * Check if Global Cappings has been reached
     * @return true if cappings are reached
     */
    public boolean isOverGlobalCappings() {
        if (cappings == null) {
            // No cappings
            return false;
        }

        if (cappings.getSession() != null && viewTracker.getSessionViewsCount() >= cappings.getSession()) {
            Logger.internal(TAG, "Session capping has been reached");
            return true;
        }

        List<LocalCampaignsResponse.GlobalCappings.TimeBasedCapping> timeBasedCappings = cappings.getTimeBasedCappings();
        if (timeBasedCappings != null) {
            for (LocalCampaignsResponse.GlobalCappings.TimeBasedCapping timeBasedCapping : timeBasedCappings) {
                if (timeBasedCapping.getDuration() != null && timeBasedCapping.getViews() != null) {
                    long timestamp = dateProvider.getCurrentDate().getTime() - (timeBasedCapping.getDuration() * 1000);
                    try {
                        if (viewTracker.getNumberOfViewEventsSince(timestamp) >= timeBasedCapping.getViews()) {
                            Logger.internal(TAG, "Time-based cappings have been reached");
                            return true;
                        }
                    } catch (ViewTrackerUnavailableException e) {
                        Logger.internal(
                            TAG,
                            "View tracker is unavailable. Campaigns will be prevented from displaying."
                        );
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if the campaign is within quiet hours
     * @param campaign to check
     * @return true if the campaign is within quiet hours
     */
    @VisibleForTesting
    protected boolean isCampaignWithinQuietHours(@NonNull LocalCampaign campaign) {
        if (campaign.quietHours == null) {
            return false;
        }
        // Init date and time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(dateProvider.getCurrentDate().getTime()));

        // If the current day is designated as a quiet day, then the entire day is quiet.
        DayOfWeek currentDay = DayOfWeek.fromCalendar(calendar);
        boolean isTodayAQuietDay =
            campaign.quietHours.getDaysOfWeek() != null && campaign.quietHours.getDaysOfWeek().contains(currentDay);
        if (isTodayAQuietDay) {
            return true;
        }

        // --- Check for Time-Based Quiet Hours ---
        // If we've reached this point, it's not a full quiet day. Now check the specific time range.
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // To make comparisons easier, convert all times to total minutes from midnight.
        int currentTimeInMinutes = (currentHour * 60) + currentMinute;
        int startTimeInMinutes = (campaign.quietHours.getStartHour() * 60) + campaign.quietHours.getStartMinute();
        int endTimeInMinutes = (campaign.quietHours.getEndHour() * 60) + campaign.quietHours.getEndMinute();

        // This logic handles two scenarios for the quiet hours interval:
        // 1. Overnight (e.g., 22:00 to 07:00), where start time is greater than end time.
        // 2. Same-day (e.g., 09:00 to 17:00), where start time is less than or equal to end time.

        // Determine if the quiet period is overnight
        boolean isOvernight = startTimeInMinutes > endTimeInMinutes;

        if (isOvernight) {
            // For an overnight period, it's a quiet time if the current time is either:
            // 1. After the start time (e.g., between 22:00 and midnight).
            // OR
            // 2. Before the end time (e.g., between midnight and 07:00).
            return currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes;
        } else {
            // For a same-day period, it is quiet time if the current time is within the interval.
            return currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes;
        }
    }

    /**
     * Update the set of watched event names
     * This method is not thread safe: do not call it without some kind of lock
     */
    private void updateWatchedEventNames() {
        Set<String> newWatchedEvents = new HashSet<>();
        synchronized (this.campaignListLock) {
            for (LocalCampaign campaign : campaignList) {
                for (LocalCampaign.Trigger trigger : campaign.triggers) {
                    if (trigger instanceof EventLocalCampaignTrigger) {
                        newWatchedEvents.add(((EventLocalCampaignTrigger) trigger).name.toUpperCase(Locale.US));
                    }
                }
            }
            watchedEventNames = newWatchedEvents;
        }
    }

    public void saveCampaigns(@NonNull Context context, @NonNull LocalCampaignsResponse response) {
        try {
            LocalCampaignsResponseSerializer serializer = new LocalCampaignsResponseSerializer();
            JSONObject jsonData = new JSONObject();
            jsonData.put("campaigns_version", response.getVersion());
            jsonData.put("campaigns", serializer.serializeCampaigns(response.getCampaignsToSave()));
            jsonData.putOpt("cappings", serializer.serializeCappings(response.getCappings()));
            jsonData.putOpt("cache_date", dateProvider.getCurrentDate().getTime());
            persistor.persistData(context, jsonData, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException e) {
            Logger.internal(TAG, "Can't persist local campaigns response", e);
        } catch (JSONException e) {
            Logger.internal(TAG, "Can't serialize local campaigns response before the save operation", e);
        }
    }

    public void saveCampaignsAsync(@NonNull final Context context, @NonNull final LocalCampaignsResponse response) {
        TaskExecutorProvider.get(context).execute(() -> saveCampaigns(context, response));
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

    public void loadSavedCampaignResponse(@NonNull final Context context) {
        JSONObject campaignsRawData;
        try {
            campaignsRawData = persistor.loadData(context, PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME);
        } catch (PersistenceException | IOException e) {
            Logger.internal(TAG, "Can't load saved local campaigns", e);
            return;
        }

        if (campaignsRawData == null) {
            return;
        }

        // Ensure cache is not too old.
        Long expirationDate = campaignsRawData.reallyOptLong("cache_date", null);
        if (expirationDate != null) {
            expirationDate += CACHE_EXPIRATION_DELAY;
            if (expirationDate <= dateProvider.getCurrentDate().getTime()) {
                Logger.internal(TAG, "Local campaign cache is too old, deleting it.");
                deleteSavedCampaignsAsync(context);
                return;
            }
        }

        LocalCampaignsResponseDeserializer localCampaignResponseDeserializer = new LocalCampaignsResponseDeserializer(
            campaignsRawData
        );
        synchronized (this.campaignListLock) {
            try {
                campaignsVersion = localCampaignResponseDeserializer.deserializeVersion();
                cappings = localCampaignResponseDeserializer.deserializeCappings();
                boolean requireJITFallback = campaignsVersion == LocalCampaignsResponse.Version.CEP;
                List<LocalCampaign> campaigns = localCampaignResponseDeserializer.deserializeCampaigns(
                    requireJITFallback
                );
                updateCampaignList(campaigns, false);
            } catch (Exception ex) {
                Logger.internal(TAG, "Can't convert json to LocalCampaignsResponse : " + ex);
            }
        }
    }

    public boolean areCampaignsLoaded() {
        return campaignsLoaded.get();
    }

    public void openViewTracker() {
        //noinspection ConstantConditions
        if (viewTracker != null) {
            Context context = RuntimeManagerProvider.get().getContext();
            if (context != null && !viewTracker.isOpen()) {
                viewTracker.open(context);
            }
        }
    }

    public void closeViewTracker() {
        try {
            //noinspection ConstantConditions
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

    /**
     * Sets the next available timestamp for Just-In-Time (JIT) synchronization
     * using the default minimum delay.
     * The default minimum delay is defined by the constant
     * {@link #MIN_DELAY_BETWEEN_JIT_SYNC}.</p>
     */
    public void setNextAvailableJITTimestampWithDefaultDelay() {
        setNextAvailableJITTimestampWithCustomDelay(MIN_DELAY_BETWEEN_JIT_SYNC);
    }

    /**
     * Sets the next available timestamp for Just-In-Time (JIT) synchronization
     * using a custom delay.
     * The default minimum delay is defined by the constant
     * {@link #DEFAULT_RETRY_AFTER}.</p>
     */
    public void setNextAvailableJITTimestampWithCustomDelay(int delay) {
        long retryAfter = delay <= 0 ? DEFAULT_RETRY_AFTER : delay;
        nextAvailableJITTimestamp = dateProvider.getCurrentDate().getTime() + retryAfter;
    }
}
