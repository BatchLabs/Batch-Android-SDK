package com.batch.android.localcampaigns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalCampaignsTracker extends LocalCampaignsSQLTracker {

    /**
     *  Count of the views tracked during the user session.
     *  This counter is reset when a new session start.
     */
    private final AtomicInteger sessionViewsCount = new AtomicInteger(0);

    /**
     * Reset the session view count
     */
    public void resetSessionViewsCount() {
        sessionViewsCount.set(0);
    }

    /**
     * Get the count of in-apps viewed during the session
     * @return sessionViewsCount
     */
    public int getSessionViewsCount() {
        return sessionViewsCount.get();
    }

    /**
     * Track
     * @param campaignID Campaign ID
     * @return
     * @throws ViewTrackerUnavailableException
     */
    @Override
    public CountedViewEvent trackViewEvent(@NonNull String campaignID, @Nullable String customUserId)
        throws ViewTrackerUnavailableException {
        CountedViewEvent ev = super.trackViewEvent(campaignID, customUserId);
        sessionViewsCount.incrementAndGet();
        return ev;
    }
}
