package com.batch.android.localcampaigns.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.date.BatchDate;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an In-App messaging campaign
 */
public class LocalCampaign {

    private static final String TAG = "LocalCampaign";

    /**
     * Campaign ID, used to track views and capping
     */
    @NonNull
    public String id;

    /**
     * Minimum messaging API level
     * Optional
     * <p>
     * The minimum messaging API level (not to be confused with SDK API Level)
     */
    @Nullable
    public Integer minimumAPILevel;

    /**
     * Maximum API level
     * Optional
     * <p>
     * Quite like the minimum API level, but maximum. Useful for dealing with old SDKs.
     */
    @Nullable
    public Integer maximumAPILevel;

    /**
     * Priority
     * Optional (default = 0)
     * <p>
     * Priority score: the higher, the more likely it is to be shown to the user
     * Used as a "last resort" method to pick the most appropriate In-App campaign
     */
    public int priority = 0;

    /**
     * Campaign start date
     * <p>
     * If the device date is earlier than this date, the campaign should not be displayed
     */
    @Nullable
    public BatchDate startDate;

    /**
     * Campaign end date
     * Optional
     * <p>
     * If it is defined and the device date is later than this date, the campaign should not be displayed
     */
    @Nullable
    public BatchDate endDate;

    /**
     * "Soft" capping
     * <p>
     * Minimum time between two displays (in seconds) for this campaign
     */
    public int minimumDisplayInterval = 60;

    /**
     * "Soft" capping
     * Optional
     * <p>
     * Number of times a user can view this campaign before being uneligible
     */
    @Nullable
    public Integer capping;

    /**
     * Output
     * <p>
     * How the message should be displayed (Landing, notification)
     */
    @NonNull
    public Output output;

    /**
     *
     */
    @NonNull
    public JSONObject eventData;

    /**
     * Trigger
     * Optional
     * <p>
     * Trigger that will, well, trigger the campaign. For example: event-based trigger.
     * If not specified, the campaign should be immediately triggered once it has been retrieved.
     */
    @NonNull
    public List<Trigger> triggers = new ArrayList<>();

    /**
     * Persist
     * Optional
     * <p>
     * Whether this campaign should be persisted on disk or not.
     * Campaigns persisted on disk need to have a triggers to work.
     */
    public boolean persist = false;

    /**
     * Dashboard campaign token
     */
    public String publicToken = null;

    /**
     * Custom payload
     */
    @Nullable
    public JSONObject customPayload;

    /**
     * Flag indicating if this campaign must be verified from the server before being displayed
     */
    public boolean requiresJustInTimeSync;

    public void generateOccurrenceID() {
        try {
            eventData.put("i", Long.toString(System.currentTimeMillis()));
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not generate occurrence id in event data", e);
        }
    }

    /**
     * Represents what will triggers the display of an In-App Campaign
     */
    public interface Trigger {
        /**
         * Type of trigger, possible values :
         * <li>NOW</li>
         * <li>EVENT</li>
         * <li>NEXT_SESSION</li>
         * <li>CAMPAIGNS_LOADED</li>
         * <li>CAMPAIGNS_REFRESHED</li>
         *
         * @return type
         */
        String getType();
    }

    public void displayMessage() {
        output.displayMessage(this);
    }

    /**
     * Define how this campaign will be displayed on the screen when triggered successfully
     */
    public abstract static class Output {

        @NonNull
        public JSONObject payload;

        public Output(@NonNull JSONObject payload) {
            this.payload = payload;
        }

        /**
         * Display a local campaign message and track campaign view with the ViewTracker.
         * It's weird to give the campaign in parameter, but we need its custom payload
         *
         * @return true is the campaign was displayed with success
         */
        protected abstract boolean displayMessage(LocalCampaign campaign);
    }

    /**
     * Class used to cache the result of a LocalCampaign after a JIT sync.
     * Keep the timestamp of the sync and whether the campaign was eligible or not.
     */
    public static class SyncedJITResult {

        /**
         * Possible states for a synced JIT campaign
         */
        public enum State {
            ELIGIBLE,
            NOT_ELIGIBLE,
            REQUIRES_SYNC,
        }

        /**
         * Timestamp of the sync
         */
        public long timestamp;

        /**
         * Whether the campaign was eligible or not after the sync
         */
        public boolean eligible;

        public SyncedJITResult(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
