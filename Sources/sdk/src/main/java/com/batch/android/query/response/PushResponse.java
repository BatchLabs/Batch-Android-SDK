package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.PushQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link PushQuery}
 *
 */
public class PushResponse extends Response
{
    /**
     * @param context
     * @param response
     * @throws JSONException
     */
    public PushResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.PUSH, response);
    }

}
