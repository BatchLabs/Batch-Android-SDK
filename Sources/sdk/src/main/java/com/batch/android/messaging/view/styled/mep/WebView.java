package com.batch.android.messaging.view.styled.mep;

import android.content.Context;
import com.batch.android.messaging.view.helper.StyleHelper;
import java.util.Map;

public class WebView extends android.webkit.WebView implements Styleable {

    public WebView(Context context) {
        super(context);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        StyleHelper.applyCommonRules(this, rules);
    }
}
