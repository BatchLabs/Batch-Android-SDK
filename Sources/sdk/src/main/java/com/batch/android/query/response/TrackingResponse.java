package com.batch.android.query.response;

import com.batch.android.query.QueryType;
import com.batch.android.query.TrackingQuery;

/**
 * Response for a {@link TrackingQuery}
 */
public class TrackingResponse extends Response {

    /**
     * @param queryID id of the query
     */
    public TrackingResponse(String queryID) {
        super(QueryType.TRACKING, queryID);
    }
}
