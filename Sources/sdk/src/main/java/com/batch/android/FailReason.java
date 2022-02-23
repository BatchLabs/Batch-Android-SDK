package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * Reason for Batch failure
 *
 */
@PublicSDK
public enum FailReason {
    /**
     * Network is not available or not responding
     */
    NETWORK_ERROR,
    /**
     * Your API key is invalid
     */
    INVALID_API_KEY,
    /**
     * Your API key has been deactivated
     */
    DEACTIVATED_API_KEY,
    /**
     * An unexpected error occured, a future retry can succeed
     */
    UNEXPECTED_ERROR,
    /**
     * Batch has globally been opted out from: network calls are not allowed
     */
    SDK_OPTED_OUT,
}
