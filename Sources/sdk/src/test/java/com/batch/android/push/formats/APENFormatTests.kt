package com.batch.android.push.formats

import android.widget.ImageView
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
    fun testScaling() {
        val format = APENFormat("title", "body", null, null)

        Assert.assertEquals(ImageView.ScaleType.CENTER_CROP, format.imageScaleType)

        format.applyArguments(null)
        Assert.assertEquals(ImageView.ScaleType.CENTER_CROP, format.imageScaleType)

        format.applyArguments(JSONObject().apply {
            put("scale", 0)
        })
        Assert.assertEquals(ImageView.ScaleType.CENTER_CROP, format.imageScaleType)

        format.applyArguments(JSONObject().apply {
            put("scale", 1)
        })
        Assert.assertEquals(ImageView.ScaleType.FIT_CENTER, format.imageScaleType)
    }
}