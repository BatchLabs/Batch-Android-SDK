package com.batch.android.module

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batch.android.Batch
import com.batch.android.JSONObjectMockitoMatcher
import com.batch.android.JSONObjectPartialMatcher
import com.batch.android.WebserviceParameterUtils
import com.batch.android.core.systemparameters.SystemParameterShortName
import com.batch.android.di.DITest
import com.batch.android.di.DITestUtils
import com.batch.android.di.providers.DataCollectionModuleProvider
import com.batch.android.event.InternalEvents
import com.batch.android.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class DataCollectionModuleTest : DITest() {

    private lateinit var context: Context
    private lateinit var trackerModule: TrackerModule

    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        simulateBatchStart(context)
        DataCollectionModuleProvider.get().batchContextBecameAvailable(context)
        trackerModule = DITestUtils.mockSingletonDependency(TrackerModule::class.java, null)
    }

    @Test
    fun testSystemParametersMayHaveChange() {

        // Expected event payload
        val expectedParams =
            JSONObject().apply {
                put("device_language", "en-US")
                put("device_region", "US")
            }

        // Start module (this will check for native changes)
        DataCollectionModuleProvider.get().batchDidStart()

        // Verify event is triggered
        Mockito.verify(trackerModule, Mockito.timeout(1000).times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.NATIVE_DATA_CHANGED),
                JSONObjectPartialMatcher.eq(expectedParams),
            )
    }

    @Test
    fun testConfigureDataPrivacy() {
        // Enable all
        Batch.updateAutomaticDataCollection {
            it.setGeoIPEnabled(true).setDeviceModelEnabled(true).setDeviceBrandEnabled(true)
        }
        // Verify all data are sent
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.NATIVE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(
                    JSONObject().apply {
                        put("geoip_resolution", true)
                        put("device_brand", "Android")
                        put("device_model", "robolectric")
                    }
                ),
            )

        // Disable only geoip
        Batch.updateAutomaticDataCollection { it.setGeoIPEnabled(false) }
        // Verify only geoip is sent with false
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.NATIVE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(JSONObject().apply { put("geoip_resolution", false) }),
            )

        // Disable only device brand and model
        Batch.updateAutomaticDataCollection {
            it.setDeviceBrandEnabled(false).setDeviceModelEnabled(false)
        }
        // Verify only device brand and model is sent with null
        Mockito.verify(trackerModule, Mockito.times(1))
            .track(
                ArgumentMatchers.eq(InternalEvents.NATIVE_DATA_CHANGED),
                JSONObjectMockitoMatcher.eq(
                    JSONObject().apply {
                        put("device_brand", JSONObject.NULL)
                        put("device_model", JSONObject.NULL)
                    }
                ),
            )

        Mockito.reset(trackerModule)

        // Disable all
        Batch.updateAutomaticDataCollection {
            it.setDeviceBrandEnabled(false).setDeviceModelEnabled(false).setGeoIPEnabled(false)
        }
        // Verify no event is send since it was already disabled (times(3) for the event triggered
        // before
        Mockito.verify(trackerModule, Mockito.never())
            .track(
                ArgumentMatchers.eq(InternalEvents.NATIVE_DATA_CHANGED),
                Mockito.any(JSONObject::class.java),
            )
    }

    @Test
    fun testConfigureDataPrivacyForIds() {
        // Check default disabled param are not is ids
        WebserviceParameterUtils.getWebserviceIdsAsJson(context).apply {
            Assert.assertFalse(has(SystemParameterShortName.DEVICE_BRAND.shortName))
            Assert.assertFalse(has(SystemParameterShortName.DEVICE_MODEL.shortName))
            Assert.assertFalse(getJSONObject("data_collection").getBoolean("geoip"))
        }

        // Enable all
        Batch.updateAutomaticDataCollection {
            it.setGeoIPEnabled(true).setDeviceModelEnabled(true).setDeviceBrandEnabled(true)
        }

        // Check values are in the ids
        WebserviceParameterUtils.getWebserviceIdsAsJson(context).apply {
            Assert.assertEquals("Android", get(SystemParameterShortName.DEVICE_BRAND.shortName))
            Assert.assertEquals("robolectric", get(SystemParameterShortName.DEVICE_MODEL.shortName))
            Assert.assertTrue(getJSONObject("data_collection").getBoolean("geoip"))
        }

        // Disable all
        Batch.updateAutomaticDataCollection {
            it.setGeoIPEnabled(false).setDeviceModelEnabled(false).setDeviceBrandEnabled(false)
        }

        // Check disabled param are not is ids
        WebserviceParameterUtils.getWebserviceIdsAsJson(context).apply {
            Assert.assertFalse(has(SystemParameterShortName.DEVICE_BRAND.shortName))
            Assert.assertFalse(has(SystemParameterShortName.DEVICE_MODEL.shortName))
            Assert.assertFalse(getJSONObject("data_collection").getBoolean("geoip"))
        }
    }
}
