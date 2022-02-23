package com.batch.android.query.response;

import com.batch.android.query.QueryType;
import com.batch.android.query.StartQuery;

/**
 * Response for a {@link StartQuery}
 */
public final class StartResponse extends Response {

    /**
     * @param queryID
     */
    public StartResponse(String queryID) {
        super(QueryType.START, queryID);
    }
}
