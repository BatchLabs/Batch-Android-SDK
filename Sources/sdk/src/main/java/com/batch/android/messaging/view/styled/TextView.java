package com.batch.android.messaging.view.styled;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;
import com.batch.android.core.Logger;
import com.batch.android.messaging.view.helper.StyleHelper;
import java.util.Map;

/**
 * Styleable TextView, based on appcompat-v7's TextView
 *
 */
@SuppressLint("AppCompatCustomView")
public class TextView extends android.widget.TextView implements Styleable {

    private static final String TAG = "TextView";

    public static Typeface typefaceOverride = null;

    public static Typeface boldTypefaceOverride = null;

    public TextView(Context context) {
        super(context);
    }

    public TextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        applyStyleRules(this, rules);
    }

    /**
     * Used to factor the implementation of the textview styling and use it in buttons
     *
     * @param targetTextView
     */
    @SuppressLint("RtlHardcoded")
    protected static void applyStyleRules(android.widget.TextView targetTextView, Map<String, String> rules) {
        StyleHelper.applyCommonRules(targetTextView, rules);

        Integer gravity = null;

        boolean bold = false;
        boolean italic = false;
        String customTypeface = null;
        float lineSpacingAdd = 0;
        float lineSpacingMultiply = 0;

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("color".equalsIgnoreCase(rule.getKey())) {
                try {
                    targetTextView.setTextColor(Color.parseColor(rule.getValue()));
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable text color (" + rule.getValue() + ")", e);
                }
            } else if ("text-align".equalsIgnoreCase(rule.getKey())) {
                if ("left".equals(rule.getValue())) {
                    gravity = Gravity.LEFT;
                } else if ("right".equals(rule.getValue())) {
                    gravity = Gravity.RIGHT;
                } else if ("center".equals(rule.getValue())) {
                    gravity = Gravity.CENTER_HORIZONTAL;
                } else if ("auto".equals(rule.getValue())) {
                    gravity = null;
                }
            } else if ("font-weight".equalsIgnoreCase(rule.getKey())) {
                if ("bold".equals(rule.getValue()) || "700".equals(rule.getValue())) {
                    bold = true;
                }
            } else if ("font-style".equalsIgnoreCase(rule.getKey())) {
                if ("italic".equals(rule.getValue())) {
                    italic = true;
                }
            } else if ("font".equalsIgnoreCase(rule.getKey())) {
                customTypeface = rule.getValue();
            } else if ("font-size".equalsIgnoreCase(rule.getKey())) {
                String fontSizeString = rule.getValue();
                boolean isSizeSP = fontSizeString.endsWith("sp");
                if (isSizeSP) {
                    fontSizeString = fontSizeString.replace("sp", "");
                }

                Float fontSize = StyleHelper.optFloat(fontSizeString);
                if (fontSize != null) {
                    targetTextView.setTextSize(
                        isSizeSP ? TypedValue.COMPLEX_UNIT_SP : TypedValue.COMPLEX_UNIT_DIP,
                        fontSize
                    );
                }
            } else if ("letter-spacing".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float spacing = StyleHelper.optFloat(rule.getValue());
                    if (spacing != null) {
                        targetTextView.setLetterSpacing(spacing);
                    }
                }
            } else if ("line-height".equalsIgnoreCase(rule.getKey())) {
                Float height = StyleHelper.optFloat(rule.getValue());
                if (height != null) {
                    lineSpacingMultiply = height;
                }
            } else if ("line-spacing".equalsIgnoreCase(rule.getKey())) {
                Float spacing = StyleHelper.optFloat(rule.getValue());
                if (spacing != null) {
                    lineSpacingAdd = spacing;
                }
            } else if ("balanced".equalsIgnoreCase(rule.getKey()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Float balanced = StyleHelper.optFloat(rule.getValue());
                if (balanced != null) {
                    //noinspection AndroidLintWrongConstant
                    targetTextView.setBreakStrategy(
                        balanced > 0 ? Layout.BREAK_STRATEGY_BALANCED : Layout.BREAK_STRATEGY_SIMPLE
                    );
                }
            }
        }

        int finalGravity = Gravity.CENTER;
        if (gravity != null) {
            finalGravity = Gravity.CENTER_VERTICAL | gravity;
        }

        targetTextView.setGravity(finalGravity);

        int typefaceStyle = Typeface.NORMAL;

        if (bold) {
            if (italic) {
                typefaceStyle = Typeface.BOLD_ITALIC;
            } else {
                typefaceStyle = Typeface.BOLD;
            }
        } else if (italic) {
            typefaceStyle = Typeface.ITALIC;
        }

        Typeface baseTypeface = typefaceOverride;

        if (bold) {
            baseTypeface = boldTypefaceOverride;
        }

        Typeface computedTypeface;

        if (baseTypeface != null) {
            computedTypeface = Typeface.create(baseTypeface, typefaceStyle);
        } else {
            computedTypeface = Typeface.create(customTypeface, typefaceStyle);
        }

        targetTextView.setTypeface(computedTypeface);

        if (lineSpacingMultiply != 0 || lineSpacingAdd != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                targetTextView.setLineSpacing(
                    lineSpacingAdd != 0 ? lineSpacingAdd : targetTextView.getLineSpacingExtra(),
                    lineSpacingMultiply != 0 ? lineSpacingMultiply : targetTextView.getLineSpacingMultiplier()
                );
            } else {
                targetTextView.setLineSpacing(lineSpacingAdd, lineSpacingMultiply);
            }
        }
    }

    public void makeScrollable() {
        // Out of the box, inertia scrolling isn't supported so do it ourselves
        setVerticalScrollBarEnabled(true);
        setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        setMovementMethod(new ScrollingMovementMethod());
        final Scroller scroller = new Scroller(getContext());
        setScroller(scroller);
        //noinspection AndroidLintClickableViewAccessibility
        setOnTouchListener(
            new View.OnTouchListener() {
                GestureDetector gesture = new GestureDetector(
                    getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            int maxScrollY =
                                getLayout().getHeight() - getHeight() + getPaddingTop() + getPaddingBottom();
                            // The layout is the one that is able to give us the real height of the textview
                            scroller.fling(0, getScrollY(), 0, (int) -velocityY, 0, 0, 0, maxScrollY);
                            return super.onFling(e1, e2, velocityX, velocityY);
                        }
                    }
                );

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Only bother scrolling and disabling the swipe to dismiss if the TextView
                    // actually scrolls
                    if (getLayout().getHeight() > getHeight()) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        gesture.onTouchEvent(event);
                    }
                    return v.onTouchEvent(event);
                }
            }
        );
    }
}
