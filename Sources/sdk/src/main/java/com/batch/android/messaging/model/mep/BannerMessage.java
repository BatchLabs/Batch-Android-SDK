package com.batch.android.messaging.model.mep;

import java.io.Serializable;

public class BannerMessage extends BaseBannerMessage implements Serializable {

    private static final long serialVersionUID = 0L;

    public BannerMessage(String messageIdentifier) {
        super(messageIdentifier);
    }
}
