package com.batch.android.messaging.view.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.ViewCompat;
import com.batch.android.core.Logger;
import com.batch.android.messaging.css.CSSParsingException;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.css.Parser;
import com.batch.android.messaging.css.builtin.BuiltinStyleProvider;
import com.batch.android.messaging.view.FlexboxLayout;
import com.batch.android.messaging.view.PositionableGradientDrawable;
import com.batch.android.messaging.view.percent.PercentRelativeLayout;
import com.batch.android.messaging.view.styled.Button;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for Style-related methods.
 * If you want to implement rules that are applied to all views, that's the right place.
 * Also contains some general measurement/general purpose helpers.
 */
public class StyleHelper {

    private static final String TAG = "StyleHelper";

    /**
     * A semi transparent black color for the ripple overlay
     */
    private static final int RIPPLE_COLOR = Color.parseColor("#22000000");

    /**
     * Static helper, do not instantiate yourself
     */
    private StyleHelper() {}

    /**
     * Parse raw css
     *
     * @throws IllegalArgumentException Throws if the css isn't valid
     */
    public static Document parseStyle(@Nullable String css) {
        try {
            if (css == null) {
                throw new IllegalArgumentException("Message's style string cannot be null");
            }
            Document style = new Parser(new BuiltinStyleProvider(), css).parse();
            if (style == null) {
                throw new IllegalArgumentException("An error occurred while parsing message style");
            }
            return style;
        } catch (CSSParsingException e) {
            throw new IllegalArgumentException("Unparsable style", e);
        }
    }

