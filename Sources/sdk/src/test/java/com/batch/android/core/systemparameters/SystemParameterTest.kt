package com.batch.android.core.systemparameters

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemParameterTest {

    @Test
    fun testGetValue() {
        val parameter =
            SystemParameter(SystemParameterShortName.APPLICATION_VERSION) {
                return@SystemParameter "my-app-version"
            }
        Assert.assertEquals("my-app-version", parameter.value)
    }

    @Test
    fun testGetShortname() {
        val parameter =
            SystemParameter(SystemParameterShortName.APPLICATION_VERSION) {
                return@SystemParameter "my-app-version"
            }
        Assert.assertEquals(SystemParameterShortName.APPLICATION_VERSION, parameter.shortName)
    }

    @Test
    fun testHasChanged() {
        val parameter =
            WatchedSystemParameter(
                ApplicationProvider.getApplicationContext(),
                SystemParameterShortName.APPLICATION_VERSION,
            ) {
                return@WatchedSystemParameter "my-app-version-1"
            }
        // First time value is null
        Assert.assertNull(parameter.lastValue)

        // Ensure the value has changed
        Assert.assertTrue(parameter.hasChanged())

        // Ensure the value is now the right one
        Assert.assertEquals("my-app-version-1", parameter.lastValue)

        // Ensure value hasn't changed since getter has hardcoded string
        Assert.assertFalse(parameter.hasChanged())
    }
}
