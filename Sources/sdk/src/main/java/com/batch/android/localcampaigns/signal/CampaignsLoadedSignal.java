package com.batch.android.localcampaigns.signal;

import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.CampaignsLoadedTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.localcampaigns.trigger.NowTrigger;

/**
 * Event that occurs when the in-app campaign list has been updated from any source
 * <p>
 * It actually matches the empty trigger, which shows campaigns when the list is loaded
 * (be it at the first SDK start from disk, or when we get an answer from the backend)
 */

public class CampaignsLoadedSignal implements Signal {

  public boolean satisfiesTrigger(LocalCampaign.Trigger trigger) {
    return (
      trigger instanceof NowTrigger ||
      trigger instanceof CampaignsLoadedTrigger ||
      trigger instanceof NextSessionTrigger
    );
  }
}
