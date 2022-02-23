package com.batch.android.query;

import android.content.Context;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;

/**
 * Query to check if the attributes have been received by the server
 *
 */
public class AttributesCheckQuery extends Query {

    /**
     * Attributes version
     */
    private long version;

    /**
     * Saved transaciton ID for that version
     */
    private String transactionID;

    // -------------------------------------------->

    public AttributesCheckQuery(Context context, long version, String transactionID) {
        super(context, QueryType.ATTRIBUTES_CHECK);
        if (version <= 0) {
            throw new IllegalArgumentException("version <= 0");
        }

        if (transactionID == null) {
            throw new IllegalArgumentException("transactionID==null");
        }

        this.version = version;
        this.transactionID = transactionID;
    }

    // -------------------------------------------->

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = super.toJSON();

        obj.put("ver", version);
        obj.put("trid", transactionID);

        return obj;
    }
}
