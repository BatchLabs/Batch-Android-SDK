package com.batch.android

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.batch.android.json.JSONObject
import com.batch.android.messaging.WebViewActionListener
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@SmallTest
class BatchMessagingWebViewJavascriptBridgeTest {

    val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testPostMessage() {
        val bridge = BatchMessagingWebViewJavascriptBridge(context, null, null)

        // Test that a known method returns a result
        assertJSONContainsKey("result", bridge.postMessage("dismiss", "{}"))

        // Test that an unknown method returns an error
        assertJSONContainsKey("error", bridge.postMessage("non_existing_method", "{}"))

        // Test argument validation
        assertJSONContainsKey("error", bridge.postMessage(null, null))
        assertJSONContainsKey("error", bridge.postMessage("dismiss", null))
        assertJSONContainsKey("error", bridge.postMessage(null, "{}"))
        assertJSONContainsKey("error", bridge.postMessage("   ", "{}"))
        assertJSONContainsKey("error", bridge.postMessage("dismiss", "[]"))
    }

    @Test
    fun testExpectedResults() {
        val bridge = MockingBridge(context, null, null)
        assertBridgeResult(MockingBridge.Expectations.installationID, bridge.get("getInstallationID"))
        assertBridgeResult(MockingBridge.Expectations.customLanguage, bridge.get("getCustomLanguage"))
        assertBridgeResult(MockingBridge.Expectations.customRegion, bridge.get("getCustomRegion"))
        assertBridgeResult(MockingBridge.Expectations.customUserID, bridge.get("getCustomUserID"))

        bridge.shouldReturnCustomDatas = false

        assertBridgeResult(null, bridge.get("getCustomLanguage"))
        assertBridgeResult(null, bridge.get("getCustomRegion"))
        assertBridgeResult(null, bridge.get("getCustomUserID"))

        assertBridgeResult(MockingBridge.Expectations.advertisingID, bridge.get("getAttributionID"))
        bridge.shouldReturnAdvertisingID = false
        assertBridgeError(bridge.get("getAttributionID"))
        bridge.shouldReturnAdvertisingID = true
        bridge.batchConfigAllowsAdvertisingID = false
        assertBridgeError(bridge.get("getAttributionID"))
    }

    @Test
    fun testTrackingID() {
        var bridge = MockingBridge(context, null, null)
        assertBridgeResult(null, bridge.get("getTrackingID"))

        val expectedTrackingID = "my_tracking_id"
        val mockPayload = JSONObject()
        mockPayload.put("did", expectedTrackingID)
        bridge = MockingBridge(context, MockMessage(mockPayload), null)
        assertBridgeResult(expectedTrackingID, bridge.get("getTrackingID"))
    }

    @Test
    fun testOpenDeeplink() {
        val mockActionListener = Mockito.mock(WebViewActionListener::class.java)
        val bridge = BatchMessagingWebViewJavascriptBridge(context, null, mockActionListener)

        assertBridgeError(bridge.postMessage("openDeeplink", "{}"))
        assertBridgeResult("ok", bridge.postMessage("openDeeplink", """{"url": "https://batch.com"}"""))
        assertBridgeResult("ok", bridge.postMessage("openDeeplink", """{"url": "https://batch.com", "openInApp": true}"""))
        assertBridgeResult("ok", bridge.postMessage("openDeeplink", """{"url": "https://batch.com", "openInApp": false}"""))
        assertBridgeResult("ok", bridge.postMessage("openDeeplink", """{"url": "https://batch.com", "openInApp": true, "analyticsID": 2}"""))
        assertBridgeResult("ok", bridge.postMessage("openDeeplink", """{"url": "https://batch.com", "openInApp": true, "analyticsID": "test_analytics"}"""))

        verify(mockActionListener).onOpenDeeplinkAction(Mockito.eq("https://batch.com"), Mockito.eq(null), Mockito.eq(null))
        verify(mockActionListener).onOpenDeeplinkAction(Mockito.eq("https://batch.com"), Mockito.eq(true), Mockito.eq(null))
        verify(mockActionListener).onOpenDeeplinkAction(Mockito.eq("https://batch.com"), Mockito.eq(false), Mockito.eq(null))
        verify(mockActionListener).onOpenDeeplinkAction(Mockito.eq("https://batch.com"), Mockito.eq(true), Mockito.eq(null))
        verify(mockActionListener).onOpenDeeplinkAction(Mockito.eq("https://batch.com"), Mockito.eq(true), Mockito.eq("test_analytics"))
    }

