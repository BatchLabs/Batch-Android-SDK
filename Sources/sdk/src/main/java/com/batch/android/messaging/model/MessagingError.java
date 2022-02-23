package com.batch.android.messaging.model;

// Represents a generic messaging error.
// For now, this is focused on network errors, but other cases might be added.
// Is used as a cause in "close_error" but might find some other usages later
public enum MessagingError {
    /**
     * Unknown error cause
     */
    UNKNOWN(0),

    /**
     * A server failure: bad SSL configuration, non 2xx HTTP status code
     */
    SERVER_FAILURE(1),

    /**
     * Unprocessable response (for example: a server served an image that could not be decoded)
     */
    INVALID_RESPONSE(2),

    /**
     * Temporary network error, which may be the client's fault: DNS failure, Timeout, etc...
     */
    CLIENT_NETWORK(3);

    public final int code;

    MessagingError(int code) {
        this.code = code;
    }
}
