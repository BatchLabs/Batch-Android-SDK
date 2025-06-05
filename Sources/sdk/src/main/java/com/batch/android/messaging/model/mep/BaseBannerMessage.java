package com.batch.android.messaging.model.mep;

import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.CTA;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BaseBannerMessage extends MEPMessage implements Serializable {

    private static final long serialVersionUID = 0L;

    public String css;
    public String titleText;
    public Action globalTapAction;
    public long globalTapDelay; // ms
    public boolean allowSwipeToDismiss;
    public String imageURL;
    public String imageDescription;
    public List<CTA> ctas = new ArrayList<>();
    public boolean showCloseButton;
    public int autoCloseDelay; // ms
    public CTADirection ctaDirection = CTADirection.HORIZONTAL;

    public BaseBannerMessage(String messageIdentifier) {
        super(messageIdentifier);
    }

    public enum CTADirection {
        HORIZONTAL,
        VERTICAL,
    }
}
