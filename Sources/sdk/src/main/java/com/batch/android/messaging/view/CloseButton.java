package com.batch.android.messaging.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import androidx.annotation.Keep;
import com.batch.android.core.Logger;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.styled.Styleable;
import java.util.Map;

/**
 * Close button view. Draws its content itself so it doesn't need to embed a drawable into the target app.
 *
 */
public class CloseButton extends View implements Styleable {

    private static final String TAG = "CloseButton";

    private static final int DEFAULT_SIZE_DP = 32;
    private static final int DEFAULT_PADDING_DP = 10;

    private static final int UNSCALED_GLYPH_PADDING_PX = 10;
    private static final int UNSCALED_GLYPH_WIDTH_PX = 2;

    private int padding = 0;

    private int backgroundColor = Color.BLACK;

    private int glyphColor = Color.WHITE;

    private int glyphPadding = -1;

    private int glyphWidth = -1;

    protected float countdownProgress = 0;

    private int computedGlyphPadding = -1;

    private Paint backgroundPaint;

    private Paint glyphPaint;

    private Paint borderPaint;

    private Drawable foregoundDrawable;

    private RectF countdownOval = new RectF();

    private RectF borderOval = new RectF();

    private boolean showBorder = false;

    public CloseButton(Context context) {
        super(context);
        init();
    }

