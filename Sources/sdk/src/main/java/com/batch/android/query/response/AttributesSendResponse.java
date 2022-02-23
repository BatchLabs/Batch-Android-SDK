package com.batch.android.query.response;

import com.batch.android.query.AttributesSendQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link AttributesSendQuery}
 */
public class AttributesSendResponse extends Response {

    public String transactionID;

    public long version = -1L;

    public AttributesSendResponse(String queryID) {
        super(QueryType.ATTRIBUTES, queryID);
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
