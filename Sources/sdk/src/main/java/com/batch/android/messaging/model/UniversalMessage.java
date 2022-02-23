package com.batch.android.messaging.model;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UniversalMessage extends Message implements Serializable {

    private static final long serialVersionUID = 0L;

    public String css;
    public String headingText;
    public String titleText;
    public String subtitleText;
    public List<CTA> ctas = new ArrayList<>();
    public String heroImageURL;
    public String videoURL;
    public String heroDescription;
    public Boolean showCloseButton;
    public Boolean attachCTAsBottom;
    public Boolean stackCTAsHorizontally;
    public Boolean stretchCTAsHorizontally;
    public Boolean flipHeroVertical;
    public Boolean flipHeroHorizontal;
    public Double heroSplitRatio;
    public int autoCloseDelay; // ms

    @Nullable
    public String getVoiceString() {
        if (!TextUtils.isEmpty(titleText)) {
            return titleText;
        }

        if (!TextUtils.isEmpty(headingText)) {
            return headingText;
        }

        return null;
    }
}
