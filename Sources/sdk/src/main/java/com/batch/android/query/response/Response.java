package com.batch.android.query.response;

import com.batch.android.query.QueryType;

/**
 * A response from server to a query
 */
public abstract class Response {

    /**
     * ID of the query
     */
    private String queryID;
    /**
     * Type of the query
     */
    private QueryType queryType;

    /**
     * @param queryType
     * @param queryID
     */
    public Response(QueryType queryType, String queryID) {
        if (queryID == null) {
            throw new NullPointerException("queryID==null");
        }

        if (queryType == null) {
            throw new NullPointerException("queryType==null");
        }

        this.queryID = queryID;
        this.queryType = queryType;
    }

    /**
     * Get the ID of the query the response is for
     *
     * @return
     */
    public String getQueryID() {
        return queryID;
    }

    /**
     * Get the type of the query the response is for
     *
     * @return
     */
    public QueryType getQueryType() {
        return queryType;
    }
}
