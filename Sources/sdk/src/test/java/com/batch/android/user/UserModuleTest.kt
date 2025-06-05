package com.batch.android.user

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.MockBatchAttributesFetchListener
import com.batch.android.MockBatchTagCollectionsFetchListener
import com.batch.android.UserDataAccessor
import com.batch.android.di.DITest
import com.batch.android.di.providers.UserModuleProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserModuleTest : DITest() {

    private lateinit var context: Context

    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        simulateBatchStart(context)
    }

    @Test
    fun testSetLanguage() {
        val expected = "fr"
        val userModule = UserModuleProvider.get()
        // Ensure default value is null
        Assert.assertNull(userModule.getLanguage(context))
        // Set a new value
        userModule.setLanguage(context, expected)
        // Ensure we get the new value
        Assert.assertEquals(expected, userModule.getLanguage(context))
    }

    @Test
    fun testSetRegion() {
        val expected = "FR"
        val userModule = UserModuleProvider.get()
        // Ensure default value is null
        Assert.assertNull(userModule.getRegion(context))
        // Set a new value
        userModule.setRegion(context, expected)
        // Ensure we get the new value
        Assert.assertEquals(expected, userModule.getRegion(context))
    }

    @Test
    fun testSetCustomID() {
        val expected = "test_custom_user_id"
        val userModule = UserModuleProvider.get()
        // Ensure default value is null
        Assert.assertNull(userModule.getCustomID(context))
        // Set a new value
        userModule.setCustomID(context, expected)
        // Ensure we get the new value
        Assert.assertEquals(expected, userModule.getCustomID(context))
    }

    @Test
    fun testClearInstallData() {
        val userModule = UserModuleProvider.get()

        // initial state
        InstallDataEditor()
            .addTag("collection", "tag")
            .setAttribute("attribute", "string")
            .saveSync()
        val attributeListener = MockBatchAttributesFetchListener()
        val tagsListener = MockBatchTagCollectionsFetchListener()

        // Fetch and ensure data has been saved
        UserDataAccessor.fetchAttributes(context, attributeListener, false)
        UserDataAccessor.fetchTagCollections(context, tagsListener, false)
        Assert.assertEquals(1, attributeListener.attributes.size)
        Assert.assertEquals(1, tagsListener.tagCollections.size)

        // Clear data
        userModule.clearInstallationData()

        Thread.sleep(100)

        // Fetch again and ensure data has been cleared
        UserDataAccessor.fetchAttributes(context, attributeListener, false)
        UserDataAccessor.fetchTagCollections(context, tagsListener, false)
        Assert.assertEquals(0, attributeListener.attributes.size)
        Assert.assertEquals(0, tagsListener.tagCollections.size)
    }
}
