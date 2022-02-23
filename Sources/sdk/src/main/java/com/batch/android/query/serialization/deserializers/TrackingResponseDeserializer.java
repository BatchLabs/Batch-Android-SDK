package com.batch.android.query.serialization.deserializers;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.TrackingResponse;

/**
 * Deserializer class for {@link TrackingResponse}
 */
public class TrackingResponseDeserializer extends ResponseDeserializer {

    /**
     * Constructor
     *
     * @param json json response
     */
    public TrackingResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return TrackingResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public TrackingResponse deserialize() throws JSONException {
        return new TrackingResponse(getId());
    }
}
