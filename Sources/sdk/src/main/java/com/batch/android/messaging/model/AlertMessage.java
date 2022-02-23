package com.batch.android.messaging.model;

import java.io.Serializable;

public class AlertMessage extends Message implements Serializable {

    private static final long serialVersionUID = 0L;

    public String titleText;
    public String cancelButtonText;
    public CTA acceptCTA;
}
