package com.batch.android.localcampaigns.serialization;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.LocalCampaignsResponseFactory;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.output.ActionOutput;
import com.batch.android.localcampaigns.output.LandingOutput;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import com.batch.android.query.serialization.serializers.LocalCampaignsResponseSerializer;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignsResponseSerializationTest {

    private LocalCampaignsResponseFactory factory;

    @Before
    public void setUp() {
        factory = new LocalCampaignsResponseFactory();
    }

    @Test
    public void testValidSerialization() throws JSONException {
        LocalCampaignsResponse response = factory.createLocalCampaignsResponse();
        LocalCampaignsResponseSerializer serializer = new LocalCampaignsResponseSerializer();
        JSONObject serializedResponse = serializer.serialize(response);

        Assert.assertEquals(response.getQueryID(), serializedResponse.getString("id"));
        Assert.assertFalse(serializedResponse.has("minDisplayInterval"));

        // Global cappings
        LocalCampaignsResponse.GlobalCappings cappings = response.getCappings();
        JSONObject jsonGlobalCappings = serializer.serializeCappings(cappings);
        Assert.assertEquals(cappings.getSession(), Integer.valueOf(jsonGlobalCappings.getInt("session")));
        Assert.assertEquals(
            cappings.getTimeBasedCappings().get(0).getViews(),
            Integer.valueOf(jsonGlobalCappings.getJSONArray("time").getJSONObject(0).getInt("views"))
        );
        Assert.assertEquals(
            cappings.getTimeBasedCappings().get(0).getDuration(),
            Integer.valueOf(jsonGlobalCappings.getJSONArray("time").getJSONObject(0).getInt("duration"))
        );

        // Local campaigns
        Assert.assertTrue(serializedResponse.hasNonNull("campaigns"));
        LocalCampaign campaign = response.getCampaigns().get(0);
        JSONObject serializedCampaign = serializedResponse.getJSONArray("campaigns").getJSONObject(0);
        Assert.assertEquals(campaign.id, serializedCampaign.getString("campaignId"));
        Assert.assertNotNull(campaign.capping);
        Assert.assertEquals(campaign.capping, Integer.valueOf(serializedCampaign.getInt("capping")));
        Assert.assertEquals(campaign.eventData, serializedCampaign.getJSONObject("eventData"));
        Assert.assertNotNull(campaign.minimumAPILevel);
        Assert.assertEquals(campaign.minimumAPILevel, Integer.valueOf(serializedCampaign.getInt("minimumApiLevel")));
        Assert.assertNotNull(campaign.maximumAPILevel);
        Assert.assertEquals(campaign.maximumAPILevel, Integer.valueOf(serializedCampaign.getInt("maximumApiLevel")));
        Assert.assertEquals(campaign.priority, serializedCampaign.getInt("priority"));
        Assert.assertEquals(campaign.minimumDisplayInterval, serializedCampaign.getInt("minDisplayInterval"));
        Assert.assertNotNull(campaign.startDate);
        Assert.assertEquals(campaign.startDate.getTime(), serializedCampaign.getJSONObject("startDate").getLong("ts"));
        Assert.assertEquals(
            campaign.startDate instanceof TimezoneAwareDate,
            serializedCampaign.getJSONObject("startDate").getBoolean("userTZ")
        );
        Assert.assertNotNull(campaign.endDate);
        Assert.assertEquals(campaign.endDate.getTime(), serializedCampaign.getJSONObject("endDate").getLong("ts"));
        Assert.assertEquals(
            campaign.endDate instanceof TimezoneAwareDate,
            serializedCampaign.getJSONObject("endDate").getBoolean("userTZ")
        );
        JSONArray jsonTriggers = serializedCampaign.getJSONArray("triggers");
        Assert.assertNotNull(jsonTriggers);
        Assert.assertEquals(campaign.triggers.get(0).getType(), jsonTriggers.getJSONObject(0).getString("type"));
        Assert.assertTrue(campaign.output instanceof LandingOutput);
        Assert.assertEquals(
            campaign.output.payload,
            serializedCampaign.getJSONObject("output").getJSONObject("payload")
        );
        Assert.assertTrue(serializedCampaign.getBoolean("requireJIT"));
    }

    @Test
    public void testValidDeserialization() throws JSONException, IOException {
        JSONObject validJsonCampaignsResponse = factory.createValidJsonResponse();
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
            validJsonCampaignsResponse
        );
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertEquals(validJsonCampaignsResponse.getString("id"), response.getQueryID());
        Assert.assertFalse(response.hasError());
        Assert.assertTrue(response.hasCampaigns());
        Assert.assertNull(response.getMinDisplayInterval());
        Assert.assertEquals(response.getCampaigns().size(), 2);

        // Global cappings
        JSONObject jsonCappings = validJsonCampaignsResponse.getJSONObject("cappings");
        JSONObject jsonTimeBasedCappings = jsonCappings.getJSONArray("time").getJSONObject(0);
        Assert.assertNotNull(response.getCappings());
        Assert.assertNotNull(response.getCappings().getSession());
        Assert.assertNotNull(response.getCappings().getTimeBasedCappings());
        Assert.assertTrue(response.hasCappings());
        Assert.assertEquals(response.getCappings().getSession().intValue(), jsonCappings.getInt("session"));
        Assert.assertEquals(response.getCappings().getTimeBasedCappings().size(), 1);
        Assert.assertEquals(
            response.getCappings().getTimeBasedCappings().get(0).getViews().intValue(),
            jsonTimeBasedCappings.getInt("views")
        );
        Assert.assertEquals(
            response.getCappings().getTimeBasedCappings().get(0).getDuration().intValue(),
            jsonTimeBasedCappings.getInt("duration")
        );

        // Local Campaign
        JSONObject jsonCampaign = validJsonCampaignsResponse.getJSONArray("campaigns").getJSONObject(0);
        LocalCampaign campaign = response.getCampaigns().get(0);
        Assert.assertEquals(jsonCampaign.getString("campaignId"), campaign.id);
        Assert.assertNotNull(campaign.capping);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("capping")), campaign.capping);
        Assert.assertEquals(jsonCampaign.getJSONObject("eventData"), campaign.eventData);
        Assert.assertNotNull(campaign.minimumAPILevel);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("minimumApiLevel")), campaign.minimumAPILevel);
        Assert.assertNotNull(campaign.maximumAPILevel);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("maximumApiLevel")), campaign.maximumAPILevel);
        Assert.assertEquals(jsonCampaign.getInt("priority"), campaign.priority);
        Assert.assertEquals(jsonCampaign.getInt("minDisplayInterval"), campaign.minimumDisplayInterval);
        Assert.assertNotNull(campaign.startDate);
        Assert.assertEquals(jsonCampaign.getJSONObject("startDate").getLong("ts"), campaign.startDate.getTime());
        Assert.assertEquals(
            jsonCampaign.getJSONObject("startDate").getBoolean("userTZ"),
            campaign.startDate instanceof TimezoneAwareDate
        );
        Assert.assertNotNull(campaign.endDate);
        Assert.assertEquals(jsonCampaign.getJSONObject("endDate").getLong("ts"), campaign.endDate.getTime());
        Assert.assertEquals(
            jsonCampaign.getJSONObject("endDate").getBoolean("userTZ"),
            campaign.endDate instanceof TimezoneAwareDate
        );
        JSONArray jsonTriggers = jsonCampaign.getJSONArray("triggers");
        Assert.assertNotNull(jsonTriggers);
        Assert.assertEquals(jsonTriggers.getJSONObject(0).getString("type"), campaign.triggers.get(0).getType());
        Assert.assertTrue(campaign.output instanceof LandingOutput);
        Assert.assertEquals(jsonCampaign.getJSONObject("output").getJSONObject("payload"), campaign.output.payload);
        Assert.assertTrue(campaign.requiresJustInTimeSync);

        // Test ACTION deserialization
        jsonCampaign = validJsonCampaignsResponse.getJSONArray("campaigns").getJSONObject(1);
        campaign = response.getCampaigns().get(1);
        Assert.assertTrue(campaign.output instanceof ActionOutput);
        Assert.assertEquals(jsonCampaign.getJSONObject("output").getJSONObject("payload"), campaign.output.payload);
    }

    @Test
    public void testErrorDeserialization() throws JSONException {
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
            factory.createErrorJsonResponse()
        );
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertTrue(response.hasError());
        Assert.assertFalse(response.hasCampaigns());
        Assert.assertFalse(response.hasCappings());
    }

    @Test
    public void testEmptyDeserialization() throws JSONException {
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
            factory.createEmptyJsonResponse()
        );
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertFalse(response.hasError());
        Assert.assertFalse(response.hasCampaigns());
        Assert.assertFalse(response.hasCappings());
    }
}
