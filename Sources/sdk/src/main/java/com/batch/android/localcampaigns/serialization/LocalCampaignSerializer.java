package com.batch.android.localcampaigns.serialization;

import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.output.LandingOutput;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import java.util.List;

public class LocalCampaignSerializer {

    /**
     * Serialize a local campaign into a JSONObject
     *
     * @param campaign campaign to serialize
     * @return the json object of the serialized campaign
     * @throws JSONException parsing exception
     */
    public JSONObject serialize(LocalCampaign campaign) throws JSONException {
        JSONObject jsonCampaign = new JSONObject();

        jsonCampaign.put("campaignId", campaign.id);

        if (campaign.publicToken != null) {
            jsonCampaign.put("campaignToken", campaign.publicToken);
        }

        jsonCampaign.put("eventData", campaign.eventData);

        if (campaign.minimumAPILevel != null) {
            jsonCampaign.put("minimumApiLevel", campaign.minimumAPILevel);
        }

        if (campaign.maximumAPILevel != null) {
            jsonCampaign.put("maximumApiLevel", campaign.maximumAPILevel);
        }

        jsonCampaign.put("priority", Math.max(campaign.priority, 0));
        jsonCampaign.put("minDisplayInterval", Math.max(campaign.minimumDisplayInterval, 0));

        if (campaign.startDate != null) {
            JSONObject startDateJSON = new JSONObject();
            startDateJSON.put("ts", campaign.startDate.getTime());
            startDateJSON.put("userTZ", campaign.startDate instanceof TimezoneAwareDate);
            jsonCampaign.put("startDate", startDateJSON);
        }

        if (campaign.endDate != null) {
            JSONObject endDateJson = new JSONObject();
            endDateJson.put("ts", campaign.endDate.getTime());
            endDateJson.put("userTZ", campaign.endDate instanceof TimezoneAwareDate);
            jsonCampaign.put("endDate", endDateJson);
        }
        if (campaign.capping != null && campaign.capping >= 0) {
            jsonCampaign.put("capping", campaign.capping);
        }
        jsonCampaign.put("persist", campaign.persist);
        jsonCampaign.put("output", parseOutput(campaign.output));
        jsonCampaign.put("triggers", parseTriggers(campaign.triggers));
        if (campaign.customPayload != null) {
            jsonCampaign.put("customPayload", campaign.customPayload);
        }
        jsonCampaign.put("requireJIT", campaign.requiresJustInTimeSync);
        return jsonCampaign;
    }

    /**
     * Serialize a list of local campaign into a JSONArray
     * @param campaigns campaigns to serialize
     * @return json array of the serialized campaigns
     * @throws JSONException parsing exception
     */
    public JSONArray serializeList(List<LocalCampaign> campaigns) throws JSONException {
        JSONArray jsonLocalCampaigns = new JSONArray();
        for (LocalCampaign campaign : campaigns) {
            JSONObject jsonCampaign = serialize(campaign);
            jsonLocalCampaigns.put(jsonCampaign);
        }
        return jsonLocalCampaigns;
    }

    /**
     * Parse a campaign output
     *
     * @param output campaign output
     * @return the json object of the serialized output
     * @throws JSONException parsing exception
     */
    private JSONObject parseOutput(LocalCampaign.Output output) throws JSONException {
        JSONObject jsonOutput = new JSONObject();
        if (output instanceof LandingOutput) {
            jsonOutput.put("type", "LANDING");
            jsonOutput.put("payload", output.payload);
        }
        return jsonOutput;
    }

    /**
     * Parse the triggers of the campaign
     *
     * @param triggers triggers to parse
     * @return A json array of the serialized triggers
     * @throws JSONException parsing exception
     */
    private JSONArray parseTriggers(List<LocalCampaign.Trigger> triggers) throws JSONException {
        JSONArray jsonTriggers = new JSONArray();
        for (LocalCampaign.Trigger trigger : triggers) {
            JSONObject jsonTrigger = parseTrigger(trigger);
            jsonTriggers.put(jsonTrigger);
        }
        return jsonTriggers;
    }

    /**
     * Parse a trigger
     *
     * @param trigger trigger to parse
     * @return A json object of the given trigger
     * @throws JSONException parsing exception
     */
    private JSONObject parseTrigger(LocalCampaign.Trigger trigger) throws JSONException {
        JSONObject jsonTrigger = new JSONObject();
        jsonTrigger.put("type", trigger.getType());
        if (trigger.getType().equals("EVENT")) {
            EventLocalCampaignTrigger eventTrigger = (EventLocalCampaignTrigger) trigger;
            jsonTrigger.put("event", eventTrigger.name);
            jsonTrigger.put("label", eventTrigger.label);
        }
        return jsonTrigger;
    }
}
