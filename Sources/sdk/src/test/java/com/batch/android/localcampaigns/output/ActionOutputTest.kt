package com.batch.android.localcampaigns.output

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.JSONObjectMockitoMatcher
import com.batch.android.di.DI
import com.batch.android.di.DITest
import com.batch.android.di.DITestUtils
import com.batch.android.json.JSONObject
import com.batch.android.localcampaigns.model.LocalCampaign
import com.batch.android.module.ActionModule
import com.batch.android.runtime.RuntimeManager
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito

@Suppress("UsePropertyAccessSyntax")
@RunWith(AndroidJUnit4::class)
@SmallTest
class ActionOutputTest {

    @After
    fun cleanup() {
        DI.reset()
    }

    @Test
    fun testExecutesAction() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val runtimeManager = DITestUtils.mockSingletonDependency(RuntimeManager::class.java, null)
        PowerMockito
                .doReturn(context)
                .`when`(runtimeManager)
                .getContext()

        val actionModuleSpy = DITestUtils.mockSingletonDependency(ActionModule::class.java, null)
        PowerMockito
                .doReturn(true)
                .`when`(actionModuleSpy)
                .performAction(any(), anyString(), any(JSONObject::class.java), any())

        val fakeCampaign = LocalCampaign()
        fakeCampaign.id = "foobar"

        var output = ActionOutput(JSONObject().apply {
            put("action", "foo")
        })
        Assert.assertTrue(output.displayMessage(fakeCampaign))

        Mockito.verify(actionModuleSpy).performAction(Mockito.eq(context),
                Mockito.eq("foo"),
                JSONObjectMockitoMatcher.eq(JSONObject()),
                Mockito.isNull())

        output = ActionOutput(JSONObject().apply {
            put("action", "foobar")
            // Invalid args type
            put("args", 2)
        })
        Assert.assertTrue(output.displayMessage(fakeCampaign))

        Mockito.verify(actionModuleSpy).performAction(Mockito.eq(context),
                Mockito.eq("foobar"),
                JSONObjectMockitoMatcher.eq(JSONObject()),
                Mockito.isNull())

        output = ActionOutput(JSONObject().apply {
            put("action", "foobaz")
            put("args", JSONObject().apply {
                put("arg1", "val1")
            })
        })
        Assert.assertTrue(output.displayMessage(fakeCampaign))

        Mockito.verify(actionModuleSpy).performAction(Mockito.eq(context),
                Mockito.eq("foobaz"),
                JSONObjectMockitoMatcher.eq(JSONObject().apply {
                    put("arg1", "val1")
                }),
                Mockito.isNull())
    }

    @Test
    fun testDoesNotCrashWithInvalidPayload() {
        val actionModuleSpy = DITestUtils.mockSingletonDependency(ActionModule::class.java, null)
        PowerMockito
                .doReturn(false)
                .`when`(actionModuleSpy)
                .performAction(any(), anyString(), any(JSONObject::class.java), any())

        val runtimeManager = DITestUtils.mockSingletonDependency(RuntimeManager::class.java, null)
        PowerMockito
                .doReturn(null)
                .`when`(runtimeManager)
                .getContext()

        val fakeCampaign = LocalCampaign()
        fakeCampaign.id = "foobar"

        var output = ActionOutput(JSONObject())
        Assert.assertFalse(output.displayMessage(fakeCampaign))

        output = ActionOutput(JSONObject().apply {
            put("foo", "bar")
        })
        Assert.assertFalse(output.displayMessage(fakeCampaign))

        Mockito.verifyNoInteractions(actionModuleSpy)

        output = ActionOutput(JSONObject().apply {
            put("action", 2)
        })
        Assert.assertFalse(output.displayMessage(fakeCampaign))

        output = ActionOutput(JSONObject().apply {
            put("action", JSONObject())
        })
        Assert.assertFalse(output.displayMessage(fakeCampaign))

        // Don't call "verifyNoInteractions" as "action" is stringified if of the wrong type
    }
}