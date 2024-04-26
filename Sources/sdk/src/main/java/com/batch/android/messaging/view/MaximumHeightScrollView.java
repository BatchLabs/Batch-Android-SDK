package com.batch.android.messaging.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * A {@link ScrollView} that supports setting a maximum height
 */
public class MaximumHeightScrollView extends ScrollView {

    private int maxHeightPx = 0;

    public MaximumHeightScrollView(Context context) {
        super(context);
    }

    public MaximumHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaximumHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MaximumHeightScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setMaxHeight(int sizeDp) {
        maxHeightPx = (int) (sizeDp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int finalHeightMeasureSpec = heightMeasureSpec;
        if (maxHeightPx != 0 && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            int wantedHeight = MeasureSpec.getSize(heightMeasureSpec);
            if (wantedHeight > maxHeightPx) {
                finalHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, finalHeightMeasureSpec);
    }
}
