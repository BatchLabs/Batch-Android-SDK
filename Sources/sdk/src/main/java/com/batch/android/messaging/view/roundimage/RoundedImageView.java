/*
 * Copyright (C) 2015 Vincent Mi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by Batch to add CSS and Blurring support
 */

package com.batch.android.messaging.view.roundimage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.styled.Styleable;
import java.util.Map;

/**
 * Rounded Image View. Also supports blurring, only for images set via setImageBitmap.
 */
@SuppressLint("AppCompatCustomView")
@SuppressWarnings("UnusedDeclaration")
public class RoundedImageView extends ImageView implements Styleable {

    // Constants for tile mode attributes
    private static final int TILE_MODE_UNDEFINED = -2;
    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_REPEAT = 1;
    private static final int TILE_MODE_MIRROR = 2;

    private static final String TAG = "RoundedImageView";
    public static final float DEFAULT_RADIUS = 0f;
    public static final float DEFAULT_BORDER_WIDTH = 0f;
    public static final Shader.TileMode DEFAULT_TILE_MODE = Shader.TileMode.CLAMP;
    private static final ScaleType[] SCALE_TYPES = {
        ScaleType.MATRIX,
        ScaleType.FIT_XY,
        ScaleType.FIT_START,
        ScaleType.FIT_CENTER,
        ScaleType.FIT_END,
        ScaleType.CENTER,
        ScaleType.CENTER_CROP,
        ScaleType.CENTER_INSIDE,
    };

    private final float[] mCornerRadii = new float[] { DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS };

    private Drawable mBackgroundDrawable;
    private ColorStateList mBorderColor = ColorStateList.valueOf(RoundedDrawable.DEFAULT_BORDER_COLOR);
    private float mBorderWidth = DEFAULT_BORDER_WIDTH;
    private ColorFilter mColorFilter = null;
    private boolean mColorMod = false;
    private Drawable mDrawable;
    private boolean mHasColorFilter = false;
    private boolean mIsOval = false;
    private boolean mMutateBackground = false;
    private int mResource;
    private int mBackgroundResource;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;
    private Shader.TileMode mTileModeX = DEFAULT_TILE_MODE;
    private Shader.TileMode mTileModeY = DEFAULT_TILE_MODE;

    // We could implement the CSS corners using mCornerRadii and the drawable's enabled rounded corner, but we won't do that
    private float cornerRadius = 0;
    // [ topLeft, topRight, bottomLeft, bottomRight ]
    private final boolean[] roundedCorners = new boolean[] { true, true, true, true };

