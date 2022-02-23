package com.batch.android.query.response;

import com.batch.android.query.PushQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link PushQuery}
 */
public class PushResponse extends Response {

    /**
     * @param queryID
     */
    public PushResponse(String queryID) {
        super(QueryType.PUSH, queryID);
    }
}
