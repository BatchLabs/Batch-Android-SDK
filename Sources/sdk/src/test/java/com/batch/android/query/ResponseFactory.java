package com.batch.android.query;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;

public class ResponseFactory {

    public JSONObject createJsonAttributesCheckResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\",\"action\":\"RECHECK\",\"ver\":1,\"t\":1499960145}");
    }

    public JSONObject createJsonAttributesSendResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\",\"trid\":\"1234-1234-1234\",\"ver\":1}");
    }

    public JSONObject createJsonPushResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\"}");
    }

    public JSONObject createJsonStartResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\"}");
    }

    public JSONObject createJsonTrackResponse() throws JSONException {
        return new JSONObject("{\"id\":\"dummy_id\"}");
    }
}
