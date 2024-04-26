package com.batch.android.query.response;

import com.batch.android.query.AttributesSendQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link AttributesSendQuery}
 */
public class AttributesSendResponse extends Response {

    private String transactionID;

    private long version = -1L;

    private String projectKey;

    public AttributesSendResponse(String queryID) {
        super(QueryType.ATTRIBUTES, queryID);
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public long getVersion() {
        return version;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }
}
