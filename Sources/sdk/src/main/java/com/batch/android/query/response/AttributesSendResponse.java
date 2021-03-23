package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.AttributesSendQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link AttributesSendQuery}
 *
 */
public class AttributesSendResponse extends Response
{
    public String transactionID;

    public long version = -1L;

    public AttributesSendResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.ATTRIBUTES, response);

        if (response.has("trid") && !response.isNull("trid")) {
            transactionID = response.getString("trid");
        }

        if (response.has("ver") && !response.isNull("ver")) {
            version = response.getLong("ver");
        }
    }

}
