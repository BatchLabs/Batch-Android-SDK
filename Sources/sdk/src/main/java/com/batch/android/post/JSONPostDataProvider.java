package com.batch.android.post;

import com.batch.android.core.ByteArrayHelper;
import com.batch.android.json.JSONObject;

/**
 * A json post data provider
 *
 */
public class JSONPostDataProvider implements PostDataProvider<JSONObject> {

    /**
     * The data
     */
    private JSONObject data;

    // ---------------------------------------->

    /**
     * Create a new provider with empty data
     */
    public JSONPostDataProvider() {
        this(new JSONObject());
    }

    /**
     * Create a new provider with given data
     *
     * @param data
     */
    public JSONPostDataProvider(JSONObject data) {
        if (data == null) {
            throw new NullPointerException("Null data");
        }

        this.data = data;
    }

    // ----------------------------------------->

    @Override
    public byte[] getData() {
        return ByteArrayHelper.getUTF8Bytes(data.toString());
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public boolean isEmpty() {
        return data == null || data.length() == 0;
    }

    @Override
    public JSONObject getRawData() {
        return data;
    }
}
