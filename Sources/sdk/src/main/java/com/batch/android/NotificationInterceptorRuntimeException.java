package com.batch.android;

/**
 * Wraps an exception that happened in a {@link BatchNotificationInterceptor}
 * <p>
 * Not meant to be exposed directly into the public api, but to be rethrown
 * <p>
 * No cause string is supported as it's pretty explicit
 *
 * @hide
 */
class NotificationInterceptorRuntimeException extends Exception {

    private RuntimeException wrappedRuntimeException;

    NotificationInterceptorRuntimeException(RuntimeException cause) {
        super(cause);
        this.wrappedRuntimeException = cause;
    }

    public RuntimeException getWrappedRuntimeException() {
        return wrappedRuntimeException;
    }
}
