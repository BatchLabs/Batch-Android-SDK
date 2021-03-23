package com.batch.android.localcampaigns.signal;

import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.CampaignsLoadedTrigger;
import com.batch.android.localcampaigns.trigger.CampaignsRefreshedTrigger;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.localcampaigns.trigger.NowTrigger;

import org.junit.Assert;
import org.junit.Test;

public class NewSessionSignalTest
{
    @Test
    public void testSatisfiesTrigger()
    {
        Signal signal = new NewSessionSignal();

        Assert.assertTrue(signal.satisfiesTrigger(new NextSessionTrigger()));

        Assert.assertFalse(signal.satisfiesTrigger(new CampaignsRefreshedTrigger()));
        Assert.assertFalse(signal.satisfiesTrigger(new CampaignsLoadedTrigger()));
        Assert.assertFalse(signal.satisfiesTrigger(new NowTrigger()));
        Assert.assertFalse(signal.satisfiesTrigger(new EventLocalCampaignTrigger("eventname",
                null)));
        Assert.assertFalse(signal.satisfiesTrigger(new LocalCampaign.Trigger() {}));
    }
}
