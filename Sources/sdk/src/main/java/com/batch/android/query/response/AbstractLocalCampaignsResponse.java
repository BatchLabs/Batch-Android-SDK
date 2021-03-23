package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.QueryType;

/**
 * Abstract response for all queries used by {@link com.batch.android.LocalCampaignsWebservice}
 */

public abstract class AbstractLocalCampaignsResponse extends Response
{
    public AbstractLocalCampaignsResponse(Context context,
                                          QueryType queryType,
                                          JSONObject response) throws JSONException
    {
        super(context, queryType, response);
    }

    public AbstractLocalCampaignsResponse(Context context, QueryType queryType, String queryID)
    {
        super(context, queryType, queryID);
    }
}
