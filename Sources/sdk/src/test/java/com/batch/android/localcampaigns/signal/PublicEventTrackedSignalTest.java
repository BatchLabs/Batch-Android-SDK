package com.batch.android.localcampaigns.signal;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.module.UserModule;
import org.junit.Assert;
import org.junit.Test;

public class PublicEventTrackedSignalTest {

    @Test
    public void testSatisfiesTrigger() throws JSONException {
        final String eventName = "my_event";
        final String eventLabel = "my_label";
        JSONObject params = new JSONObject();
        JSONObject datas = new JSONObject();
        datas.put("toto", 22);
        JSONObject dataJson = new JSONObject(datas);
        params.put(UserModule.PARAMETER_KEY_LABEL, eventLabel);
        params.put(UserModule.PARAMETER_KEY_DATA, dataJson.toString());
        EventTrackedSignal baseEventSignal = new EventTrackedSignal(eventName, params);
        Signal signal = new PublicEventTrackedSignal(baseEventSignal);

        Assert.assertTrue(signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, eventLabel)));

        Assert.assertFalse(
            "Must not satisfy an incorrect event name",
            signal.satisfiesTrigger(new EventLocalCampaignTrigger("toto", eventLabel))
        );

        Assert.assertFalse(
            "Must not satisfy an incorrect label name",
            signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, "toto"))
        );

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
