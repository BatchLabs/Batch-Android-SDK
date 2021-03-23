package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.QueryType;
import com.batch.android.query.TrackingQuery;

/**
 * Response for a {@link TrackingQuery}
 *
 */
public class TrackingResponse extends Response
{
    /**
     * @param context
     * @param response
     * @throws JSONException
     */
    public TrackingResponse(Context context, JSONObject response) throws JSONException
    {
        super(context, QueryType.TRACKING, response);
    }
}
