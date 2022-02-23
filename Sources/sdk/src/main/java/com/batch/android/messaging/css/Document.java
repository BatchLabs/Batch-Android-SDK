package com.batch.android.messaging.css;

import android.graphics.Point;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Document {

    private static final String TAG = "CSS";
    private static final Pattern MEDIA_QUERY_PATTERN = Pattern.compile(
        "@media (ios|android|\\*) and \\((max|min)-(width|height):\\s*(\\d*)\\)"
    );

    public List<Ruleset> rulesets;

    public List<MediaQuery> mediaQueries;

    public Document() {
        rulesets = new ArrayList<>();
        mediaQueries = new ArrayList<>();
    }

    @NonNull
    public Map<String, String> getFlatRules(@NonNull DOMNode node, @Nullable Point screenSize) {
        return getFlatRules(getRules(node, screenSize));
    }

    @NonNull
    public Map<String, String> getFlatRules(@Nullable List<Declaration> declarations) {
        if (declarations == null) {
            return new HashMap<>();
        }

        try {
            Map<String, String> rules = new HashMap<>();
            Map<String, String> variables = new HashMap<>();

            for (Declaration declaration : declarations) {
                if (declaration instanceof Variable) {
                    if (declaration.name != null && declaration.name.length() > 2) {
                        variables.put(declaration.name, declaration.value);
                    }
                } else {
                    if (declaration.name != null) {
                        rules.put(declaration.name, declaration.value);
                    }
                }
            }

            // Merge the variables
            Iterator<Map.Entry<String, String>> i = rules.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, String> rule = i.next();

                final String ruleValue = rule.getValue();
                if (ruleValue.startsWith("var(")) {
                    final String varName = ruleValue.substring(4, ruleValue.length() - 1);
                    if (TextUtils.isEmpty(varName)) {
                        continue;
                    }

                    final String varValue = variables.get(varName.toLowerCase(Locale.US));
                    if (TextUtils.isEmpty(varValue)) {
                        i.remove();
                    } else {
                        rule.setValue(varValue);
                    }
                }
            }

            // Split "margin" and "padding" into individual values
            final String padding = rules.get("padding");
            if (!TextUtils.isEmpty(padding)) {
                String[] splitPadding = padding.split("\\s+");

                if (splitPadding.length == 1) {
                    splitPadding = new String[] { splitPadding[0], splitPadding[0], splitPadding[0], splitPadding[0] };
                }

                if (splitPadding.length == 4) {
                    // We let the explicit rules override the short one
                    if (!rules.containsKey("padding-top")) {
                        rules.put("padding-top", splitPadding[0]);
                    }
                    if (!rules.containsKey("padding-right")) {
                        rules.put("padding-right", splitPadding[1]);
                    }
                    if (!rules.containsKey("padding-bottom")) {
                        rules.put("padding-bottom", splitPadding[2]);
                    }
                    if (!rules.containsKey("padding-left")) {
                        rules.put("padding-left", splitPadding[3]);
                    }
                }

                rules.remove("padding");
            }

            // And now for the margin
            final String margin = rules.get("margin");
            if (!TextUtils.isEmpty(margin)) {
                String[] splitMargin = margin.split("\\s+");

                if (splitMargin.length == 1) {
                    splitMargin = new String[] { splitMargin[0], splitMargin[0], splitMargin[0], splitMargin[0] };
                }

                if (splitMargin.length == 4) {
                    // We let the explicit rules override the short one
                    if (!rules.containsKey("margin-top")) {
                        rules.put("margin-top", splitMargin[0]);
                    }
                    if (!rules.containsKey("margin-right")) {
                        rules.put("margin-right", splitMargin[1]);
                    }
                    if (!rules.containsKey("margin-bottom")) {
                        rules.put("margin-bottom", splitMargin[2]);
                    }
                    if (!rules.containsKey("margin-left")) {
                        rules.put("margin-left", splitMargin[3]);
                    }
                }

                rules.remove("margin");
            }

            return rules;
        } catch (Exception e) {
            Logger.internal(TAG, "Unexpected error while extracting flat rules", e);
            throw e;
        }
    }

    @NonNull
    public List<Declaration> getRules(@NonNull DOMNode node, @Nullable Point screenSize) {
        final List<Declaration> declarations = new ArrayList<>(getRules(node, rulesets));

        for (MediaQuery mediaQuery : mediaQueries) {
            if (matchesMediaQuery(mediaQuery.rule, screenSize)) {
                declarations.addAll(getRules(node, mediaQuery.rulesets));
            }
        }

        return declarations;
    }

    @NonNull
    private List<Declaration> getRules(@NonNull DOMNode node, @NonNull List<Ruleset> rulesets) {
        final List<Declaration> declarations = new ArrayList<>();

        //noinspection ConstantConditions
        if (rulesets == null) {
            return declarations;
        }

        for (Ruleset ruleset : rulesets) {
            // The * block is special, only extract variables from it, but everybody will get them
            // Basically it means that the * block is merged with every other in the scope (where the scope is whether we're in a media query or not)
            if ("*".equals(ruleset.selector)) {
                for (Declaration rulesetDeclaration : ruleset.declarations) {
                    if (rulesetDeclaration instanceof Variable) {
                        declarations.add(rulesetDeclaration);
                    }
                }
                continue;
            }

            if (node.matchesSelector(ruleset.selector)) {
                declarations.addAll(ruleset.declarations);
            }
        }

        return declarations;
    }

    private boolean matchesMediaQuery(String mediaQueryRule, Point screenSize) {
        if (TextUtils.isEmpty(mediaQueryRule)) {
            return false;
        }

        if (screenSize != null) {
            final Matcher matcher = MEDIA_QUERY_PATTERN.matcher(mediaQueryRule);
            if (matcher.matches()) {
                try {
                    String platform = matcher.group(1); // ios|android|*
                    String minMax = matcher.group(2); // min|max
                    String dimension = matcher.group(3); // width|height
                    int size = Integer.parseInt(matcher.group(4)); // 300
                    return matchesSizeMediaQuery(screenSize, platform, minMax, dimension, size);
                } catch (NumberFormatException e) {
                    Logger.internal(TAG, "Error while parsing a media query size rule", e);
                    return false;
                }
            }
        }

        if (mediaQueryRule.startsWith("@android")) {
            if (mediaQueryRule.length() == 8) { // It's only @android
                return true;
            } else {
                if (mediaQueryRule.length() > 9) { // It's @android-xxxx
                    final String wantedApiLevel = mediaQueryRule.substring(9, mediaQueryRule.length());
                    try {
                        int wantedApiLevelInt = Integer.parseInt(wantedApiLevel);
                        if (Build.VERSION.SDK_INT >= wantedApiLevelInt) {
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // Really, do nothing!
                    }
                }
            }
        }
        return false;
    }

    private boolean matchesSizeMediaQuery(
        Point screenSize,
        String platform,
        String minMax,
        String dimension,
        int size
    ) {
        if (screenSize == null || platform == null || minMax == null || dimension == null) {
            return false;
        }

        if ("ios".equals(platform)) {
            // Other possibilities than "ios", "android" or "*" are filtred by the regexp
            return false;
        }

        int comparedSize;

        if ("height".equals(dimension)) {
            comparedSize = screenSize.y;
        } else {
            comparedSize = screenSize.x;
        }

        // max-(width|height) is <=
        // min-(width|height) is >=
        if ("max".equals(minMax)) {
            return comparedSize <= size;
        } else {
            return comparedSize >= size;
        }
    }
}
