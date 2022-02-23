package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * Batch Messaging Exception. Usually wraps another exception.
 */
@PublicSDK
public class BatchMessagingException extends Exception {

    public BatchMessagingException() {
        super();
    }

    public BatchMessagingException(String message) {
        super(message);
    }

    public BatchMessagingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BatchMessagingException(Throwable cause) {
        super(cause);
    }
}
