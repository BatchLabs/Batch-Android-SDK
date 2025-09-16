package com.batch.android.localcampaigns.signal;

import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.module.UserModule;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class PublicEventTrackedSignalTest {

    @Test
    public void testSatisfiesTrigger() throws JSONException {
        final String eventName = "my_event";
        final String eventLabel = "my_label";
        JSONObject params = new JSONObject();
        JSONObject data = new JSONObject();
        data.put("toto", 22);
        JSONObject dataJson = new JSONObject(data);
        params.put(UserModule.PARAMETER_KEY_LABEL, eventLabel);
        params.put(UserModule.PARAMETER_KEY_ATTRIBUTES, dataJson.toString());
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
        Assert.assertFalse(signal.satisfiesTrigger(() -> null));
    }

    @Test
    public void testSatisfiesTriggerWithAttributes() throws JSONException {
        final String eventName = "my_event";
        final String eventLabel = "my_label";
        final JSONObject eventAttributes = new JSONObject(
            "{\"size.s\":\"M\",\"items_string.a\":[\"item2\",\"Item1\",\"item3\"],\"items_objects.a\":[{\"sub_string.s\":\"mysubstring\",\"sub_bool.b\":true}]}"
        );

        JSONObject parameters = new JSONObject();
        JSONObject attributes = new JSONObject();
        attributes.put("size.s", "M");
        attributes.put("items_string.a", new JSONArray(Arrays.asList("Item1", "item2", "item3", "item4,")));
        attributes.put(
            "items_objects.a",
            new JSONArray(
                Collections.singletonList(new JSONObject("{\"sub_bool.b\":true,\"sub_string.s\":mysubstring},"))
            )
        );
        parameters.put(UserModule.PARAMETER_KEY_LABEL, eventLabel);
        parameters.put(UserModule.PARAMETER_KEY_ATTRIBUTES, attributes);

        EventTrackedSignal baseEventSignal = new EventTrackedSignal(eventName, parameters);
        Signal signal = new PublicEventTrackedSignal(baseEventSignal);

        Assert.assertTrue(
            signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, eventLabel, eventAttributes))
        );

        Assert.assertFalse(
            "Must not satisfy an incorrect event name",
            signal.satisfiesTrigger(new EventLocalCampaignTrigger("toto", eventLabel, eventAttributes))
        );

        Assert.assertFalse(
            "Must not satisfy an incorrect label name",
            signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, "toto", eventAttributes))
        );

        attributes.put("items_string.a", new JSONArray(Arrays.asList("item2", "item3", "item4,")));
        Assert.assertFalse(
            "Must not satisfy an incorrect attributes (missing one item in list)",
            signal.satisfiesTrigger(new EventLocalCampaignTrigger(eventName, eventLabel, eventAttributes))
        );

        Assert.assertFalse(signal.satisfiesTrigger(new NextSessionTrigger()));
        Assert.assertFalse(signal.satisfiesTrigger(() -> null));
    }
}
