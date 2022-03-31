package com.batch.android.localcampaigns;

import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.query.response.LocalCampaignsResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LocalCampaignsResponseFactory {

    public LocalCampaignsResponse createLocalCampaignsResponse() throws JSONException {
        LocalCampaignsResponse response = new LocalCampaignsResponse("dummy_id");
        response.setCampaigns(new ArrayList<>());

        // Global cappings
        List<LocalCampaignsResponse.GlobalCappings.TimeBasedCapping> timeBasedCappings = new ArrayList<>();
        LocalCampaignsResponse.GlobalCappings.TimeBasedCapping timeBasedCapping = new LocalCampaignsResponse.GlobalCappings.TimeBasedCapping(
            2,
            3600
        );
        timeBasedCappings.add(timeBasedCapping);
        LocalCampaignsResponse.GlobalCappings cappings = new LocalCampaignsResponse.GlobalCappings(
            2,
            timeBasedCappings
        );
        response.setCappings(cappings);
        // Local campaigns
        LocalCampaign campaign = new LocalCampaign();
        campaign.id = "25876676";
        campaign.capping = 3;
        campaign.eventData = new JSONObject("{\"type\":\"l\",\"foo\":\"bar\"}");
        campaign.minimumAPILevel = 3;
        campaign.maximumAPILevel = 30;
        campaign.priority = 2;
        campaign.minimumDisplayInterval = 3;
        campaign.startDate = new TimezoneAwareDate(1499960145L);
        campaign.endDate = new TimezoneAwareDate(2147866695000L);
        campaign.triggers = new ArrayList<>();
        campaign.triggers.add(new NextSessionTrigger());
        campaign.output =
            LandingOutputProvider.get(
                new JSONObject(
                    "{\n   \"type\":\"LANDING\",\n   \"payload\":{\n      \"kind\":\"universal\",\n      \"id\":\"webtest\",\n      \"did\":\"webtest\",\n      \"hero\":\"https://static.batch.com.s3.eu-west-1.amazonaws.com/documentation/logo_batch_full_178.png\",\n      \"h1\":\"WOW\",\n      \"h2\":\"Ho\",\n      \"h3\":\"Subtitle\",\n      \"body\":\"This is a NEXT_SESSION triggered campaign.\",\n      \"close\":true,\n      \"cta\":[\n         {\n            \"l\":\"Okay!\",\n            \"a\":null,\n            \"args\":{\n               \n            }\n         },\n         {\n            \"l\":\"Okay!2\",\n            \"a\":null,\n            \"args\":{\n               \n            }\n         }\n      ],\n      \"style\":\"#image-cnt {blur: 200;} #image {border-radius: 10; margin-left: 30; margin-right: 30; margin-top: 40;} #placeholder{background-color:#018BAA;}#content {\\n    background-color: #018BFF;\\n    height: 100%\\n   padding-top: 24;\\n    padding-left: 20;\\n    padding-right: 20;\\n    padding-bottom: 20;\\n}\\n#h1 {\\n    color: #018BFF;\\n   padding-left: 15;\\n    padding-right: 15;\\n    padding-top: 4;\\n    padding-bottom: 4;\\n    border-radius: 12;\\n   background-color:white;\\n    font-weight: bold;\\n    font-size: 12;\\n    height: 24;\\n    width: auto;\\n}\\n#h2 {\\n    margin-top: 24;\\n    color: white;\\n    font-weight: bold;\\n    font-size: 35;\\n}\\n#body {\\n    color: #80C5FF;\\n}\\n#cta1 {\\n    color: #018BFF;\\n    padding-left: 60;\\n    padding-right: 60;\\n    padding-top: 10;\\n    padding-bottom: 10;\\n    border-radius: 4;\\n    background-color:white;\\n    font-weight: bold;\\n    font-size: 18;\\n}\\n#close {\\n    glyph-width: 1.5;\\n    glyph-padding: 11;\\n    background-color: #212C3C;\\n    margin-top: 30;\\n    margin-right: 30;\\n}\"\n   }\n}"
                )
            );
        campaign.requiresJustInTimeSync = true;
        response.getCampaigns().add(campaign);
        return response;
    }

    public JSONObject createValidJsonResponse() throws JSONException, IOException {
        ClassLoader classLoader = CampaignManagerTest.class.getClassLoader();
        assert classLoader != null;
        InputStream inputStream = classLoader.getResourceAsStream("fake_geo_campaigns.json");
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonCampaignsStringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            jsonCampaignsStringBuilder.append(line);
        }
        return new JSONObject(jsonCampaignsStringBuilder.toString()).getJSONArray("queries").getJSONObject(0);
    }

    public JSONObject createErrorJsonResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\",\"error\": {\"code\": 2, \"reason\": \"internal error\"}}");
    }

    public JSONObject createEmptyJsonResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\",\"campaigns\": []}");
    }
}
