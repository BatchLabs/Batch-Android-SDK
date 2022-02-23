package com.batch.android.query.response;

import com.batch.android.query.AttributesCheckQuery;
import com.batch.android.query.QueryType;

/**
 * Response for {@link AttributesCheckQuery}
 */
public class AttributesCheckResponse extends Response {

    private String actionString;

    public long version = -1L;

    public Long time = null;

    public AttributesCheckResponse(String queryID) {
        super(QueryType.ATTRIBUTES_CHECK, queryID);
    }

    public Action getAction() {
        if (actionString == null) {
            return Action.UNKNOWN;
        }

        if ("OK".equalsIgnoreCase(actionString)) {
            return Action.OK;
        } else if ("BUMP".equalsIgnoreCase(actionString)) {
            return Action.BUMP;
        } else if ("RECHECK".equalsIgnoreCase(actionString)) {
            return Action.RECHECK;
        } else if ("RESEND".equalsIgnoreCase(actionString)) {
            return Action.RESEND;
        }

        return Action.UNKNOWN;
    }

    public enum Action {
        OK,
        BUMP,
        RECHECK,
        RESEND,
        UNKNOWN,
    }

    public void setActionString(String actionString) {
        this.actionString = actionString;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
