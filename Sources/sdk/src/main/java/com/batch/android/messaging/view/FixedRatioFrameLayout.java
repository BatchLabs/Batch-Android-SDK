package com.batch.android.messaging.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.batch.android.messaging.Size2D;

/**
 * View that will optionally size itself to match a target size while preserving its ratio
 */
public class FixedRatioFrameLayout extends FrameLayout {

    @Nullable
    private Size2D targetSize;

    public FixedRatioFrameLayout(@NonNull Context context, @Nullable Size2D targetSize) {
        super(context);
        init(targetSize);
    }

    public FixedRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedRatioFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FixedRatioFrameLayout(
        @NonNull Context context,
        @Nullable AttributeSet attrs,
        int defStyleAttr,
        int defStyleRes
    ) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@Nullable Size2D targetSize) {
        setTargetSize(targetSize);
    }

    public void setTargetSize(@Nullable Size2D targetSize) {
        if (targetSize != null && (targetSize.width <= 1 || targetSize.height <= 1)) {
            targetSize = null;
        }

        this.targetSize = targetSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (targetSize == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

        // As Android gives us our largest dimensions (this layout is NOT meant to be used with anything else than MATCH_PARENT)
        // Calculate both scaled sizes and pick the smallest, as we should not go over width/heightMeasureSpec
        // Don't check if the mode is AT_MOST as other cases are unsupported

        double scaledWidth = measuredHeight * ((double) targetSize.width / targetSize.height);
        double scaledHeight = measuredWidth * ((double) targetSize.height / targetSize.width);

        int computedWidth, computedHeight;

        if (scaledWidth > measuredWidth) {
            // Scaling by width makes us end up going over our bounds, so we actually needed to scale
            // the height
            computedWidth = measuredWidth;
            computedHeight = (int) scaledHeight;
        } else {
            computedWidth = (int) scaledWidth;
            computedHeight = measuredHeight;
        }

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(computedWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(computedHeight, MeasureSpec.EXACTLY)
        );
    }
}
