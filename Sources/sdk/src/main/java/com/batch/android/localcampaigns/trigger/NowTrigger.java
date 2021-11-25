package com.batch.android.localcampaigns.trigger;

import com.batch.android.localcampaigns.model.LocalCampaign;

/**
 * Trigger displaying campaigns as soon as possible
 */

public class NowTrigger implements LocalCampaign.Trigger
{
    @Override
    public String getType()
    {
        return "NOW";
    }
}
