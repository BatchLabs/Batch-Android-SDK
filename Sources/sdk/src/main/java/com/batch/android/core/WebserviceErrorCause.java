package com.batch.android.core;

/**
 * Possible causes of webservice failure
 *
 */
public enum WebserviceErrorCause {
    /**
     * When the server response was not readable
     */
    PARSING_ERROR,

    /**
     * When the response is 500
     */
    SERVER_ERROR,

    /**
     * On network timeout
     */
    NETWORK_TIMEOUT,

    /**
     * On SSL error
     */
    SSL_HANDSHAKE_FAILURE,

    /**
     * Other cause of failure
     */
    OTHER,
}