    @Test
    fun testDismiss() {
        val mockActionListener = Mockito.mock(WebViewActionListener::class.java)
        val bridge = BatchMessagingWebViewJavascriptBridge(context, null, mockActionListener)

        assertBridgeResult("ok", bridge.postMessage("dismiss", "{}"))
        assertBridgeResult("ok", bridge.postMessage("dismiss", """{"analyticsID": 2}"""))
        assertBridgeResult("ok", bridge.postMessage("dismiss", """{"analyticsID": "test_analytics"}"""))

        verify(mockActionListener).onDismissAction(Mockito.eq(null))
        verify(mockActionListener).onDismissAction(Mockito.eq("2"))
        verify(mockActionListener).onDismissAction(Mockito.eq("test_analytics"))
    }

    @Test
    fun testPerformAction() {
        val mockActionListener = Mockito.mock(WebViewActionListener::class.java)
        val bridge = BatchMessagingWebViewJavascriptBridge(context, null, mockActionListener)

        assertBridgeError(bridge.postMessage("performAction", "{}"))
        assertBridgeError(bridge.postMessage("performAction", """{"badKey": "foo"}"""))
        assertBridgeResult("ok", bridge.postMessage("performAction", """{"name":"batch.test", "args": {"arg1":"value"}}"""))
        assertBridgeResult("ok", bridge.postMessage("performAction", """{"name":"batch.test", "args": {"arg1":"value"}, "analyticsID": 2}"""))
        assertBridgeResult("ok", bridge.postMessage("performAction", """{"name":"batch.test", "args": {"arg1":"value"}, "analyticsID": " "}"""))
        assertBridgeResult("ok", bridge.postMessage("performAction", """{"name":"batch.test", "args": {"arg1":"value"}, "analyticsID": "test_analytics"}"""))

        verify(mockActionListener).onPerformAction(Mockito.eq("batch.test"), JSONObjectMockitoMatcher.eq(JSONObject().apply {
            put("arg1", "value")
        }), Mockito.eq(null))

        verify(mockActionListener).onPerformAction(Mockito.eq("batch.test"), JSONObjectMockitoMatcher.eq(JSONObject().apply {
            put("arg1", "value")
        }), Mockito.eq("2"))

        verify(mockActionListener).onPerformAction(Mockito.eq("batch.test"), JSONObjectMockitoMatcher.eq(JSONObject().apply {
            put("arg1", "value")
        }), Mockito.eq(" "))

        verify(mockActionListener).onPerformAction(Mockito.eq("batch.test"), JSONObjectMockitoMatcher.eq(JSONObject().apply {
            put("arg1", "value")
        }), Mockito.eq("test_analytics"))
    }

    private fun assertJSONContainsKey(key: String, rawJSON: String) {
        Assert.assertTrue(JSONObject(rawJSON).has(key));
    }

    private fun assertBridgeResult(expected: String?, bridgeResponse: String) {
        with(JSONObject(bridgeResponse)) {
            Assert.assertEquals(expected, reallyOptString("result", null))
        }
    }

    private fun assertBridgeError(bridgeResponse: String) {
        with(JSONObject(bridgeResponse)) {
            Assert.assertNotNull(reallyOptString("error", null))
        }
    }
}

class MockingBridge(context: Context, message: BatchMessage?, actionListener: WebViewActionListener?) : BatchMessagingWebViewJavascriptBridge(context, message, actionListener) {
    object Expectations {
        const val installationID = "install"
        const val customLanguage = "xx"
        const val customRegion = "XX"
        const val customUserID = "customid"
        const val advertisingID = "abcdef-ghijkl"
    }

    var shouldReturnCustomDatas = true
    var shouldReturnAdvertisingID = true
    var batchConfigAllowsAdvertisingID = true

    fun get(value: String): String {
        return this.postMessage(value, "{}")
    }

    override fun getInstallationID(): String? {
        return Expectations.installationID
    }

    override fun getCustomLanguage(): String? {
        if (shouldReturnCustomDatas) {
            return Expectations.customLanguage
        }
        return null
    }

    override fun getCustomRegion(): String? {
        if (shouldReturnCustomDatas) {
            return Expectations.customRegion
        }
        return null
    }

    override fun getCustomUserID(): String? {
        if (shouldReturnCustomDatas) {
            return Expectations.customUserID
        }
        return null
    }

    override fun isAdvertisingIDAllowedByConfig(): Boolean {
        return batchConfigAllowsAdvertisingID
    }

    override fun getAdvertisingIDValue(): String? {
        if (shouldReturnAdvertisingID) {
            return Expectations.advertisingID
        }
        return null
    }
}

class MockMessage(val payload: JSONObject) : BatchMessage() {
    override fun getJSON(): JSONObject {
        return payload
    }

    override fun getCustomPayloadInternal(): JSONObject {
        throw NotImplementedError("Not implemented")
    }

    override fun getKind(): String {
        throw NotImplementedError("Not implemented")
    }

    override fun getBundleRepresentation(): Bundle {
        throw NotImplementedError("Not implemented")
    }

}