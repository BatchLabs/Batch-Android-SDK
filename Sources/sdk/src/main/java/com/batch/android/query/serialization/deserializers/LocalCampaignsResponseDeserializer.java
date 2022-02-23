package com.batch.android.query.serialization.deserializers;

import com.batch.android.core.Logger;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.serialization.LocalCampaignDeserializer;
import com.batch.android.query.response.LocalCampaignsResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializer class for {@link LocalCampaignsResponse}
 */
public class LocalCampaignsResponseDeserializer extends ResponseDeserializer {

    private static final String TAG = "LocalCampaignsResponseDeserializer";

    /**
     * Local campaign deserializer
     */
    private final LocalCampaignDeserializer localCampaignDeserializer = new LocalCampaignDeserializer();

    /**
     * Constructor
     *
     * @param json json response
     */
    public LocalCampaignsResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return LocalCampaignsResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public LocalCampaignsResponse deserialize() throws JSONException {
        if (json == null) {
            throw new JSONException("Cannot deserialize a null json object");
        }

        LocalCampaignsResponse response = new LocalCampaignsResponse(getId());

        LocalCampaignsResponse.Error error = parseError();
        response.setError(error);

        Long minDisplayInterval = json.reallyOptLong("minDisplayInterval", null);

        JSONArray jsonCampaigns = json.optJSONArray("campaigns");
        List<LocalCampaign> campaigns = localCampaignDeserializer.deserializeList(jsonCampaigns);
        response.setCampaigns(campaigns);
        response.setMinDisplayInterval(minDisplayInterval);
        return response;
    }

    /**
     * Parse error response if there's one
     *
     * @return LocalCampaignsResponse.Error || null
     * @throws JSONException parsing exception
     */
    private LocalCampaignsResponse.Error parseError() throws JSONException {
        LocalCampaignsResponse.Error error = null;
        if (json != null && json.hasNonNull("error")) {
            error = new LocalCampaignsResponse.Error();
            JSONObject errorJson = json.getJSONObject("error");
            if (errorJson.hasNonNull("code")) {
                error.setCode(errorJson.getInt("code"));
            }
            if (errorJson.has("message")) {
                error.setMessage(errorJson.getString("message"));
            }
        }
        return error;
    }
}
