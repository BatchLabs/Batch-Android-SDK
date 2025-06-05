package com.batch.android.messaging.model.mep;

import com.batch.android.messaging.model.CTA;
import java.io.Serializable;

public class AlertMessage extends MEPMessage implements Serializable {

    private static final long serialVersionUID = 0L;

    public String titleText;
    public String cancelButtonText;
    public CTA acceptCTA;

    public AlertMessage(String messageIdentifier) {
        super(messageIdentifier);
    }
}
