package com.batch.android.messaging.model;

import java.io.Serializable;

public class WebViewMessage extends Message implements Serializable {

    private static final long serialVersionUID = 0L;

    public String css;
    public String url;
    public int timeout; //ms
    public boolean openDeeplinksInApp;
    public boolean devMode;
}
