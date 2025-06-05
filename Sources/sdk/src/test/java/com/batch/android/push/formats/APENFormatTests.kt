package com.batch.android.push.formats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class APENFormatTests {
    @Test
    fun testApenLayoutTypeFillMatchParent() {
        val layoutType = APENFormat.LayoutType.CENTER_CROP_MP
        Assert.assertEquals(false, layoutType.shouldFitCenter())
        Assert.assertEquals(false, layoutType.shouldForceLayoutHeight())
    }

    @Test
    fun testApenLayoutTypeFill200() {
        val layoutType = APENFormat.LayoutType.CENTER_CROP_200
        Assert.assertEquals(false, layoutType.shouldFitCenter())
        Assert.assertEquals(true, layoutType.shouldForceLayoutHeight())
    }

    @Test
    fun testApenLayoutTypeFitMatchParent() {
        val layoutType = APENFormat.LayoutType.FIT_CENTER_MP
        Assert.assertEquals(true, layoutType.shouldFitCenter())
        Assert.assertEquals(false, layoutType.shouldForceLayoutHeight())
    }

    @Test
    fun testApenLayoutTypeFit200() {
        val layoutType = APENFormat.LayoutType.FIT_CENTER_200
        Assert.assertEquals(true, layoutType.shouldFitCenter())
        Assert.assertEquals(true, layoutType.shouldForceLayoutHeight())
    }

    @Test
    fun testScaling() {
        val format = APENFormat("title", "body", null, null)

        // Testing default format is fit 200
        Assert.assertEquals(APENFormat.LayoutType.FIT_CENTER_200, format.layoutType)
        format.applyArguments(null)
        Assert.assertEquals(APENFormat.LayoutType.FIT_CENTER_200, format.layoutType)

        // Testing arguments fit center match parent
        format.applyArguments(JSONObject().apply { put("apen_layout_type", "fit_mp") })
        Assert.assertEquals(APENFormat.LayoutType.FIT_CENTER_MP, format.layoutType)

        // Testing arguments fit center 200dp
        format.applyArguments(JSONObject().apply { put("apen_layout_type", "fit_200") })
        Assert.assertEquals(APENFormat.LayoutType.FIT_CENTER_200, format.layoutType)

        // Testing arguments fill match parent
        format.applyArguments(JSONObject().apply { put("apen_layout_type", "fill_mp") })
        Assert.assertEquals(APENFormat.LayoutType.CENTER_CROP_MP, format.layoutType)

        // Testing arguments fill 200dp
        format.applyArguments(JSONObject().apply { put("apen_layout_type", "fill_200") })
        Assert.assertEquals(APENFormat.LayoutType.CENTER_CROP_200, format.layoutType)
    }
}
