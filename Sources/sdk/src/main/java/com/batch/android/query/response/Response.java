package com.batch.android.query.response;

import androidx.annotation.NonNull;
import com.batch.android.query.QueryType;

/**
 * A response from server to a query
 */
public abstract class Response {

    /**
     * ID of the query
     */
    @NonNull
    private final String queryID;

    /**
     * Type of the query
     */
    @NonNull
    private final QueryType queryType;

    /**
     * @param queryType Type of the query
     * @param queryID   ID of the query
     */
    @SuppressWarnings("ConstantConditions")
    public Response(@NonNull QueryType queryType, @NonNull String queryID) {
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
     * @return The query ID
     */
    @NonNull
    public String getQueryID() {
        return queryID;
    }

    /**
     * Get the type of the query the response is for
     *
     * @return The query type
     */
    @NonNull
    public QueryType getQueryType() {
        return queryType;
    }
}
