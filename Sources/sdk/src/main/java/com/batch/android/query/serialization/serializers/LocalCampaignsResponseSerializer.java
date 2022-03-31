package com.batch.android.query.serialization.serializers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.serialization.LocalCampaignSerializer;
import com.batch.android.query.response.LocalCampaignsResponse;
import java.util.List;

/**
 * Serializer class for {@link LocalCampaignsResponse}
 */
public class LocalCampaignsResponseSerializer {

    /**
     * Local campaign serializer
     */
    private final LocalCampaignSerializer localCampaignSerializer = new LocalCampaignSerializer();

    /**
     * (Method not used)
     * Serialize a LocalCampaignsResponse to a JSONObject
     *
     * @return local campaigns response serialized
     * @throws JSONException parsing exception
     */
    public JSONObject serialize(LocalCampaignsResponse response) throws JSONException {
        if (response == null) {
            throw new JSONException("Cannot serialize a null response");
        }
        JSONObject jsonLocalCampaignResponse = new JSONObject();
        jsonLocalCampaignResponse.put("id", response.getQueryID());
        jsonLocalCampaignResponse.put("minDisplayInterval", response.getMinDisplayInterval());
        if (response.getCampaigns().isEmpty()) {
            throw new JSONException("Cannot serialize an empty campaigns list");
        }
        JSONArray jsonCampaigns = localCampaignSerializer.serializeList(response.getCampaigns());
        jsonLocalCampaignResponse.put("campaigns", jsonCampaigns);

        JSONObject jsonCappings = serializeCappings(response.getCappings());
        jsonLocalCampaignResponse.putOpt("cappings", jsonCappings);
        return jsonLocalCampaignResponse;
    }

    /**
     * Serialize a list of campaigns into a json array
     * @param campaigns to serialize
     * @return serialized campaigns
     * @throws JSONException parsing exception
     */
    @NonNull
    public JSONArray serializeCampaigns(List<LocalCampaign> campaigns) throws JSONException {
        return localCampaignSerializer.serializeList(campaigns);
    }

    /**
     * Serialize global in-app cappings into a json object
     * @param cappings to serialize
     * @return serialized cappings
     * @throws JSONException parsing exception
     */
    @Nullable
    public JSONObject serializeCappings(LocalCampaignsResponse.GlobalCappings cappings) throws JSONException {
        if (cappings != null) {
            JSONObject jsonGlobalCapping = new JSONObject();
            jsonGlobalCapping.putOpt("session", cappings.getSession());
            if (cappings.getTimeBasedCappings() != null) {
                JSONArray jsonTimeBasedCappings = new JSONArray();
                for (LocalCampaignsResponse.GlobalCappings.TimeBasedCapping timeBasedCapping : cappings.getTimeBasedCappings()) {
                    JSONObject jsonTimeBasedCapping = new JSONObject();
                    jsonTimeBasedCapping.put("views", timeBasedCapping.getViews());
                    jsonTimeBasedCapping.put("duration", timeBasedCapping.getDuration());
                    jsonTimeBasedCappings.put(jsonTimeBasedCapping);
                }
                jsonGlobalCapping.put("time", jsonTimeBasedCappings);
            }
            return jsonGlobalCapping;
        }
        return null;
    }
}
