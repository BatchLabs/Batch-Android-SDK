package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.AttributesCheckQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link AttributesCheckQuery}
 *
 */
public class AttributesCheckResponse extends Response
{

    private String actionString;

    public long version = -1L;

    public Long time = null;

    public AttributesCheckResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.ATTRIBUTES_CHECK, response);

        if (response.has("action") && !response.isNull("action")) {
            actionString = response.getString("action");
        }

        if (response.has("ver") && !response.isNull("ver")) {
            version = response.getLong("ver");
        }

        if (response.has("t") && !response.isNull("t")) {
            version = response.getLong("t");
        }
    }

    public Action getAction()
    {
        if (actionString == null) {
            return Action.UNKNOWN;
        }

        if ("OK".equalsIgnoreCase(actionString)) {
            return Action.OK;
        } else if ("BUMP".equalsIgnoreCase(actionString)) {
            return Action.BUMP;
        } else if ("RECHECK".equalsIgnoreCase(actionString)) {
            return Action.RECHECK;
        } else if ("RESEND".equalsIgnoreCase(actionString)) {
            return Action.RESEND;
        }

        return Action.UNKNOWN;
    }

    public enum Action
    {
        OK,
        BUMP,
        RECHECK,
        RESEND,
        UNKNOWN
    }
}
