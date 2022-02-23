package com.batch.android.query.serialization.deserializers;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.AttributesCheckResponse;

/**
 * Deserializer class for {@link AttributesCheckResponse}
 */
public class AttributesCheckResponseDeserializer extends ResponseDeserializer {

    /**
     * Constructor
     *
     * @param json json response
     */
    public AttributesCheckResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return AttributesCheckResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public AttributesCheckResponse deserialize() throws JSONException {
        AttributesCheckResponse response = new AttributesCheckResponse(getId());
        if (json.hasNonNull("action")) {
            response.setActionString(json.getString("action"));
        }
        if (json.hasNonNull("ver")) {
            response.setVersion(json.getLong("ver"));
        }
        if (json.has("t") && !json.isNull("t")) {
            response.setTime(json.getLong("t"));
        }
        return response;
    }
}
