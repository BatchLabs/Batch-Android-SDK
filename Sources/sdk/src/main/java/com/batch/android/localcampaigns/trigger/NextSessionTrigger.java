package com.batch.android.localcampaigns.trigger;

import com.batch.android.localcampaigns.model.LocalCampaign;

/**
 * Triggers when a new user session starts
 */
public class NextSessionTrigger implements LocalCampaign.Trigger {

    @Override
    public String getType() {
        return "NEXT_SESSION";
    }
}