    /**
     * Apply common rules to a view.
     * <p>
     * While this method tries to peek at the View's class to avoid code duplication (especially for the background drawable stuff),
     * a view implementing specific rules should extends its base class and implement the {@link com.batch.android.messaging.view.styled.Styleable} interface.
     *
     * @param targetView View that the rules will be applied to.
     * @param rules      CSS-like rules.
     */
    public static void applyCommonRules(View targetView, Map<String, String> rules) {
        // You're in for a wild ride

        if (targetView == null || rules == null) {
            return;
        }

        // left, top, right, bottom
        Float[] padding = { 0f, 0f, 0f, 0f };
        PositionableGradientDrawable backgroundGradient = null;
        Integer backgroundColor = null;
        Integer borderColor = null;
        Float cornerRadius = null;
        Float borderWidth = null;

        boolean shouldClipToOutline = true;

        final Resources res = targetView.getResources();

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("background".equalsIgnoreCase(rule.getKey())) {
                String value = rule.getValue();
                if (value.startsWith("#")) {
                    try {
                        backgroundColor = parseColor(rule.getValue());
                    } catch (IllegalArgumentException e) {
                        Logger.internal(TAG, "Unparsable background color (" + rule.getValue() + ")", e);
                    }
                } else if (value.startsWith("linear-gradient(") && value.endsWith(")")) {
                    value = value.substring(16, value.length() - 1);
                    String[] gradientArguments = value.split(",");
                    if (gradientArguments.length < 3) {
                        continue;
                    }
                    Float angle = optFloat(gradientArguments[0].replace("deg", ""));
                    if (angle == null) {
                        continue;
                    }
                    PositionableGradientDrawable.Orientation gradientOrientation;
                    // Android uses the same value as -webkit-linear-gradient.
                    // We use standard linear-gradiant angle, so we rewrite the switch
                    switch (angle.intValue()) {
                        default:
                        case 0:
                            gradientOrientation = PositionableGradientDrawable.Orientation.BOTTOM_TOP;
                            break;
                        case 90:
                            gradientOrientation = PositionableGradientDrawable.Orientation.LEFT_RIGHT;
                            break;
                        case 180:
                            gradientOrientation = PositionableGradientDrawable.Orientation.TOP_BOTTOM;
                            break;
                        case -90:
                        case 270:
                            gradientOrientation = PositionableGradientDrawable.Orientation.RIGHT_LEFT;
                            break;
                    }

                    int[] colors = new int[gradientArguments.length - 1];

                    List<Float> positions = new LinkedList<>();

                    for (int i = 1; i < gradientArguments.length; i++) {
                        // Split on the space, to see if there's a position
                        // The position must be in [0;1]
                        // ex: linear-gradient(90, #FFBBAA 0.5, #FFAAEE 1)
                        // "%" will be stripped
                        String[] components = gradientArguments[i].replace("%", "").trim().split("\\s+", 2);
                        if (components.length == 0) {
                            continue;
                        }

                        colors[i - 1] = parseColor(components[0]);

                        // Looks like we've got a position
                        if (components.length > 1) {
                            Float position = optFloat(components[1]);
                            if (position != null && position >= 0 && position <= 100) {
                                position = position / 100f;
                                positions.add(position);
                            }
                        }
                    }

                    float[] positionsArray = null;
                    // We need to have exactly one position per color
                    if (positions.size() == colors.length) {
                        positionsArray = new float[positions.size()];
                        for (int i = 0; i < positions.size(); i++) {
                            positionsArray[i] = positions.get(i);
                        }
                    }

                    backgroundGradient = new PositionableGradientDrawable(gradientOrientation, colors, positionsArray);
                }
            } else if ("background-color".equalsIgnoreCase(rule.getKey())) {
                try {
                    backgroundColor = parseColor(rule.getValue());
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable background-color (" + rule.getValue() + ")", e);
                }
            } else if ("border-color".equalsIgnoreCase(rule.getKey())) {
                try {
                    borderColor = parseColor(rule.getValue());
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable border-color (" + rule.getValue() + ")", e);
                }
            } else if ("border-width".equalsIgnoreCase(rule.getKey())) {
                borderWidth = optFloat(rule.getValue());
            } else if ("border-radius".equalsIgnoreCase(rule.getKey())) {
                cornerRadius = optFloat(rule.getValue());
            } else if ("opacity".equalsIgnoreCase(rule.getKey())) {
                Float val = optFloat(rule.getValue());
                if (val != null) {
                    targetView.setAlpha(val);
                }
            } else if ("elevation".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = optFloat(rule.getValue());
                    if (val != null) {
                        targetView.setElevation(dpToPixels(res, val));
                    }
                }
            } else if ("z-index".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = optFloat(rule.getValue());
                    if (val != null) {
                        targetView.setZ(dpToPixels(res, val));
                    }
                }
            } else if ("clip-subviews".equalsIgnoreCase(rule.getKey())) {
                Float val = optFloat(rule.getValue());
                if (val != null) {
                    shouldClipToOutline = val > 0;
                }
            }
            // Padding
            else if ("padding-left".equalsIgnoreCase(rule.getKey())) {
                padding[0] = optFloat(rule.getValue());
            } else if ("padding-top".equalsIgnoreCase(rule.getKey())) {
                padding[1] = optFloat(rule.getValue());
            } else if ("padding-right".equalsIgnoreCase(rule.getKey())) {
                padding[2] = optFloat(rule.getValue());
            } else if ("padding-bottom".equalsIgnoreCase(rule.getKey())) {
                padding[3] = optFloat(rule.getValue());
            }
        }

        if (!(targetView instanceof TextureView)) {
            // Generate the background drawable, if we need the advanced features.
            // Transparent is an edge case, we want to force the background gradient here to remove the elevation
            if (
                (backgroundColor != null && backgroundColor == Color.TRANSPARENT) ||
                backgroundGradient != null ||
                borderColor != null ||
                cornerRadius != null ||
                borderWidth != null
            ) {
                if (backgroundGradient == null) {
                    backgroundGradient = new PositionableGradientDrawable();
                }

                // BG Color
                if (backgroundColor != null) {
                    backgroundGradient.setColor(backgroundColor);
                }

                // Stroke
                if (borderWidth != null) {
                    Integer color = backgroundColor != null ? backgroundColor : borderColor;
                    if (color != null) {
                        backgroundGradient.setStroke(dpToPixels(res, borderWidth), color);
                    }
                }

                // Corner radius
                if (cornerRadius != null) {
                    backgroundGradient.setCornerRadius(dpToPixels(res, cornerRadius));
                }

                Drawable finalDrawable = backgroundGradient;

                if (targetView instanceof Button) {
                    finalDrawable = getPressableGradientDrawable(backgroundGradient);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    targetView.setBackground(finalDrawable);
                } else {
                    //noinspection deprecation
                    targetView.setBackgroundDrawable(finalDrawable);
                }
            } else if (backgroundColor != null) {
                // Simply set the background color if we don't need more advanced stuff
                // Buttons require another tinting way
                if (targetView instanceof AppCompatButton) {
                    int[][] states = new int[][] {
                        new int[] { android.R.attr.state_pressed },
                        new int[] { android.R.attr.state_enabled },
                    };

                    int[] colors = new int[] { darkenColor(backgroundColor), backgroundColor };

                    ViewCompat.setBackgroundTintList(targetView, new ColorStateList(states, colors));
                } else {
                    targetView.setBackgroundColor(backgroundColor);
                }
            }

            if (cornerRadius != null && shouldClipToOutline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                targetView.setClipToOutline(true);
            }
        }

        final Drawable background = targetView.getBackground();
        Rect bgPadding = new Rect();
        if (background != null) {
            background.getPadding(bgPadding);
        }

        targetView.setPadding(
            bgPadding.left + dpToPixels(res, padding[0]),
            bgPadding.top + dpToPixels(res, padding[1]),
            bgPadding.right + dpToPixels(res, padding[2]),
            bgPadding.bottom + dpToPixels(res, padding[3])
        );
    }

    /**
     * Generate {@link FlexboxLayout.LayoutParams} for the given CSS rules.
     *
     * @param context    The view's context
     * @param baseParams Base layout parameters. Pass "null" if you want default rules to be used (WRAP_CONTENT)
     * @param rules      CSS-like rules
     * @return Layout parameters matching the CSS rules
     */
    public static FlexboxLayout.LayoutParams getFlexLayoutParams(
        Context context,
        FlexboxLayout.LayoutParams baseParams,
        Map<String, String> rules
    ) {
        FlexboxLayout.LayoutParams lp;
        if (baseParams != null) {
            lp = new FlexboxLayout.LayoutParams(baseParams);
        } else {
            lp =
                new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
        }

        // left, top, right, bottom
        Float[] margin = { 0f, 0f, 0f, 0f };

        final Resources res = context.getResources();

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("margin-left".equalsIgnoreCase(rule.getKey())) {
                margin[0] = optFloat(rule.getValue());
            } else if ("margin-top".equalsIgnoreCase(rule.getKey())) {
                margin[1] = optFloat(rule.getValue());
            } else if ("margin-right".equalsIgnoreCase(rule.getKey())) {
                margin[2] = optFloat(rule.getValue());
            } else if ("margin-bottom".equalsIgnoreCase(rule.getKey())) {
                margin[3] = optFloat(rule.getValue());
            } else if ("width".equalsIgnoreCase(rule.getKey())) {
                if ("100%".equals(rule.getValue())) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if ("auto".equals(rule.getValue())) {
                    lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    Float size = optFloat(rule.getValue());
                    if (size != null) {
                        lp.width = dpToPixels(res, size);
                    }
                }
            } else if ("height".equalsIgnoreCase(rule.getKey())) {
                if ("100%".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if ("auto".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    Float size = optFloat(rule.getValue());
                    if (size != null) {
                        lp.height = dpToPixels(res, size);
                    }
                }
            } else if ("align".equalsIgnoreCase(rule.getKey())) {
                if ("left".equals(rule.getValue())) {
                    lp.alignSelf = FlexboxLayout.LayoutParams.ALIGN_SELF_FLEX_START;
                } else if ("right".equals(rule.getValue())) {
                    lp.alignSelf = FlexboxLayout.LayoutParams.ALIGN_SELF_FLEX_END;
                } else if ("center".equals(rule.getValue())) {
                    lp.alignSelf = FlexboxLayout.LayoutParams.ALIGN_SELF_CENTER;
                } else if ("auto".equals(rule.getValue())) {
                    lp.alignSelf = FlexboxLayout.LayoutParams.ALIGN_SELF_AUTO;
                }
            } else if ("flex-grow".equalsIgnoreCase(rule.getKey())) {
                Float val = optFloat(rule.getValue());
                if (val != null) {
                    lp.flexGrow = val;
                }
            } else if ("flex-shrink".equalsIgnoreCase(rule.getKey())) {
                Float val = optFloat(rule.getValue());
                if (val != null) {
                    lp.flexShrink = val;
                }
            } else if ("flex-basis".equalsIgnoreCase(rule.getKey())) {
                Float val = optFloat(rule.getValue());
                if (val != null) {
                    lp.flexBasisPercent = val;
                }
            } else if ("flex-order".equalsIgnoreCase(rule.getKey())) {
                Integer val = optInt(rule.getValue());
                if (val != null) {
                    lp.order = val;
                }
            }
        }

        lp.setMargins(
            dpToPixels(res, margin[0]),
            dpToPixels(res, margin[1]),
            dpToPixels(res, margin[2]),
            dpToPixels(res, margin[3])
        );

        return lp;
    }

    /**
     * Generate {@link RelativeLayout.LayoutParams} for the given CSS rules.
     *
     * @param context        The view's context
     * @param base           Base layout parameters. Pass "null" if you want default rules to be used (MATCH_PARENT)
     * @param rules          CSS-like rules
     * @param relativeToView Sets if the alignments should be relative to a view. If null, _PARENT variants of the rules will be used when applicable
     * @return Layout parameters matching the CSS rules
     */
    public static PercentRelativeLayout.LayoutParams getRelativeLayoutParams(
        Context context,
        PercentRelativeLayout.LayoutParams base,
        Map<String, String> rules,
        int defaultHorizontalAlign,
        View relativeToView
    ) {
        PercentRelativeLayout.LayoutParams lp;
        if (base != null) {
            // Only copy the margin params
            lp = new PercentRelativeLayout.LayoutParams(base);
        } else {
            lp =
                new PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
        }
        boolean hAlignSet = false;

        // left, top, right, bottom
        Float[] margin = { 0f, 0f, 0f, 0f };

        final Resources res = context.getResources();

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("margin-left".equalsIgnoreCase(rule.getKey())) {
                margin[0] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-top".equalsIgnoreCase(rule.getKey())) {
                margin[1] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-right".equalsIgnoreCase(rule.getKey())) {
                margin[2] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-bottom".equalsIgnoreCase(rule.getKey())) {
                margin[3] = StyleHelper.optFloat(rule.getValue());
            } else if ("width".equalsIgnoreCase(rule.getKey()) || "size".equalsIgnoreCase(rule.getKey())) {
                if ("auto".equals(rule.getValue())) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                } else {
                    Float size = StyleHelper.optFloat(rule.getValue());
                    if (size != null) {
                        lp.width = StyleHelper.dpToPixels(res, size);
                    }
                }
            } else if ("height".equalsIgnoreCase(rule.getKey())) {
                if ("100%".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if ("fill".equals(rule.getValue())) {
                    // Like 100%, but does not contribute to size measurement
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    if (relativeToView != null) {
                        lp.addRule(RelativeLayout.ALIGN_TOP, relativeToView.getId());
                        lp.addRule(RelativeLayout.ALIGN_BOTTOM, relativeToView.getId());
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    }
                } else if ("auto".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    Float size = optFloat(rule.getValue());
                    if (size != null) {
                        lp.height = dpToPixels(res, size);
                    }
                }
            } else if ("align".equalsIgnoreCase(rule.getKey())) {
                if ("left".equals(rule.getValue())) {
                    if (relativeToView != null) {
                        lp.addRule(RelativeLayout.ALIGN_LEFT, relativeToView.getId());
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    }
                    hAlignSet = true;
                } else if ("right".equals(rule.getValue())) {
                    if (relativeToView != null) {
                        lp.addRule(RelativeLayout.ALIGN_RIGHT, relativeToView.getId());
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    }
                    hAlignSet = true;
                } else if ("center".equals(rule.getValue()) || "auto".equals(rule.getValue())) {
                    lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    hAlignSet = true;
                }
            } else if ("align-v".equalsIgnoreCase(rule.getKey()) || "vertical-align".equalsIgnoreCase(rule.getKey())) {
                if ("top".equals(rule.getValue())) {
                    if (relativeToView != null) {
                        lp.addRule(RelativeLayout.ALIGN_TOP, relativeToView.getId());
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    }
                } else if ("bottom".equals(rule.getValue())) {
                    if (relativeToView != null) {
                        lp.addRule(RelativeLayout.ALIGN_BOTTOM, relativeToView.getId());
                    } else {
                        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    }
                } else if ("center".equals(rule.getValue()) || "auto".equals(rule.getValue())) {
                    lp.addRule(RelativeLayout.CENTER_VERTICAL);
                }
            }
        }

        if (!hAlignSet) {
            lp.addRule(defaultHorizontalAlign);
        }

        lp.setMargins(
            StyleHelper.dpToPixels(res, margin[0]),
            StyleHelper.dpToPixels(res, margin[1]),
            StyleHelper.dpToPixels(res, margin[2]),
            StyleHelper.dpToPixels(res, margin[3])
        );

        return lp;
    }

    /**
     * Generate {@link FrameLayout.LayoutParams} for the given CSS rules.
     *
     * @param context The view's context
     * @param base    Base layout parameters. Pass "null" if you want default rules to be used (MATCH_PARENT)
     * @param rules   CSS-like rules
     * @return Layout parameters matching the CSS rules
     */
    @SuppressLint("RtlHardcoded")
    public static FrameLayout.LayoutParams getFrameLayoutParams(
        Context context,
        FrameLayout.LayoutParams base,
        Map<String, String> rules
    ) {
        FrameLayout.LayoutParams lp;
        if (base != null) {
            // The copy constructor is only available from API 19... lol.
            lp = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams) base);
            lp.gravity = base.gravity;
        } else {
            lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        lp.gravity = Gravity.RIGHT;

        // left, top, right, bottom
        Float[] margin = { 0f, 0f, 0f, 0f };

        final Resources res = context.getResources();

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("margin-left".equalsIgnoreCase(rule.getKey())) {
                margin[0] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-top".equalsIgnoreCase(rule.getKey())) {
                margin[1] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-right".equalsIgnoreCase(rule.getKey())) {
                margin[2] = StyleHelper.optFloat(rule.getValue());
            } else if ("margin-bottom".equalsIgnoreCase(rule.getKey())) {
                margin[3] = StyleHelper.optFloat(rule.getValue());
            } else if ("width".equalsIgnoreCase(rule.getKey()) || "size".equalsIgnoreCase(rule.getKey())) {
                if ("auto".equals(rule.getValue())) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                } else {
                    Float size = StyleHelper.optFloat(rule.getValue());
                    if (size != null) {
                        lp.width = StyleHelper.dpToPixels(res, size);
                    }
                }
            } else if ("height".equalsIgnoreCase(rule.getKey())) {
                if ("100%".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else if ("auto".equals(rule.getValue())) {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                } else {
                    Float size = optFloat(rule.getValue());
                    if (size != null) {
                        lp.height = dpToPixels(res, size);
                    }
                }
            } else if ("align".equalsIgnoreCase(rule.getKey())) {
                if ("left".equals(rule.getValue())) {
                    lp.gravity = Gravity.LEFT;
                } else if ("right".equals(rule.getValue())) {
                    lp.gravity = Gravity.RIGHT;
                } else if ("center".equals(rule.getValue())) {
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                } else if ("auto".equals(rule.getValue())) {
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                }
            } else if ("vertical-align".equalsIgnoreCase(rule.getKey())) {
                if ("top".equals(rule.getValue())) {
                    lp.gravity |= Gravity.TOP;
                } else if ("bottom".equals(rule.getValue())) {
                    lp.gravity |= Gravity.BOTTOM;
                } else if ("center".equals(rule.getValue())) {
                    lp.gravity |= Gravity.CENTER_VERTICAL;
                }
            }
        }

        lp.setMargins(
            StyleHelper.dpToPixels(res, margin[0]),
            StyleHelper.dpToPixels(res, margin[1]),
            StyleHelper.dpToPixels(res, margin[2]),
            StyleHelper.dpToPixels(res, margin[3])
        );

        return lp;
    }

    /**
     * Converts a density-independent pixels measurement value to pixels.
     *
     * @param resources View's resources
     * @param dp        Density-independent pixel value
     * @return The corresponding pixel size for the current resources density
     */
    public static int dpToPixels(Resources resources, Float dp) {
        if (dp == null) {
            return 0;
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }

    /**
     * Converts a pixels measurement value to density-independent pixels.
     *
     * @param resources View's resources
     * @param px        Pixel value
     * @return The corresponding density-independent pixel size for the current resources density
     */
    public static float pixelsToDp(Resources resources, Float px) {
        if (px == null) {
            return 0;
        }
        return px / resources.getDisplayMetrics().density;
    }

    /**
     * Returns the integer value of a string.
     * Doesn't throw an exception if it fails.
     *
     * @param stringValue String to parse
     * @return Integer value if possible, null otherwise
     */
    public static Integer optInt(String stringValue) {
        if (stringValue == null) {
            return null;
        }

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ignored) {}

        return null;
    }

    /**
     * Returns the float value of a string.
     * Doesn't throw an exception if it fails.
     *
     * @param stringValue String to parse
     * @return Float value if possible, null otherwise
     */
    public static Float optFloat(String stringValue) {
        if (stringValue == null) {
            return null;
        }

        try {
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException ignored) {}

        return null;
    }

    /**
     * Parse a color. Accepts "#rrggbb", "#aarrggbb" or "transparent".
     * Will fallback on {@link Color#TRANSPARENT} when an error occurs.
     *
     * @param color Color string to parse
     * @return Parsed color, or {@link Color#TRANSPARENT}
     */
    public static int parseColor(String color) {
        if (TextUtils.isEmpty(color) || "transparent".equalsIgnoreCase(color)) {
            return Color.TRANSPARENT;
        }
        try {
            return Color.parseColor(color.trim());
        } catch (IllegalArgumentException e) {
            return Color.TRANSPARENT;
        }
    }

    /**
     * Darkens a color, using HSL
     *
     * @param color Color to be darkened
     * @return Darkened color
     */
    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(color, hsv);
    }

    /**
     * Get a pressable drawable: a state drawable that is either a darkened drawable or a RippleDrawable depending on the current API.
     * Used for buttons.
     *
     * @param baseDrawable Drawable to wrap
     * @return A drawable that will be darkened on press
     */
    private static Drawable getPressableGradientDrawable(PositionableGradientDrawable baseDrawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Generate a mask using the same shape. That way we can ripple a transparent drawable easily
            PositionableGradientDrawable mask = (PositionableGradientDrawable) baseDrawable
                .getConstantState()
                .newDrawable()
                .mutate();
            mask.setColor(Color.WHITE);
            return new RippleDrawable(ColorStateList.valueOf(RIPPLE_COLOR), baseDrawable, mask);
        } else {
            // Mutate the original drawable so we can keep the corner radius
            PositionableGradientDrawable darkDrawable = (PositionableGradientDrawable) baseDrawable
                .getConstantState()
                .newDrawable()
                .mutate();
            darkDrawable.setStroke(0, Color.TRANSPARENT);
            darkDrawable.setColor(RIPPLE_COLOR);

            LayerDrawable ld = new LayerDrawable(new Drawable[] { baseDrawable, darkDrawable });

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[] { android.R.attr.state_pressed }, ld);
            stateListDrawable.addState(new int[] { android.R.attr.state_focused }, ld);
            stateListDrawable.addState(new int[] { android.R.attr.state_activated }, ld);
            stateListDrawable.addState(new int[] {}, baseDrawable);

            return stateListDrawable;
        }
    }
}
