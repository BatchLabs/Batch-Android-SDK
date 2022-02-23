package com.batch.android.messaging.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BaseBannerMessage extends Message implements Serializable {

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

    public enum CTADirection {
        HORIZONTAL,
        VERTICAL,
    }
}
