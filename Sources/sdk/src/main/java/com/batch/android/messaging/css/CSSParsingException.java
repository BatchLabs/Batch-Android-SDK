package com.batch.android.messaging.css;

public class CSSParsingException extends Exception {

    public CSSParsingException() {}

    public CSSParsingException(String detailMessage) {
        super(detailMessage);
    }

    public CSSParsingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public CSSParsingException(Throwable throwable) {
        super(throwable);
    }
}
