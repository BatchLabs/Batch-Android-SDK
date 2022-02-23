package com.batch.android.localcampaigns.trigger;

import com.batch.android.localcampaigns.model.LocalCampaign;

public class CampaignsLoadedTrigger implements LocalCampaign.Trigger {

    @Override
    public String getType() {
        return "CAMPAIGNS_LOADED";
    }
}
