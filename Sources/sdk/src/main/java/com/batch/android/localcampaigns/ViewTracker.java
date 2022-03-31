package com.batch.android.localcampaigns;

import androidx.annotation.NonNull;
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
     * @param campaignID
     */
    CountedViewEvent trackViewEvent(@NonNull String campaignID) throws ViewTrackerUnavailableException;

    /**
     * Get the counted view events for a given campaign ID
     */
    @NonNull
    CountedViewEvent getViewEvent(@NonNull String campaignId) throws ViewTrackerUnavailableException;

    /**
     * Tell how many times have campaigns been seen
     *
     * @param campaignsIds A list containing the ids
     * @return Map Campaign id -> View counter
     */
    @NonNull
    Map<String, Integer> getViewCounts(@NonNull List<String> campaignsIds) throws ViewTrackerUnavailableException;

    /**
     * Track how much time has passed since the last view of a campaign
     *
     * @param campaignId
     * @return The difference, measured in milliseconds, between the last occurence
     * and midnight, January 1, 1970 UTC
     */
    long campaignLastOccurrence(@NonNull String campaignId) throws ViewTrackerUnavailableException;

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

        public int count = 0;

        public long lastOccurrence = -1;

        public CountedViewEvent(@NonNull String campaignID) {
            this.campaignID = campaignID;
        }
    }
}
