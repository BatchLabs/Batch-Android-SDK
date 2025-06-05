package com.batch.android.messaging.model.mep;

import java.io.Serializable;

public class WebViewMessage extends MEPMessage implements Serializable {

    private static final long serialVersionUID = 0L;

    public String css;
    public String url;
    public int timeout; //ms
    public boolean openDeeplinksInApp;
    public boolean devMode;

    public WebViewMessage(String messageIdentifier) {
        super(messageIdentifier);
    }
}