    public RoundedImageView(Context context) {
        super(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private static Shader.TileMode parseTileMode(int tileMode) {
        switch (tileMode) {
            case TILE_MODE_CLAMP:
                return Shader.TileMode.CLAMP;
            case TILE_MODE_REPEAT:
                return Shader.TileMode.REPEAT;
            case TILE_MODE_MIRROR:
                return Shader.TileMode.MIRROR;
            default:
                return null;
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    public ScaleType getScaleType() {
        return mScaleType;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        assert scaleType != null;

        if (mScaleType != scaleType) {
            mScaleType = scaleType;
            super.setScaleType(scaleType);

            updateDrawableAttrs();
            updateBackgroundDrawableAttrs(false);
            invalidate();
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mResource = 0;
        mDrawable = RoundedDrawable.fromDrawable(drawable);
        updateDrawableAttrs();
        super.setImageDrawable(mDrawable);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        mResource = 0;

        mDrawable = RoundedDrawable.fromBitmap(bm);
        updateDrawableAttrs();
        super.setImageDrawable(mDrawable);
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        if (mResource != resId) {
            mResource = resId;
            mDrawable = resolveResource();
            updateDrawableAttrs();
            super.setImageDrawable(mDrawable);
        }
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        setImageDrawable(getDrawable());
    }

    private Drawable resolveResource() {
        Resources rsrc = getResources();
        if (rsrc == null) {
            return null;
        }

        Drawable d = null;

        if (mResource != 0) {
            try {
                d = rsrc.getDrawable(mResource);
            } catch (Exception e) {
                Log.w(TAG, "Unable to find resource: " + mResource, e);
                // Don't try again.
                mResource = 0;
            }
        }
        return RoundedDrawable.fromDrawable(d);
    }

    @Override
    public void setBackground(Drawable background) {
        setBackgroundDrawable(background);
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        if (mBackgroundResource != resId) {
            mBackgroundResource = resId;
            mBackgroundDrawable = resolveBackgroundResource();
            setBackgroundDrawable(mBackgroundDrawable);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundDrawable = new ColorDrawable(color);
        setBackgroundDrawable(mBackgroundDrawable);
    }

    private Drawable resolveBackgroundResource() {
        Resources rsrc = getResources();
        if (rsrc == null) {
            return null;
        }

        Drawable d = null;

        if (mBackgroundResource != 0) {
            try {
                d = rsrc.getDrawable(mBackgroundResource);
            } catch (Exception e) {
                Log.w(TAG, "Unable to find resource: " + mBackgroundResource, e);
                // Don't try again.
                mBackgroundResource = 0;
            }
        }
        return RoundedDrawable.fromDrawable(d);
    }

    private void updateDrawableAttrs() {
        updateAttrs(mDrawable, mScaleType);
    }

    private void updateBackgroundDrawableAttrs(boolean convert) {
        if (mMutateBackground) {
            if (convert) {
                mBackgroundDrawable = RoundedDrawable.fromDrawable(mBackgroundDrawable);
            }
            updateAttrs(mBackgroundDrawable, ScaleType.FIT_XY);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mColorFilter != cf) {
            mColorFilter = cf;
            mHasColorFilter = true;
            mColorMod = true;
            applyColorMod();
            invalidate();
        }
    }

    private void applyColorMod() {
        // Only mutate and apply when modifications have occurred. This should
        // not reset the mColorMod flag, since these filters need to be
        // re-applied if the Drawable is changed.
        if (mDrawable != null && mColorMod) {
            mDrawable = mDrawable.mutate();
            if (mHasColorFilter) {
                mDrawable.setColorFilter(mColorFilter);
            }
            //mDrawable.setXfermode(mXfermode);
            //mDrawable.setAlpha(mAlpha * mViewAlphaScale >> 8);
        }
    }

    private void updateAttrs(Drawable drawable, ScaleType scaleType) {
        if (drawable == null) {
            return;
        }

        if (drawable instanceof RoundedDrawable) {
            ((RoundedDrawable) drawable).setScaleType(scaleType)
                .setBorderWidth(mBorderWidth)
                .setBorderColor(mBorderColor)
                .setOval(mIsOval)
                .setTileModeX(mTileModeX)
                .setTileModeY(mTileModeY);

            if (mCornerRadii != null) {
                ((RoundedDrawable) drawable).setCornerRadius(
                        mCornerRadii[Corner.TOP_LEFT],
                        mCornerRadii[Corner.TOP_RIGHT],
                        mCornerRadii[Corner.BOTTOM_RIGHT],
                        mCornerRadii[Corner.BOTTOM_LEFT]
                    );
            }

            applyColorMod();
        } else if (drawable instanceof LayerDrawable) {
            // loop through layers to and set drawable attrs
            LayerDrawable ld = ((LayerDrawable) drawable);
            for (int i = 0, layers = ld.getNumberOfLayers(); i < layers; i++) {
                updateAttrs(ld.getDrawable(i), scaleType);
            }
        }
    }

    @Override
    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        mBackgroundDrawable = background;
        updateBackgroundDrawableAttrs(true);
        //noinspection deprecation
        super.setBackgroundDrawable(mBackgroundDrawable);
    }

    /**
     * @return the largest corner radius.
     */
    public float getCornerRadius() {
        return getMaxCornerRadius();
    }

    /**
     * @return the largest corner radius.
     */
    public float getMaxCornerRadius() {
        float maxRadius = 0;
        for (float r : mCornerRadii) {
            maxRadius = Math.max(r, maxRadius);
        }
        return maxRadius;
    }

    /**
     * Get the corner radius of a specified corner.
     *
     * @param corner the corner.
     * @return the radius.
     */
    public float getCornerRadius(@Corner int corner) {
        return mCornerRadii[corner];
    }

    /**
     * Set all the corner radii from a dimension resource id.
     *
     * @param resId dimension resource id of radii.
     */
    public void setCornerRadiusDimen(@DimenRes int resId) {
        float radius = getResources().getDimension(resId);
        setCornerRadius(radius, radius, radius, radius);
    }

    /**
     * Set the corner radius of a specific corner from a dimension resource id.
     *
     * @param corner the corner to set.
     * @param resId  the dimension resource id of the corner radius.
     */
    public void setCornerRadiusDimen(@Corner int corner, @DimenRes int resId) {
        setCornerRadius(corner, getResources().getDimensionPixelSize(resId));
    }

    /**
     * Set the corner radii of all corners in px.
     *
     * @param radius the radius to set.
     */
    public void setCornerRadius(float radius) {
        cornerRadius = radius;
        setCornerRadius(
            roundedCorners[Corner.TOP_LEFT] ? radius : 0,
            roundedCorners[Corner.TOP_RIGHT] ? radius : 0,
            roundedCorners[Corner.BOTTOM_RIGHT] ? radius : 0,
            roundedCorners[Corner.BOTTOM_LEFT] ? radius : 0
        );
    }

    /**
     * Set the corner radius of a specific corner in px.
     *
     * @param corner the corner to set.
     * @param radius the corner radius to set in px.
     */
    public void setCornerRadius(@Corner int corner, float radius) {
        if (mCornerRadii[corner] == radius) {
            return;
        }
        mCornerRadii[corner] = radius;

        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    /**
     * Set the corner radii of each corner individually. Currently only one unique nonzero value is
     * supported.
     *
     * @param topLeft     radius of the top left corner in px.
     * @param topRight    radius of the top right corner in px.
     * @param bottomRight radius of the bottom right corner in px.
     * @param bottomLeft  radius of the bottom left corner in px.
     */
    public void setCornerRadius(float topLeft, float topRight, float bottomLeft, float bottomRight) {
        if (
            mCornerRadii[Corner.TOP_LEFT] == topLeft &&
            mCornerRadii[Corner.TOP_RIGHT] == topRight &&
            mCornerRadii[Corner.BOTTOM_RIGHT] == bottomRight &&
            mCornerRadii[Corner.BOTTOM_LEFT] == bottomLeft
        ) {
            return;
        }

        mCornerRadii[Corner.TOP_LEFT] = topLeft;
        mCornerRadii[Corner.TOP_RIGHT] = topRight;
        mCornerRadii[Corner.BOTTOM_LEFT] = bottomLeft;
        mCornerRadii[Corner.BOTTOM_RIGHT] = bottomRight;

        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    public float getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(@DimenRes int resId) {
        setBorderWidth(getResources().getDimension(resId));
    }

    public void setBorderWidth(float width) {
        if (mBorderWidth == width) {
            return;
        }

        mBorderWidth = width;
        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    @ColorInt
    public int getBorderColor() {
        return mBorderColor.getDefaultColor();
    }

    public void setBorderColor(@ColorInt int color) {
        setBorderColor(ColorStateList.valueOf(color));
    }

    public ColorStateList getBorderColors() {
        return mBorderColor;
    }

    public void setBorderColor(ColorStateList colors) {
        if (mBorderColor.equals(colors)) {
            return;
        }

        mBorderColor = (colors != null) ? colors : ColorStateList.valueOf(RoundedDrawable.DEFAULT_BORDER_COLOR);
        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        if (mBorderWidth > 0) {
            invalidate();
        }
    }

    public boolean isOval() {
        return mIsOval;
    }

    public void setOval(boolean oval) {
        mIsOval = oval;
        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    public Shader.TileMode getTileModeX() {
        return mTileModeX;
    }

    public void setTileModeX(Shader.TileMode tileModeX) {
        if (this.mTileModeX == tileModeX) {
            return;
        }

        this.mTileModeX = tileModeX;
        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    public Shader.TileMode getTileModeY() {
        return mTileModeY;
    }

    public void setTileModeY(Shader.TileMode tileModeY) {
        if (this.mTileModeY == tileModeY) {
            return;
        }

        this.mTileModeY = tileModeY;
        updateDrawableAttrs();
        updateBackgroundDrawableAttrs(false);
        invalidate();
    }

    public boolean mutatesBackground() {
        return mMutateBackground;
    }

    public void mutateBackground(boolean mutate) {
        if (mMutateBackground == mutate) {
            return;
        }

        mMutateBackground = mutate;
        updateBackgroundDrawableAttrs(true);
        invalidate();
    }

    @Override
    public void applyStyleRules(Map<String, String> rules) {
        // Do not apply the common rules for ImageView

        if (rules == null) {
            return;
        }

        // left, top, right, bottom
        Float[] padding = { 0f, 0f, 0f, 0f };

        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("border-radius".equalsIgnoreCase(rule.getKey())) {
                Float radius = StyleHelper.optFloat(rule.getValue());
                setCornerRadius(radius != null ? StyleHelper.dpToPixels(getResources(), radius) : 0);
            } else if ("opacity".equalsIgnoreCase(rule.getKey())) {
                Float val = StyleHelper.optFloat(rule.getValue());
                if (val != null) {
                    setAlpha(val);
                }
            } else if ("elevation".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = StyleHelper.optFloat(rule.getValue());
                    if (val != null) {
                        setElevation(val);
                    }
                }
            } else if ("scale".equalsIgnoreCase(rule.getKey())) {
                final String value = rule.getValue();
                if ("fill".equalsIgnoreCase(value)) {
                    setScaleType(ScaleType.CENTER_CROP);
                } else if ("fit".equalsIgnoreCase(value)) {
                    setScaleType(ScaleType.FIT_CENTER);
                }
            } else if ("z-index".equalsIgnoreCase(rule.getKey())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Float val = StyleHelper.optFloat(rule.getValue());
                    if (val != null) {
                        setZ(StyleHelper.dpToPixels(getResources(), val));
                    }
                }
            } else if ("rounded-corners".equalsIgnoreCase(rule.getKey())) {
                final String value = rule.getValue();
                if ("all".equalsIgnoreCase(value)) {
                    roundedCorners[Corner.TOP_LEFT] = true;
                    roundedCorners[Corner.TOP_RIGHT] = true;
                    roundedCorners[Corner.BOTTOM_LEFT] = true;
                    roundedCorners[Corner.BOTTOM_RIGHT] = true;
                } else if ("left".equalsIgnoreCase(value)) {
                    roundedCorners[Corner.TOP_LEFT] = true;
                    roundedCorners[Corner.TOP_RIGHT] = false;
                    roundedCorners[Corner.BOTTOM_LEFT] = true;
                    roundedCorners[Corner.BOTTOM_RIGHT] = false;
                } else if ("right".equalsIgnoreCase(value)) {
                    roundedCorners[Corner.TOP_LEFT] = false;
                    roundedCorners[Corner.TOP_RIGHT] = true;
                    roundedCorners[Corner.BOTTOM_LEFT] = false;
                    roundedCorners[Corner.BOTTOM_RIGHT] = true;
                } else if ("top".equalsIgnoreCase(value)) {
                    roundedCorners[Corner.TOP_LEFT] = true;
                    roundedCorners[Corner.TOP_RIGHT] = true;
                    roundedCorners[Corner.BOTTOM_LEFT] = false;
                    roundedCorners[Corner.BOTTOM_RIGHT] = false;
                } else if ("bottom".equalsIgnoreCase(value)) {
                    roundedCorners[Corner.TOP_LEFT] = false;
                    roundedCorners[Corner.TOP_RIGHT] = false;
                    roundedCorners[Corner.BOTTOM_LEFT] = true;
                    roundedCorners[Corner.BOTTOM_RIGHT] = true;
                }

                // Refresh the corner radius
                setCornerRadius(cornerRadius);
            }
            // Padding
            else if ("padding-left".equalsIgnoreCase(rule.getKey())) {
                padding[0] = StyleHelper.optFloat(rule.getValue());
            } else if ("padding-top".equalsIgnoreCase(rule.getKey())) {
                padding[1] = StyleHelper.optFloat(rule.getValue());
            } else if ("padding-right".equalsIgnoreCase(rule.getKey())) {
                padding[2] = StyleHelper.optFloat(rule.getValue());
            } else if ("padding-bottom".equalsIgnoreCase(rule.getKey())) {
                padding[3] = StyleHelper.optFloat(rule.getValue());
            }
        }

        final Resources res = getResources();
        setPadding(
            StyleHelper.dpToPixels(res, padding[0]),
            StyleHelper.dpToPixels(res, padding[1]),
            StyleHelper.dpToPixels(res, padding[2]),
            StyleHelper.dpToPixels(res, padding[3])
        );
    }
}
