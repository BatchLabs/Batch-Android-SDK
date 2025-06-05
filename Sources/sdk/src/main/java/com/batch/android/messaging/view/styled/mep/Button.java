package com.batch.android.messaging.view.styled.mep;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatButton;
import com.batch.android.messaging.view.helper.StyleHelper;
import java.util.Map;

/**
 * Styleable Button, based on appcompat-v7's Button
 *
 */
public class Button extends AppCompatButton implements Styleable {

    public Button(Context context) {
        super(context);
    }

    public Button(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Button(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("elevation".equalsIgnoreCase(rule.getKey())) {
                // The base CSS got rid of the elevation, but buttons add another!
                Float val = StyleHelper.optFloat(rule.getValue());
                if (val != null && val == 0) {
                    setStateListAnimator(null);
                }
            }
        }

        TextView.applyStyleRules(this, rules);
    }
}
