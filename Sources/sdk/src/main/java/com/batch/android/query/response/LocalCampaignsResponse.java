package com.batch.android.query.response;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.batch.android.core.Logger;
import com.batch.android.date.TimezoneAwareDate;
import com.batch.android.date.UTCDate;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.CampaignsLoadedTrigger;
import com.batch.android.localcampaigns.trigger.CampaignsRefreshedTrigger;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;
import com.batch.android.localcampaigns.trigger.NowTrigger;
import com.batch.android.query.LocalCampaignsQuery;
import com.batch.android.query.QueryType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Response for {@link LocalCampaignsQuery}
 */

public class LocalCampaignsResponse extends AbstractLocalCampaignsResponse
{
    private static final String TAG = "LocalCampaignsResponse";

    private List<LocalCampaign> campaigns;
    private Long minDisplayInterval;
    private boolean autosave;

    public LocalCampaignsResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.LOCAL_CAMPAIGNS, response);
        autosave = true;

        parseResponse(response);
    }

    public LocalCampaignsResponse(Context context,
                                  JSONObject response,
                                  boolean autosave) throws JSONException
    {
        super(context, QueryType.LOCAL_CAMPAIGNS, response);
        this.autosave = autosave;

        parseResponse(response);
    }

    @NonNull
    public List<LocalCampaign> getCampaigns()
    {
        return campaigns != null ? campaigns : new ArrayList<>();
    }

    @Nullable
    public Long getMinDisplayInterval()
    {
        return minDisplayInterval;
    }

    private void parseResponse(JSONObject response) throws JSONException
    {
        // TODO Parse that out of the response
        if (response.has("error")) {
            JSONObject errorJson = response.getJSONObject("error");

            StringBuilder errorStringBuilder = new StringBuilder();
            if (errorJson.has("code") || errorJson.has("message")) {
                errorStringBuilder.append("Local campaigns response contains an error : ")
                        .append(errorJson.toString());
            } else {
                errorStringBuilder.append("Local campaigns response contains an unidentified error.");
            }
            Logger.internal(TAG, errorStringBuilder.toString());

            if (autosave) {
                CampaignManagerProvider.get().deleteSavedCampaignsAsync(
                        getContext());
            }
            return;
        }

        minDisplayInterval = response.reallyOptLong("minDisplayInterval", null);

        // Parse campaigns
        JSONArray contents = response.optJSONArray("campaigns");

        List<JSONObject> persistCampaigns = new ArrayList<>();

        if (contents != null) {
            List<LocalCampaign> campaigns = new ArrayList<>();

            for (int i = 0; i < contents.length(); i++) {
                try {
                    JSONObject campaignJson = contents.getJSONObject(i);
                    LocalCampaign localCampaign = parseCampaign(campaignJson);
                    if (localCampaign.persist) {
                        persistCampaigns.add(campaignJson);
                    }

                    campaigns.add(localCampaign);
                } catch (Exception e) {
                    Logger.internal(TAG,
                            "An error occurred while parsing an In-App Campaign. Skipping.",
                            e);
                }
            }

            this.campaigns = campaigns;
        }

        // Save response in Background
        //TODO: This should manually be done, not on class instaniation!
        if (autosave) {
            JSONObject responseCopy = new JSONObject();
            //TODO: Temporary fix for ch24046
            responseCopy.put("id", response.reallyOptString("id", "dummy_id"));
            responseCopy.put("campaigns", new JSONArray(persistCampaigns));

            saveResponse(responseCopy);
        }
    }

    private LocalCampaign parseCampaign(JSONObject json) throws JSONException
    {
        if (json == null) {
            throw new JSONException("Cannot parse a null campaign json");
        }

        LocalCampaign campaign = new LocalCampaign();

        campaign.id = json.getString("campaignId");
        if (campaign.id == null || TextUtils.isEmpty(campaign.id.trim())) {
            throw new JSONException("Invalid campaignId");
        }

        campaign.publicToken = json.optString("campaignToken");

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

        campaign.minimumDisplayInterval = json.reallyOptInteger("minDisplayInterval",
                campaign.minimumDisplayInterval);
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
        if (campaign.endDate != null && campaign.startDate != null && campaign.startDate.compareTo(
                campaign.endDate) >= 0)
        {
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

        return campaign;
    }

    private LocalCampaign.Output parseOutput(JSONObject json) throws JSONException
    {
        String type = json.reallyOptString("type", null);

        if (TextUtils.isEmpty(type)) {
            throw new JSONException("Invalid campaign output type");
        }

        type = type.toUpperCase(Locale.US);

        LocalCampaign.Output output;
        JSONObject payload = json.getJSONObject("payload");
        switch (type) {
            case "LANDING":
                output = LandingOutputProvider.get(payload);
                break;
            default:
                throw new JSONException("Invalid campaign output type");
        }

        return output;
    }

    private List<LocalCampaign.Trigger> parseTriggers(JSONArray json) throws JSONException
    {
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

    private LocalCampaign.Trigger parseTrigger(JSONObject json) throws JSONException
    {
        String type = json.reallyOptString("type", null);

        if (TextUtils.isEmpty(type)) {
            throw new JSONException("Invalid campaign trigger type");
        }

        type = type.toUpperCase(Locale.US);

        switch (type) {
            case "NOW":
                return new NowTrigger();
            case "CAMPAIGNS_REFRESHED":
                return new CampaignsRefreshedTrigger();
            case "CAMPAIGNS_LOADED":
                return new CampaignsLoadedTrigger();
            case "NEXT_SESSION":
                return new NextSessionTrigger();
            case "EVENT":
                String eventName = json.reallyOptString("event", null);
                if (TextUtils.isEmpty(type)) {
                    throw new JSONException("Invalid campaign event trigger name");
                }
                return new EventLocalCampaignTrigger(eventName,
                        json.reallyOptString("label", null));
            default:
                throw new JSONException("Unknown campaign triggers \"" + type + "\"");
        }
    }

    @VisibleForTesting
    protected void saveResponse(@NonNull JSONObject response) {
        CampaignManagerProvider.get().saveCampaignsResponseAsync(
                getContext(),
                response);
    }
}
