package com.batch.android.messaging.css;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DOMNode {

    public String type;
    public String identifier;
    public List<String> classes;

    public DOMNode() {
        classes = new ArrayList<>();
    }

    public DOMNode(@NonNull String identifier, @Nullable String... classes) {
        this.identifier = identifier;
        this.classes = new ArrayList<>();
        if (classes != null) {
            Collections.addAll(this.classes, classes);
        }
    }

    public boolean matchesSelector(String selectorString) {
        if (TextUtils.isEmpty(selectorString)) {
            return false;
        }

        final String[] selectors = selectorString.split(",");
        for (String selector : selectors) {
            selector = selector.trim().toLowerCase(Locale.US);
            if (selector.length() < 2) {
                continue;
            }

            final String selectorValue = selector.substring(1);
            if (selector.charAt(0) == '#') {
                if (selectorValue.equalsIgnoreCase(identifier)) {
                    return true;
                }
            } else if (selector.charAt(0) == '.') {
                if (classes == null) {
                    continue;
                }

                for (String className : classes) {
                    if (selectorValue.equalsIgnoreCase(className)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
