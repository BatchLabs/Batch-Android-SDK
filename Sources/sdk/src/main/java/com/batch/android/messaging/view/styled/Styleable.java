package com.batch.android.messaging.view.styled;

import java.util.Map;

/**
 * Interface describing a styleable view.
 * A styleable view will understand CSS-like rules and apply them on itself.
 *
 */
public interface Styleable {
    void applyStyleRules(Map<String, String> rules);
}
