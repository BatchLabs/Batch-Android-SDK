package com.batch.android.messaging.view.styled;

import android.content.Context;
import android.view.View;
import com.batch.android.messaging.view.helper.StyleHelper;
import java.util.Map;

/**
 * Basic separator view
 */
public class SeparatorView extends View implements Styleable {

    public SeparatorView(Context context) {
        super(context);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        StyleHelper.applyCommonRules(this, rules);
    }
}
