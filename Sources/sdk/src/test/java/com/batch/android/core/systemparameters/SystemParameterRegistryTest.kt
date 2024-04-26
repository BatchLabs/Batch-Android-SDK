package com.batch.android.core.systemparameters

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.batch.android.di.DITest
import com.batch.android.di.providers.SystemParameterRegistryProvider

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemParameterRegistryTest : DITest() {

    @Test
    fun testGetParameters() {
        val registry = SystemParameterRegistryProvider.get(ApplicationProvider.getApplicationContext())
        Assert.assertEquals(SystemParameterShortName.values().size, registry.parameters.size)
    }

    @Test
    fun testGetWatchedParameters() {
        val registry = SystemParameterRegistryProvider.get(ApplicationProvider.getApplicationContext())
        Assert.assertEquals(17, registry.watchedParameters.size)
    }

    @Test
    fun testGetSystemParamByShortname() {
        val registry = SystemParameterRegistryProvider.get(ApplicationProvider.getApplicationContext())
        val parameter = registry.getSystemParamByShortname("apv")
        Assert.assertEquals(SystemParameterShortName.APPLICATION_VERSION, parameter?.shortName)
    }

}