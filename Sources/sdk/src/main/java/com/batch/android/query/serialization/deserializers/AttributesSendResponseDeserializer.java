package com.batch.android.query.serialization.deserializers;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.response.AttributesSendResponse;

/**
 * Deserializer class for {@link AttributesSendResponse}
 */
public class AttributesSendResponseDeserializer extends ResponseDeserializer {

    /**
     * Constructor
     *
     * @param json json response
     */
    public AttributesSendResponseDeserializer(JSONObject json) {
        super(json);
    }

    /**
     * Deserialize method
     *
     * @return AttributesSendResponse deserialized
     * @throws JSONException parsing exception
     */
    @Override
    public AttributesSendResponse deserialize() throws JSONException {
        AttributesSendResponse response = new AttributesSendResponse(getId());
        if (json.hasNonNull("trid")) {
            response.setTransactionID(json.getString("trid"));
        }
        if (json.hasNonNull("ver")) {
            response.setVersion(json.getLong("ver"));
        }
        return response;
    }
}
