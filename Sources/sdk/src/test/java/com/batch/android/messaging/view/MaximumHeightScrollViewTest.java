package com.batch.android.messaging.view;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View.MeasureSpec;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MaximumHeightScrollViewTest {

    @Test
    @Config(qualifiers = "xhdpi")
    public void testMaxHeight() {
        int expectedWidth = 100;
        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(expectedWidth, MeasureSpec.EXACTLY);

        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(5000, MeasureSpec.AT_MOST);

        final Context context = ApplicationProvider.getApplicationContext();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        final MaximumHeightScrollView view = new MaximumHeightScrollView(context);

        view.measure(widthMeasureSpec, heightMeasureSpec);
        Assert.assertEquals(expectedWidth, view.getMeasuredWidth());
        Assert.assertEquals(0, view.getMeasuredHeight());

        int maxHeightDp = 200;
        int maxHeightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxHeightDp, dm);

        view.setMaxHeight(maxHeightDp);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        Assert.assertEquals(expectedWidth, view.getMeasuredWidth());
        Assert.assertEquals(maxHeightPx, view.getMeasuredHeight(), 0);

        maxHeightDp = 400;

        view.setMaxHeight(maxHeightDp);
        view.measure(widthMeasureSpec, heightMeasureSpec);
        Assert.assertEquals(expectedWidth, view.getMeasuredWidth());
        Assert.assertEquals(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, maxHeightDp, dm),
            view.getMeasuredHeight(),
            0
        );
    }
}
