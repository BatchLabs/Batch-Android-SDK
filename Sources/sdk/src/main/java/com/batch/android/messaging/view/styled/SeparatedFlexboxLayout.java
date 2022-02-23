package com.batch.android.messaging.view.styled;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.batch.android.messaging.view.DelegatedTouchEventViewGroup;
import com.batch.android.messaging.view.FlexboxLayout;
import com.batch.android.messaging.view.helper.StyleHelper;
import java.util.Map;

/**
 * A custom Flexbox layout that's stylable and automatically adds separators
 * <p>
 * This class is _not_ meant to be used via Android tools, so it does not have the standard View constructors
 */
@SuppressLint("ViewConstructor")
public class SeparatedFlexboxLayout extends FlexboxLayout implements Styleable, DelegatedTouchEventViewGroup {

    private DelegatedTouchEventViewGroup.Delegate delegate;
    private String separatorPrefix;
    private SeparatorStyleProvider styleProvider;

    private int separatorCount = 0;

    public SeparatedFlexboxLayout(Context context, String separatorPrefix, SeparatorStyleProvider styleProvider) {
        super(context);
        this.separatorPrefix = separatorPrefix;
        this.styleProvider = styleProvider;

        if (TextUtils.isEmpty(separatorPrefix)) {
            throw new IllegalArgumentException("separatorPrefix cannot be null or empty");
        }

        if (styleProvider == null) {
            throw new IllegalArgumentException("styleProvider cannot be null");
        }
    }

    @Override
    public void addView(View child) {
        // Automatically add the first separator
        if (separatorCount == 0) {
            addSeparator();
        }
        super.addView(child);
        addSeparator();
    }

    private void internalAddView(View child) {
        super.addView(child);
    }

    public boolean isHorizontal() {
        int flexDirection = getFlexDirection();
        return (
            flexDirection == FlexboxLayout.FLEX_DIRECTION_ROW ||
            flexDirection == FlexboxLayout.FLEX_DIRECTION_ROW_REVERSE
        );
    }

    public String getSeparatorPrefix() {
        return separatorPrefix;
    }

    public void addSeparator() {
        //Separators are added with a default width of 10dp and height of 0
        //That way, just like iOS, you can make them appear just by setting the height

        final Map<String, String> rules = styleProvider.getRulesForSeparator(
            this,
            separatorPrefix + "-sep-" + separatorCount
        );
        final SeparatorView separator = new SeparatorView(getContext());
        separator.setLayoutParams(
            StyleHelper.getFlexLayoutParams(
                getContext(),
                new FlexboxLayout.LayoutParams(StyleHelper.dpToPixels(getResources(), 10f), 0),
                rules
            )
        );
        separator.applyStyleRules(rules);
        internalAddView(separator);

        separatorCount++;
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        StyleHelper.applyCommonRules(this, rules);

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("flex-justify".equalsIgnoreCase(rule.getKey())) {
                if ("center".equals(rule.getValue())) {
                    setJustifyContent(JUSTIFY_CONTENT_CENTER);
                } else if ("end".equals(rule.getValue())) {
                    setJustifyContent(JUSTIFY_CONTENT_FLEX_END);
                } else if ("start".equals(rule.getValue())) {
                    setJustifyContent(JUSTIFY_CONTENT_FLEX_START);
                } else if ("space-around".equals(rule.getValue())) {
                    setJustifyContent(JUSTIFY_CONTENT_SPACE_AROUND);
                } else if ("space-between".equals(rule.getValue())) {
                    setJustifyContent(JUSTIFY_CONTENT_SPACE_BETWEEN);
                }
            } else if ("flex-align-items".equalsIgnoreCase(rule.getKey())) {
                if ("baseline".equals(rule.getValue())) {
                    setAlignItems(ALIGN_ITEMS_BASELINE);
                } else if ("center".equals(rule.getValue())) {
                    setAlignItems(ALIGN_ITEMS_CENTER);
                } else if ("stretch".equals(rule.getValue())) {
                    setAlignItems(ALIGN_ITEMS_STRETCH);
                } else if ("end".equals(rule.getValue())) {
                    setAlignItems(ALIGN_ITEMS_FLEX_END);
                } else if ("start".equals(rule.getValue())) {
                    setAlignItems(ALIGN_ITEMS_FLEX_START);
                }
            } else if ("flex-align-content".equalsIgnoreCase(rule.getKey())) {
                if ("space-around".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_SPACE_AROUND);
                } else if ("space-between".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_SPACE_BETWEEN);
                } else if ("center".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_CENTER);
                } else if ("stretch".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_STRETCH);
                } else if ("end".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_FLEX_END);
                } else if ("start".equals(rule.getValue())) {
                    setAlignContent(ALIGN_CONTENT_FLEX_START);
                }
            } else if ("flex-direction".equalsIgnoreCase(rule.getKey())) {
                if ("row".equals(rule.getValue())) {
                    setFlexDirection(FLEX_DIRECTION_ROW);
                } else if ("row-reverse".equals(rule.getValue())) {
                    setFlexDirection(FLEX_DIRECTION_ROW_REVERSE);
                } else if ("column".equals(rule.getValue())) {
                    setFlexDirection(FLEX_DIRECTION_COLUMN);
                } else if ("column-reverse".equals(rule.getValue())) {
                    setFlexDirection(FLEX_DIRECTION_COLUMN_REVERSE);
                }
            }
        }
    }

    public interface SeparatorStyleProvider {
        Map<String, String> getRulesForSeparator(SeparatedFlexboxLayout layout, String separatorID);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (delegate != null) {
            return delegate.onInterceptTouchEvent(ev, this);
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (delegate != null) {
            return delegate.onTouchEvent(ev, this, false);
        } else {
            return super.onTouchEvent(ev);
        }
    }

    @Override
    public void setTouchEventDelegate(@Nullable Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean superOnInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean superOnTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }
}
