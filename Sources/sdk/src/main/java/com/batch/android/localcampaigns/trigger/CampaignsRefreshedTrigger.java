package com.batch.android.localcampaigns.trigger;

import com.batch.android.localcampaigns.model.LocalCampaign;

public class CampaignsRefreshedTrigger implements LocalCampaign.Trigger {

  @Override
  public String getType() {
    return "CAMPAIGNS_REFRESHED";
  }
}
