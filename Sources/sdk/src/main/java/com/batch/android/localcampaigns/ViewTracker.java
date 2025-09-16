package com.batch.android.localcampaigns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Interface that defines a Local Campaign view tracker.
 */
public interface ViewTracker {
    public static final int KIND_VIEW = 1;

    /**
     * Track a view
     *
     * @param campaignID The campaign id
     * @param customUserId The custom user id
     * @return The counted view event
     */
    CountedViewEvent trackViewEvent(@NonNull String campaignID, @Nullable String customUserId)
        throws ViewTrackerUnavailableException;

    /**
     * Get the counted view events for a given campaign ID
     *
     * @param campaignId The campaign id
     * @return The counted view event
     */
    @NonNull
    CountedViewEvent getViewEventByCampaignId(@NonNull String campaignId) throws ViewTrackerUnavailableException;

    /**
     * Get the counted view events for a given campaign ID and custom user ID
     *
     * @param campaignId The campaign id
     * @param customUserId The custom user id
     * @return The counted view event
     */
    @NonNull
    CountedViewEvent getViewEventByCampaignIdAndCustomId(@NonNull String campaignId, @Nullable String customUserId)
        throws ViewTrackerUnavailableException;

    /**
     * Tell how many times have campaigns been seen
     *
     * @param campaignsIds A list containing the ids
     * @return Map Campaign id -> View counter
     */
    @NonNull
    Map<String, Integer> getViewCountsByCampaignIds(@NonNull List<String> campaignsIds)
        throws ViewTrackerUnavailableException;

    /**
     * Tell how many times have campaigns been seen for a given custom user id
     * @param campaignsIds A list containing the ids
     * @param customUserId The custom user id
     * @return Map Campaign id -> View counter
     * @throws ViewTrackerUnavailableException exception
     */
    @NonNull
    Map<String, Integer> getViewCountsByCampaignIdsAndCustomUserId(
        @NonNull List<String> campaignsIds,
        @Nullable String customUserId
    ) throws ViewTrackerUnavailableException;

    /**
     * Track how much time has passed since the last view of a campaign
     *
     * @param campaignId The campaign id
     * @return The difference, measured in milliseconds, between the last occurence
     * and midnight, January 1, 1970 UTC
     */
    long getCampaignLastOccurrence(@NonNull String campaignId) throws ViewTrackerUnavailableException;

    /**
     * Get the number of view event tracked since a given timestamp
     * @param timestamp date (timestamp in ms)
     * @return total view events since the given date
     * @throws ViewTrackerUnavailableException exception
     */
    int getNumberOfViewEventsSince(long timestamp) throws ViewTrackerUnavailableException;

    class CountedViewEvent {

        @NonNull
        public String campaignID;

        @Nullable
        public String customUserId;

        public int count = 0;

        public long lastOccurrence = -1;

        public CountedViewEvent(@NonNull String campaignID) {
            this.campaignID = campaignID;
        }

        public CountedViewEvent(@NonNull String campaignID, @Nullable String customUserId) {
            this.campaignID = campaignID;
            this.customUserId = customUserId;
        }
    }
}
