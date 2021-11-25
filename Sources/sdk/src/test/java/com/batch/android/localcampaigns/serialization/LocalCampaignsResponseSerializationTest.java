package com.batch.android.localcampaigns.serialization;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.LocalCampaignsResponseFactory;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.output.LandingOutput;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import com.batch.android.query.serialization.serializers.LocalCampaignsResponseSerializer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignsResponseSerializationTest
{

    private LocalCampaignsResponseFactory factory;

    @Before
    public void setUp()
    {
        factory = new LocalCampaignsResponseFactory();
    }

    @Test
    public void testValidSerialization() throws JSONException
    {
        LocalCampaignsResponse response = factory.createLocalCampaignsResponse();
        LocalCampaignsResponseSerializer serializer = new LocalCampaignsResponseSerializer(response);
        JSONObject serializedResponse = serializer.serialize();
        Assert.assertEquals(response.getQueryID(), serializedResponse.getString("id"));
        Assert.assertFalse(serializedResponse.has("minDisplayInterval"));
        Assert.assertTrue(serializedResponse.hasNonNull("campaigns"));
        LocalCampaign campaign = response.getCampaigns().get(0);
        JSONObject serializedCampaign = serializedResponse.getJSONArray("campaigns").getJSONObject(0);
        Assert.assertEquals(campaign.id, serializedCampaign.getString("campaignId"));
        Assert.assertNotNull(campaign.capping);
        Assert.assertEquals(campaign.capping,
                Integer.valueOf(serializedCampaign.getInt("capping")));
        Assert.assertEquals(campaign.eventData, serializedCampaign.getJSONObject("eventData"));
        Assert.assertNotNull(campaign.minimumAPILevel);
        Assert.assertEquals(campaign.minimumAPILevel,
                Integer.valueOf(serializedCampaign.getInt("minimumApiLevel")));
        Assert.assertNotNull(campaign.maximumAPILevel);
        Assert.assertEquals(campaign.maximumAPILevel,
                Integer.valueOf(serializedCampaign.getInt("maximumApiLevel")));
        Assert.assertEquals(campaign.priority, serializedCampaign.getInt("priority"));
        Assert.assertEquals(campaign.minimumDisplayInterval,
                serializedCampaign.getInt("minDisplayInterval"));
        Assert.assertNotNull(campaign.startDate);
        Assert.assertEquals(campaign.startDate.getTime(),
                serializedCampaign.getJSONObject("startDate").getLong("ts"));
        Assert.assertEquals(campaign.startDate instanceof TimezoneAwareDate,
                serializedCampaign.getJSONObject("startDate").getBoolean("userTZ"));
        Assert.assertNotNull(campaign.endDate);
        Assert.assertEquals(campaign.endDate.getTime(),
                serializedCampaign.getJSONObject("endDate").getLong("ts"));
        Assert.assertEquals(campaign.endDate instanceof TimezoneAwareDate,
                serializedCampaign.getJSONObject("endDate").getBoolean("userTZ"));
        JSONArray jsonTriggers = serializedCampaign.getJSONArray("triggers");
        Assert.assertNotNull(jsonTriggers);
        Assert.assertEquals(campaign.triggers.get(0).getType(),
                jsonTriggers.getJSONObject(0).getString("type"));
        Assert.assertTrue(campaign.output instanceof LandingOutput);
        Assert.assertEquals(campaign.output.payload,
                serializedCampaign.getJSONObject("output").getJSONObject("payload"));
    }

    @Test
    public void testValidDeserialization() throws JSONException, IOException
    {
        JSONObject validJsonCampaignsResponse = factory.createValidJsonResponse();
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
                validJsonCampaignsResponse);
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertEquals(validJsonCampaignsResponse.getString("id"), response.getQueryID());
        Assert.assertFalse(response.hasError());
        Assert.assertTrue(response.hasCampaigns());
        Assert.assertNull(response.getMinDisplayInterval());
        Assert.assertEquals(response.getCampaigns().size(), 1);
        JSONObject jsonCampaign = validJsonCampaignsResponse.getJSONArray("campaigns").getJSONObject(
                0);
        LocalCampaign campaign = response.getCampaigns().get(0);
        Assert.assertEquals(jsonCampaign.getString("campaignId"), campaign.id);
        Assert.assertNotNull(campaign.capping);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("capping")), campaign.capping);
        Assert.assertEquals(jsonCampaign.getJSONObject("eventData"), campaign.eventData);
        Assert.assertNotNull(campaign.minimumAPILevel);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("minimumApiLevel")),
                campaign.minimumAPILevel);
        Assert.assertNotNull(campaign.maximumAPILevel);
        Assert.assertEquals(Integer.valueOf(jsonCampaign.getInt("maximumApiLevel")),
                campaign.maximumAPILevel);
        Assert.assertEquals(jsonCampaign.getInt("priority"), campaign.priority);
        Assert.assertEquals(jsonCampaign.getInt("minDisplayInterval"),
                campaign.minimumDisplayInterval);
        Assert.assertNotNull(campaign.startDate);
        Assert.assertEquals(jsonCampaign.getJSONObject("startDate").getLong("ts"),
                campaign.startDate.getTime());
        Assert.assertEquals(jsonCampaign.getJSONObject("startDate").getBoolean("userTZ"),
                campaign.startDate instanceof TimezoneAwareDate);
        Assert.assertNotNull(campaign.endDate);
        Assert.assertEquals(jsonCampaign.getJSONObject("endDate").getLong("ts"),
                campaign.endDate.getTime());
        Assert.assertEquals(jsonCampaign.getJSONObject("endDate").getBoolean("userTZ"),
                campaign.endDate instanceof TimezoneAwareDate);
        JSONArray jsonTriggers = jsonCampaign.getJSONArray("triggers");
        Assert.assertNotNull(jsonTriggers);
        Assert.assertEquals(jsonTriggers.getJSONObject(0).getString("type"),
                campaign.triggers.get(0).getType());
        Assert.assertTrue(campaign.output instanceof LandingOutput);
        Assert.assertEquals(jsonCampaign.getJSONObject("output").getJSONObject("payload"),
                campaign.output.payload);
    }

    @Test
    public void testErrorDeserialization() throws JSONException
    {
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
                factory.createErrorJsonResponse());
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertTrue(response.hasError());
        Assert.assertFalse(response.hasCampaigns());
    }

    @Test
    public void testEmptyDeserialization() throws JSONException
    {
        LocalCampaignsResponseDeserializer deserializer = new LocalCampaignsResponseDeserializer(
                factory.createEmptyJsonResponse());
        LocalCampaignsResponse response = deserializer.deserialize();
        Assert.assertFalse(response.hasError());
        Assert.assertFalse(response.hasCampaigns());
    }
}
