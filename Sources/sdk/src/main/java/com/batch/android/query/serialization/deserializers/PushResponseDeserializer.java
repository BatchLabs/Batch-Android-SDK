package com.batch.android.query.serialization.deserializers;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.PushResponse;

/**
 * Deserializer class for {@link PushResponse}
 */
public class PushResponseDeserializer extends ResponseDeserializer {

    /**
     * Constructor
     *
     * @param json json response
     */
    public PushResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return PushResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public PushResponse deserialize() throws JSONException {
        return new PushResponse(getId());
    }
}