    public CloseButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CloseButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CloseButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(
                new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setOval(
                            getPadding(),
                            getPadding(),
                            view.getWidth() - getPadding(),
                            view.getHeight() - getPadding()
                        );
                    }
                }
            );

            setClipToOutline(true);
        }

        refreshPaint();
        //TODO: How to translate this?
        setContentDescription("Close button");

        // This views comme with 10dp of padding by default
        setPadding(StyleHelper.dpToPixels(getResources(), (float) DEFAULT_PADDING_DP));

        // Set Android's default selectable item drawable (overlay on < L, Ripple on newer)
        TypedArray ta = getContext().obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
        foregoundDrawable = ta.getDrawable(0);
        if (foregoundDrawable != null) {
            // Setting the callback ensures that the drawable animation works
            foregoundDrawable.setCallback(this);
        }
        ta.recycle();
    }

    public void refreshPaint() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        glyphPaint = new Paint();
        glyphPaint.setStyle(Paint.Style.STROKE);
        glyphPaint.setColor(glyphColor);
        glyphPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(glyphColor);
        borderPaint.setAntiAlias(true);

        // We need to call that as this sets the strokeWidth of the paint
        recomputeMetrics();
        invalidate();
    }

    public void recomputeMetrics() {
        float density = getResources().getDisplayMetrics().density;
        Resources resources = getResources();
        int padding = getPadding();
        float viewScalingRatio = (getWidth() - padding * 2) / (DEFAULT_SIZE_DP * density);
        computedGlyphPadding =
            glyphPadding >= 0 ? glyphPadding : (int) (UNSCALED_GLYPH_PADDING_PX * density * viewScalingRatio);

        glyphPaint.setStrokeWidth(
            glyphWidth >= 0 ? glyphWidth : (int) (UNSCALED_GLYPH_WIDTH_PX * density * viewScalingRatio)
        );

        borderPaint.setStrokeWidth(
            Math.max(
                glyphPaint.getStrokeWidth() + StyleHelper.dpToPixels(resources, 1.0f),
                StyleHelper.dpToPixels(resources, 2.0f)
            )
        );

        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        int radius = (int) (halfHeight - padding - glyphPaint.getStrokeWidth());

        countdownOval.set(halfWidth - radius, halfHeight - radius, halfWidth + radius, halfHeight + radius);

        borderOval.set(padding, padding, getWidth() - padding, getHeight() - padding);
    }

    @Override
    public void setBackgroundColor(int color) {
        backgroundColor = color;
        refreshPaint();
    }

    public void setGlyphColor(int color) {
        glyphColor = color;
        refreshPaint();
    }

    public void setForegoundDrawable(Drawable d) {
        foregoundDrawable = d;
        invalidate();
    }

    /**
     * Default to false. Set to true to display a 1.5-px wide border the color of the glyph @ 75% opacity.
     */
    public void setShowBorder(boolean show) {
        showBorder = show;
        invalidate();
    }

    /**
     * Set the padding
     * Use {@link #setPadding(int)}
     */
    @Override
    @Deprecated
    public void setPadding(int left, int top, int right, int bottom) {
        setPadding(left);
    }

    public void setPadding(int padding) {
        // This view only supports having the same padding everywhere
        this.padding = padding;
        recomputeMetrics();
    }

    public int getPadding() {
        return this.padding;
    }

    /**
     * Sets the countdown progress (if any).
     * Progress is represented as a float going from 0.0 to 1.0
     */
    @Keep
    public void setCountdownProgress(float countdownProgress) {
        this.countdownProgress = Math.max(0.0f, Math.min(1.0f, countdownProgress));
        invalidate();
    }

    /**
     * Set the glyph padding
     *
     * @param glyphPadding Glyph padding, in pixels. Do not forget to scale it! -1 to use the default.
     */
    public void setGlyphPadding(int glyphPadding) {
        this.glyphPadding = glyphPadding;
        recomputeMetrics();
        invalidate();
    }

    /**
     * Set the glyph width
     *
     * @param glyphWidth Glyph width, in pixels. Do not forget to scale it! -1 to use the default.
     */
    public void setGlyphWidth(int glyphWidth) {
        this.glyphWidth = glyphWidth;
        recomputeMetrics();
        invalidate();
    }

    @Override
    public ViewOutlineProvider getOutlineProvider() {
        return super.getOutlineProvider();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recomputeMetrics();

        if (foregoundDrawable != null) {
            foregoundDrawable.setBounds(0, 0, w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int halfHeight = getHeight() / 2;
        canvas.drawCircle(getWidth() / 2, halfHeight, halfHeight - getPadding(), backgroundPaint);
        canvas.drawLine(
            computedGlyphPadding + getPadding(),
            computedGlyphPadding + getPadding(),
            getWidth() - computedGlyphPadding - getPadding(),
            getHeight() - computedGlyphPadding - getPadding(),
            glyphPaint
        );
        canvas.drawLine(
            getWidth() - computedGlyphPadding - getPadding(),
            computedGlyphPadding + getPadding(),
            computedGlyphPadding + getPadding(),
            getHeight() - computedGlyphPadding - getPadding(),
            glyphPaint
        );

        if (showBorder) {
            canvas.drawArc(borderOval, 0, 360.0f, false, borderPaint);
        }

        if (countdownProgress > 0) {
            canvas.drawArc(countdownOval, -90, 360.0f * countdownProgress, false, glyphPaint);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Update the drawable in draw() rather than onDraw() to support animated drawables
        if (foregoundDrawable != null) {
            foregoundDrawable.draw(canvas);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;

        // Since the ratio of this view is 1:1, we ignore the height

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);

        // Fall back on default size if none was specified
        if (widthMode == MeasureSpec.AT_MOST) {
            width = (int) Math.min(DEFAULT_SIZE_DP * density + getPadding() * 2, width);
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            width = (int) (DEFAULT_SIZE_DP * density + getPadding() * 2);
        }

        //noinspection SuspiciousNameCombination
        setMeasuredDimension(width, width);
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        final Resources res = getResources();
        float density = res.getDisplayMetrics().density;

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("background-color".equalsIgnoreCase(rule.getKey())) {
                try {
                    setBackgroundColor(StyleHelper.parseColor(rule.getValue()));
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable background color (" + rule.getValue() + ")", e);
                }
            } else if ("color".equalsIgnoreCase(rule.getKey())) {
                try {
                    setGlyphColor(StyleHelper.parseColor(rule.getValue()));
                } catch (IllegalArgumentException e) {
                    Logger.internal(TAG, "Unparsable glyph color (" + rule.getValue() + ")", e);
                }
            } else if ("glyph-padding".equalsIgnoreCase(rule.getKey())) {
                Float val = StyleHelper.optFloat(rule.getValue());
                if (val != null) {
                    setGlyphPadding((int) (density * val.intValue()));
                }
            } else if ("glyph-width".equalsIgnoreCase(rule.getKey())) {
                Float val = StyleHelper.optFloat(rule.getValue());
                if (val != null) {
                    setGlyphWidth((int) (density * val.intValue()));
                }
            } else if ("elevation".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = StyleHelper.optFloat(rule.getValue());
                    if (val != null) {
                        setElevation(StyleHelper.dpToPixels(res, val));
                    }
                }
            } else if ("z-index".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = StyleHelper.optFloat(rule.getValue());
                    if (val != null) {
                        setZ(StyleHelper.dpToPixels(res, val));
                    }
                }
            }
        }
    }

    //region Foreground drawable support

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (foregoundDrawable != null) {
            foregoundDrawable.setState(getDrawableState());
        }

        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == foregoundDrawable || super.verifyDrawable(who);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (foregoundDrawable != null) {
            foregoundDrawable.jumpToCurrentState();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (foregoundDrawable != null) {
            foregoundDrawable.setHotspot(x, y);
        }
    }
    //endregion
}
