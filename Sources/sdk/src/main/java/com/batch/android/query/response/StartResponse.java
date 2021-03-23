package com.batch.android.query.response;


import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.QueryType;
import com.batch.android.query.StartQuery;

/**
 * Response for a {@link StartQuery}
 *
 */
public final class StartResponse extends Response
{

    /**
     * @param context
     * @param response
     * @throws JSONException
     */
    public StartResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.START, response);
    }

}
