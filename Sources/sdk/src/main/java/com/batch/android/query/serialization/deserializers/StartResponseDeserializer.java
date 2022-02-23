package com.batch.android.query.serialization.deserializers;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.StartResponse;

/**
 * Deserializer class for {@link StartResponse}
 */
public class StartResponseDeserializer extends ResponseDeserializer {

    /**
     * Constructor
     *
     * @param json json response
     */
    public StartResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return StartResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public StartResponse deserialize() throws JSONException {
        return new StartResponse(getId());
    }
}
