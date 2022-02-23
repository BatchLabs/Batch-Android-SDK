package com.batch.android.messaging;

/**
 * Exception that represents a messaging payload parsing error
 *
 */
public class PayloadParsingException extends Exception {

    public PayloadParsingException() {}

    public PayloadParsingException(String detailMessage) {
        super(detailMessage);
    }

    public PayloadParsingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public PayloadParsingException(Throwable throwable) {
        super(throwable);
    }
}
