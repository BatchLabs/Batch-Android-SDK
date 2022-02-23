package com.batch.android.query.serialization.serializers;

import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.serialization.LocalCampaignSerializer;
import com.batch.android.query.response.LocalCampaignsResponse;

/**
 * Serializer class for {@link LocalCampaignsResponse}
 */
public class LocalCampaignsResponseSerializer {

    /**
     * Initial local campaign response object
     */
    private final LocalCampaignsResponse response;

    /**
     * Local campaign serializer
     */
    private final LocalCampaignSerializer localCampaignSerializer = new LocalCampaignSerializer();

    /**
     * Constructor
     *
     * @param response initial response object
     */
    public LocalCampaignsResponseSerializer(LocalCampaignsResponse response) {
        this.response = response;
    }

    /**
     * Serialize a LocalCampaignsResponse to a JSONObject
     *
     * @return local campaigns response serialized
     * @throws JSONException parsing exception
     */
    public JSONObject serialize() throws JSONException {
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
        return jsonLocalCampaignResponse;
    }
}
