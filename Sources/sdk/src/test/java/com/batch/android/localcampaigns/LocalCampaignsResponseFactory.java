package com.batch.android.localcampaigns;

import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.DayOfWeek;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.model.QuietHours;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.query.response.LocalCampaignsResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalCampaignsResponseFactory {

    public LocalCampaignsResponse createLocalCampaignsResponse(LocalCampaignsResponse.Version version)
        throws JSONException {
        LocalCampaignsResponse response = new LocalCampaignsResponse("dummy_id", version);
        response.setCampaigns(new ArrayList<>());
        // Version
        response.setVersion(version);

        // Global cappings (MEP only)
        if (version == LocalCampaignsResponse.Version.MEP) {
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
        }

        LocalCampaign campaign = new LocalCampaign();
        campaign.id =
            version == LocalCampaignsResponse.Version.MEP ? "25876676" : "orchestration_hgei1r4goherjghj12232253";
        campaign.publicToken = "orchestration_hgei1r4goherjghj12232253";
        campaign.capping = 3;
        campaign.eventData = new JSONObject("{\"type\":\"l\",\"foo\":\"bar\"}");
        campaign.minimumAPILevel = 3;
        campaign.maximumAPILevel = 30;
        campaign.priority = 2;
        campaign.minimumDisplayInterval = 3;
        campaign.displayDelay = version == LocalCampaignsResponse.Version.MEP ? 0 : 10;
        campaign.startDate = new TimezoneAwareDate(1499960145L);
        campaign.endDate = new TimezoneAwareDate(2147866695000L);

        if (version == LocalCampaignsResponse.Version.CEP) {
            campaign.quietHours = new QuietHours(10, 0, 20, 0, Arrays.asList(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY));
        }

        campaign.triggers = new ArrayList<>();
        if (version == LocalCampaignsResponse.Version.MEP) {
            campaign.triggers.add(new NextSessionTrigger());
        } else {
            final JSONObject attributes = new JSONObject("{\"attributes\":{\"size.s\":\"M\"}}");
            campaign.triggers.add(new EventLocalCampaignTrigger("E.ADD_TO_CART", "label", attributes));
        }
        campaign.output =
            version == LocalCampaignsResponse.Version.MEP
                ? LandingOutputProvider.get(
                    new JSONObject(
                        "{\n   \"type\":\"LANDING\",\n   \"payload\":{\n      \"kind\":\"universal\",\n      \"id\":\"webtest\",\n      \"did\":\"webtest\",\n      \"hero\":\"https://static.batch.com.s3.eu-west-1.amazonaws.com/documentation/logo_batch_full_178.png\",\n      \"h1\":\"WOW\",\n      \"h2\":\"Ho\",\n      \"h3\":\"Subtitle\",\n      \"body\":\"This is a NEXT_SESSION triggered campaign.\",\n      \"close\":true,\n      \"cta\":[\n         {\n            \"l\":\"Okay!\",\n            \"a\":null,\n            \"args\":{\n               \n            }\n         },\n         {\n            \"l\":\"Okay!2\",\n            \"a\":null,\n            \"args\":{\n               \n            }\n         }\n      ],\n      \"style\":\"#image-cnt {blur: 200;} #image {border-radius: 10; margin-left: 30; margin-right: 30; margin-top: 40;} #placeholder{background-color:#018BAA;}#content {\\n    background-color: #018BFF;\\n    height: 100%\\n   padding-top: 24;\\n    padding-left: 20;\\n    padding-right: 20;\\n    padding-bottom: 20;\\n}\\n#h1 {\\n    color: #018BFF;\\n   padding-left: 15;\\n    padding-right: 15;\\n    padding-top: 4;\\n    padding-bottom: 4;\\n    border-radius: 12;\\n   background-color:white;\\n    font-weight: bold;\\n    font-size: 12;\\n    height: 24;\\n    width: auto;\\n}\\n#h2 {\\n    margin-top: 24;\\n    color: white;\\n    font-weight: bold;\\n    font-size: 35;\\n}\\n#body {\\n    color: #80C5FF;\\n}\\n#cta1 {\\n    color: #018BFF;\\n    padding-left: 60;\\n    padding-right: 60;\\n    padding-top: 10;\\n    padding-bottom: 10;\\n    border-radius: 4;\\n    background-color:white;\\n    font-weight: bold;\\n    font-size: 18;\\n}\\n#close {\\n    glyph-width: 1.5;\\n    glyph-padding: 11;\\n    background-color: #212C3C;\\n    margin-top: 30;\\n    margin-right: 30;\\n}\"\n   }\n}"
                    )
                )
                : LandingOutputProvider.get(
                    new JSONObject(
                        "{\"minMLvl\":30,\"format\":\"fullscreen\",\"position\":\"top\",\"root\":{\"backgroundColor\":[\"#ffffffff\",\"#383838ff\"],\"borderColor\":[\"#000000FF\",\"#000000FF\"],\"borderWidth\":0,\"children\":[{\"aspect\":\"fill\",\"height\":\"200px\",\"id\":\"i$sl\",\"margin\":[8,8,8,8],\"radius\":[8,8,8,8],\"type\":\"image\"},{\"color\":[\"#707070FF\",\"#B3B3B3FF\"],\"fontDecoration\":[\"bold\"],\"fontSize\":14,\"id\":\"tren\",\"margin\":[16,8,0,8],\"maxLines\":0,\"textAlign\":\"center\",\"type\":\"text\"},{\"color\":[\"#383838FF\",\"#EFEFEFff\"],\"fontDecoration\":[\"bold\"],\"fontSize\":26,\"id\":\"tu#b\",\"margin\":[0,8,4,8],\"maxLines\":0,\"textAlign\":\"center\",\"type\":\"text\"},{\"align\":\"center\",\"color\":[\"#474747ff\",\"#b2b2b2ff\"],\"margin\":[8,20,8,20],\"thickness\":1,\"type\":\"divider\",\"width\":\"100%\"},{\"color\":[\"#383838FF\",\"#EFEFEFFF\"],\"fontSize\":14,\"id\":\"t¥kz\",\"margin\":[8,8,8,8],\"maxLines\":0,\"textAlign\":\"center\",\"type\":\"text\"},{\"children\":[{\"align\":\"center\",\"backgroundColor\":[\"#E3E3E3ff\",\"#393939ff\"],\"borderColor\":[\"#000000FF\",\"#000000FF\"],\"borderWidth\":0,\"fontDecoration\":[\"bold\"],\"fontSize\":14,\"id\":\"b66s\",\"margin\":[0,0,0,0],\"maxLines\":0,\"padding\":[12,8,12,8],\"radius\":[8,8,8,8],\"textAlign\":\"center\",\"textColor\":[\"#707070FF\",\"#FFFFFFFF\"],\"type\":\"button\",\"width\":\"100%\"},{\"align\":\"center\",\"backgroundColor\":[\"#0968ACFF\",\"#0968ACFF\"],\"borderColor\":[\"#000000FF\",\"#000000FF\"],\"borderWidth\":0,\"fontDecoration\":[\"bold\"],\"fontSize\":14,\"id\":\"bs06\",\"margin\":[0,0,0,0],\"maxLines\":0,\"padding\":[12,8,12,8],\"radius\":[8,8,8,8],\"textAlign\":\"center\",\"textColor\":[\"#FFFFFFFF\",\"#FFFFFFFF\"],\"type\":\"button\",\"width\":\"100%\"}],\"contentAlign\":\"center\",\"margin\":[16,8,8,8],\"ratios\":[50,50],\"spacing\":8,\"type\":\"columns\"}],\"margin\":[0,0,0,0],\"radius\":[8,8,8,8]},\"closeOptions\":{\"button\":{\"backgroundColor\":[\"#515151ff\",\"#ffffffff\"],\"color\":[\"#FFFFFFFF\",\"#707070FF\"]}},\"texts\":{\"b66s\":\"No thanks\",\"bs06\":\"Yes\",\"tren\":\"obi wan kenobi\",\"tu#b\":\"Hello There !\",\"t¥kz\":\"You don't want to sell me death sticks ! You want to go home and rethink your life.\"},\"urls\":{\"i$sl\":\"https://bstatic.batch.com/9d37a53f05702c2b0903b1bb7df74cd0/682f44ad38c6d.png\",\"is40\":\"https://bstatic.batch.com/2f17fa77df3ee276ab24aa9e93e8a89d/682f42e63b256.png\"},\"actions\":null,\"eventData\":{\"n\":\"00ddb76c-3723-11f0-9b0e-a2f360bd501d\"}}"
                    )
                );
        campaign.requiresJustInTimeSync = true;
        response.getCampaigns().add(campaign);
        return response;
    }

    public JSONObject createValidJsonResponse(LocalCampaignsResponse.Version version)
        throws JSONException, IOException {
        String fileName = version == LocalCampaignsResponse.Version.MEP
            ? "fake_geo_campaigns.json"
            : "fake_cep_campaigns.json";
        ClassLoader classLoader = CampaignManagerTest.class.getClassLoader();
        assert classLoader != null;
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonCampaignsStringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            jsonCampaignsStringBuilder.append(line);
        }
        return new JSONObject(jsonCampaignsStringBuilder.toString()).getJSONArray("queries").getJSONObject(0);
    }

    public JSONObject createErrorJsonResponse() throws JSONException {
        return new JSONObject(
            "{\"id\":\"dummy_id\",\"campaigns_version\":\"MEP\",\"error\": {\"code\": 2, \"reason\": \"internal error\"}}"
        );
    }

    public JSONObject createEmptyJsonResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\",\"campaigns_version\":\"MEP\",\"campaigns\": []}");
    }
}
