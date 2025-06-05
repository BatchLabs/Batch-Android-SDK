package com.batch.android.core.systemparameters

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemParameterHelperTest {

    @Test
    fun testSerializeSystemParameters() {
        val languageParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_LANGUAGE,
                ) {
                    return@WatchedSystemParameter "en"
                }
                .apply { hasChanged() }
        val regionParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_REGION,
                ) {
                    return@WatchedSystemParameter "EN"
                }
                .apply { hasChanged() }
        val timeZoneParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_TIMEZONE,
                ) {
                    return@WatchedSystemParameter "Europe/Paris"
                }
                .apply { hasChanged() }
        val serializedParams =
            SystemParameterHelper.serializeSystemParameters(
                listOf(languageParameter, regionParameter, timeZoneParameter)
            )

        Assert.assertEquals(
            "{\"device_language\":\"en\",\"device_region\":\"EN\",\"device_timezone\":\"Europe\\/Paris\"}",
            serializedParams.toString(),
        )
    }

    @Test
    fun testSerializeSystemParametersWithNullValues() {
        val languageParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_LANGUAGE,
                ) {
                    return@WatchedSystemParameter null
                }
                .apply { hasChanged() }
        val regionParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_REGION,
                ) {
                    return@WatchedSystemParameter null
                }
                .apply { hasChanged() }
        val timeZoneParameter =
            WatchedSystemParameter(
                    ApplicationProvider.getApplicationContext(),
                    SystemParameterShortName.DEVICE_TIMEZONE,
                ) {
                    return@WatchedSystemParameter null
                }
                .apply { hasChanged() }
        val serializedParams =
            SystemParameterHelper.serializeSystemParameters(
                listOf(languageParameter, regionParameter, timeZoneParameter)
            )

        Assert.assertEquals(
            "{\"device_language\":null,\"device_region\":null,\"device_timezone\":null}",
            serializedParams.toString(),
        )
    }
}
