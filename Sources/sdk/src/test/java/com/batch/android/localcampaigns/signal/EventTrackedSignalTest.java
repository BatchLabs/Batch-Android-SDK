package com.batch.android.localcampaigns.signal;

import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import org.junit.Assert;
import org.junit.Test;

public class EventTrackedSignalTest {

    @Test
    public void testSatisfiesTrigger() {
        final String eventName = "my_event";
        Signal signal = new EventTrackedSignal(eventName, null);

        Assert.assertTrue(signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, null)));

        Assert.assertFalse(signal.satisfiesTrigger(new NextSessionTrigger()));
        Assert.assertFalse(
            signal.satisfiesTrigger(
                new LocalCampaign.Trigger() {
                    @Override
                    public String getType() {
                        return null;
                    }
                }
            )
        );
    }
}
