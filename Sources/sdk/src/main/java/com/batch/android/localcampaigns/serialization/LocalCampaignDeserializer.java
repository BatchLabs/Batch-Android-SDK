package com.batch.android.localcampaigns.serialization;

import android.text.TextUtils;
import com.batch.android.core.Logger;
import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.date.UTCDate;
import com.batch.android.di.providers.ActionOutputProvider;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.output.ActionOutput;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocalCampaignDeserializer {

    private static final String TAG = "LocalCampaignDeserializer";

    /**
     * Parse a json campaigns object
     *
     * @param json campaign json object
     * @return the LocalCampaign
     * @throws JSONException parsing exception
     */
    public LocalCampaign deserialize(JSONObject json) throws JSONException {
        if (json == null) {
            throw new JSONException("Cannot parse a null campaign json");
        }

        LocalCampaign campaign = new LocalCampaign();

        campaign.id = json.getString("campaignId");
        if (campaign.id == null || TextUtils.isEmpty(campaign.id.trim())) {
            throw new JSONException("Invalid campaignId");
        }

        campaign.publicToken = json.optString("campaignToken", null);

        campaign.eventData = json.getJSONObject("eventData");
        if (campaign.eventData == null) {
            throw new JSONException("Invalid eventData");
        }

        campaign.minimumAPILevel = json.reallyOptInteger("minimumApiLevel", null);
        if (campaign.minimumAPILevel != null && campaign.minimumAPILevel < 0) {
            throw new JSONException("Invalid campaign minimum API level");
        }

        campaign.maximumAPILevel = json.reallyOptInteger("maximumApiLevel", null);
        if (campaign.maximumAPILevel != null && campaign.maximumAPILevel < 0) {
            throw new JSONException("Invalid campaign maximum API level");
        }

        campaign.priority = json.reallyOptInteger("priority", 0);
        if (campaign.priority < 0) {
            throw new JSONException("Invalid campaign priority");
        }

        campaign.minimumDisplayInterval = json.reallyOptInteger("minDisplayInterval", campaign.minimumDisplayInterval);
        if (campaign.minimumDisplayInterval < 0) {
            throw new JSONException("Invalid campaign minimum display interval");
        }

        JSONObject startDateJSON = json.optJSONObject("startDate");
        if (startDateJSON != null) {
            long startDateTimestamp = startDateJSON.getLong("ts");
            if (startDateJSON.reallyOptBoolean("userTZ", true)) {
                campaign.startDate = new TimezoneAwareDate(startDateTimestamp);
            } else {
                campaign.startDate = new UTCDate(startDateTimestamp);
            }
        }

        JSONObject endDateJSON = json.optJSONObject("endDate");
        if (endDateJSON != null) {
            long endDateTimestamp = endDateJSON.getLong("ts");
            if (endDateJSON.reallyOptBoolean("userTZ", true)) {
                campaign.endDate = new TimezoneAwareDate(endDateTimestamp);
            } else {
                campaign.endDate = new UTCDate(endDateTimestamp);
            }
        }

        // If start date is equal or greater than end date, drop it
        if (
            campaign.endDate != null &&
            campaign.startDate != null &&
            campaign.startDate.compareTo(campaign.endDate) >= 0
        ) {
            throw new JSONException("Start date is equals or greater than end date.");
        }

        campaign.capping = json.reallyOptInteger("capping", null);
        if (campaign.capping != null && campaign.capping < 0) {
            throw new JSONException("Invalid campaign capping");
        }

        campaign.persist = json.reallyOptBoolean("persist", true);

        campaign.triggers = parseTriggers(json.getJSONArray("triggers"));

        campaign.output = parseOutput(json.getJSONObject("output"));

        campaign.customPayload = json.optJSONObject("customPayload");

        campaign.requiresJustInTimeSync = json.reallyOptBoolean("requireJIT", false);

        return campaign;
    }

    /**
     * Deserialize a JSONArray into a list of local campaign
     *
     * @param json serialized campaigns
     * @return a list of local campaign
     */
    public List<LocalCampaign> deserializeList(JSONArray json) {
        List<LocalCampaign> campaigns = new ArrayList<>();
        if (json != null) {
            for (int i = 0; i < json.length(); i++) {
                try {
                    JSONObject campaignJson = json.getJSONObject(i);
                    LocalCampaign localCampaign = deserialize(campaignJson);
                    campaigns.add(localCampaign);
                } catch (Exception e) {
                    Logger.internal(TAG, "An error occurred while parsing an In-App Campaign. Skipping.", e);
                }
            }
        }
        return campaigns;
    }

    /**
     * Parse a json output object
     *
     * @param json output json object
     * @return the LocalCampaign.Output
     * @throws JSONException parsing exception
     */
    private LocalCampaign.Output parseOutput(JSONObject json) throws JSONException {
        String type = json.reallyOptString("type", null);

        if (TextUtils.isEmpty(type)) {
            throw new JSONException("Invalid campaign output type");
        }

        type = type.toUpperCase(Locale.US);

        LocalCampaign.Output output;
        JSONObject payload = json.getJSONObject("payload");
        if ("LANDING".equals(type)) {
            output = LandingOutputProvider.get(payload);
        } else if ("ACTION".equals(type)) {
            output = ActionOutputProvider.get(payload);
        } else {
            throw new JSONException("Invalid campaign output type");
        }
        return output;
    }

    /**
     * Parse a json triggers array
     *
     * @param json triggers json array
     * @return A list of <LocalCampaign.Trigger>
     * @throws JSONException parsing exception
     */
    private List<LocalCampaign.Trigger> parseTriggers(JSONArray json) throws JSONException {
        List<LocalCampaign.Trigger> triggers = new ArrayList<>(json.length());
        for (int i = 0; i < json.length(); ++i) {
            try {
                JSONObject jsonTrigger = json.getJSONObject(i);
                LocalCampaign.Trigger trigger = parseTrigger(jsonTrigger);
                triggers.add(trigger);
            } catch (JSONException ex) {
                Logger.internal(TAG, "Invalid trigger : " + ex.toString());
            }
        }
        if (triggers.isEmpty()) {
            throw new JSONException("There is no valid trigger in the list.");
        }
        return triggers;
    }

    /**
     * Parse a json trigger object
     *
     * @param json trigger json object
     * @return the LocalCampaign.Trigger
     * @throws JSONException parsing exception
     */
    private LocalCampaign.Trigger parseTrigger(JSONObject json) throws JSONException {
        String type = json.reallyOptString("type", null);

        if (TextUtils.isEmpty(type)) {
            throw new JSONException("Invalid campaign trigger type");
        }

        type = type.toUpperCase(Locale.US);

        switch (type) {
            // Workaround to handle deprecated ASAP trigger as NEXT_SESSION (post-sync)
            case "NOW":
            case "NEXT_SESSION":
                return new NextSessionTrigger();
            case "EVENT":
                String eventName = json.reallyOptString("event", null);
                if (TextUtils.isEmpty(type)) {
                    throw new JSONException("Invalid campaign event trigger name");
                }
                return new EventLocalCampaignTrigger(eventName, json.reallyOptString("label", null));
            default:
                throw new JSONException("Unknown campaign triggers \"" + type + "\"");
        }
    }
}
