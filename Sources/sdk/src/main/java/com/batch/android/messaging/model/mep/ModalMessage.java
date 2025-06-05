package com.batch.android.messaging.model.mep;

import java.io.Serializable;

public class ModalMessage extends BaseBannerMessage implements Serializable {

    private static final long serialVersionUID = 0L;

    public ModalMessage(String messageIdentifier) {
        super(messageIdentifier);
    }
}
