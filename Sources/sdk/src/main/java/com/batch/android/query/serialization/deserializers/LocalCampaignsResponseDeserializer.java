package com.batch.android.query.serialization.deserializers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

        LocalCampaignsResponse.GlobalCappings cappings = deserializeCappings();
        response.setCappings(cappings);

        return response;
    }

    /**
     * Only deserialize the local campaigns from the json response
     * @return A list of LocalCampaign
     */
    @NonNull
    public List<LocalCampaign> deserializeCampaigns() {
        JSONArray jsonCampaigns = json.optJSONArray("campaigns");
        return localCampaignDeserializer.deserializeList(jsonCampaigns);
    }

    /**
     * Only deserialize the global in-app cappings from the json response
     *
     * @return the LocalCampaignsResponse.GlobalCappings
     * @throws JSONException parsing exception
     */
    @Nullable
    public LocalCampaignsResponse.GlobalCappings deserializeCappings() throws JSONException {
        LocalCampaignsResponse.GlobalCappings cappings = null;
        if (json != null && json.hasNonNull("cappings")) {
            JSONObject jsonCappings = json.getJSONObject("cappings");

            Integer session = jsonCappings.reallyOptInteger("session", null);

            List<LocalCampaignsResponse.GlobalCappings.TimeBasedCapping> timeBasedCappings = null;

            if (jsonCappings.hasNonNull("time")) {
                timeBasedCappings = parseTimeBasedCappings(jsonCappings.getJSONArray("time"));
            }
            cappings = new LocalCampaignsResponse.GlobalCappings(session, timeBasedCappings);
        }
        return cappings;
    }

    /**
     * Parse a json array into a list of Time-Based Cappings
     *
     * @param json time based capping array
     * @return the LocalCampaignsResponse.GlobalCappings.TimeBasedCapping list
     */
    @Nullable
    private List<LocalCampaignsResponse.GlobalCappings.TimeBasedCapping> parseTimeBasedCappings(JSONArray json) {
        List<LocalCampaignsResponse.GlobalCappings.TimeBasedCapping> timeBasedCappings = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject jsonTimeBasedCapping = json.getJSONObject(i);
                Integer views = jsonTimeBasedCapping.reallyOptInteger("views", null);
                Integer duration = jsonTimeBasedCapping.reallyOptInteger("duration", null);
                if (views != null && views != 0 && duration != null && duration != 0) {
                    LocalCampaignsResponse.GlobalCappings.TimeBasedCapping timeBasedCapping = new LocalCampaignsResponse.GlobalCappings.TimeBasedCapping(
                        views,
                        duration
                    );
                    timeBasedCappings.add(timeBasedCapping);
                }
            } catch (Exception e) {
                Logger.internal(TAG, "An error occurred while parsing an In-App TimeBasedCapping. Skipping.", e);
            }
        }
        return timeBasedCappings.isEmpty() ? null : timeBasedCappings;
    }

    /**
     * Parse error response if there's one
     *
     * @return LocalCampaignsResponse.Error || null
     * @throws JSONException parsing exception
     */
    @Nullable
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
