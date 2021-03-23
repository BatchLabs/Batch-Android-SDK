package com.batch.android.query.response;

import android.content.Context;

import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.query.QueryType;

/**
 * A response from server to a query
 *
 */
public abstract class Response
{
    /**
     * ID of the query
     */
    private String queryID;
    /**
     * Saved application context
     */
    private Context context;
    /**
     * Type of the query
     */
    private QueryType queryType;

// ----------------------------------------->

    /**
     * @param context
     * @param queryType
     * @param response
     * @throws JSONException
     */
    public Response(Context context, QueryType queryType, JSONObject response) throws JSONException
    {
        this(context, queryType, response.getString("id"));
    }

    /**
     * @param context
     * @param queryType
     * @param queryID
     */
    public Response(Context context, QueryType queryType, String queryID)
    {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        if (queryID == null) {
            throw new NullPointerException("queryID==null");
        }

        if (queryType == null) {
            throw new NullPointerException("queryType==null");
        }

        this.context = context.getApplicationContext();
        this.queryID = queryID;
        this.queryType = queryType;
    }

// ------------------------------------------>

    /**
     * Get the ID of the query the response is for
     *
     * @return
     */
    public String getQueryID()
    {
        return queryID;
    }

    /**
     * Get the type of the query the response is for
     *
     * @return
     */
    public QueryType getQueryType()
    {
        return queryType;
    }

    /**
     * Get the saved application context
     *
     * @return
     */
    protected Context getContext()
    {
        return context;
    }
}
