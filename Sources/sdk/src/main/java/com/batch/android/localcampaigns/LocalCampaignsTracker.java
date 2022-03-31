package com.batch.android.localcampaigns;

import androidx.annotation.NonNull;

public final class LocalCampaignsTracker extends LocalCampaignsSQLTracker {

    /**
     *  Count of the views tracked during the user session.
     *  This counter is reset when a new session start.
     */
    private int sessionViewsCount = 0;

    /**
     * Reset the session view count
     */
    public void resetSessionViewsCount() {
        this.sessionViewsCount = 0;
    }

    /**
     * Get the count of in-apps viewed during the session
     * @return sessionViewsCount
     */
    public int getSessionViewsCount() {
        return sessionViewsCount;
    }

    /**
     * Track
     * @param campaignID Campaign ID
     * @return
     * @throws ViewTrackerUnavailableException
     */
    @Override
    public CountedViewEvent trackViewEvent(@NonNull String campaignID) throws ViewTrackerUnavailableException {
        sessionViewsCount++;
        return super.trackViewEvent(campaignID);
    }
}
