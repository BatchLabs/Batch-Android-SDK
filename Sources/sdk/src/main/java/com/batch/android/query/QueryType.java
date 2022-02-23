package com.batch.android.query;

/**
 * The different query types.
 *
 */
public enum QueryType {
    /**
     * Start query
     */
    START,
    /**
     * Query for event tracking
     */
    TRACKING,
    /**
     * Query for push token sending
     */
    PUSH,
    /**
     * Query for user attributes send
     */
    ATTRIBUTES,
    /**
     * Query for user attributes check
     */
    ATTRIBUTES_CHECK,
    /**
     * Local campaigns
     */
    LOCAL_CAMPAIGNS,
}
