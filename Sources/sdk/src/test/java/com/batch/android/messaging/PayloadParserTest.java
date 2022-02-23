package com.batch.android.messaging;

import static android.os.Build.VERSION_CODES.O;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.BaseBannerMessage;
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.ModalMessage;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.model.WebViewMessage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Test case that checks that all the message formats are serializable
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PayloadParserTest {

    @Test
    public void testImagePayload() throws PayloadParsingException, JSONException {
        String payload =
            "{'image':'https://static.batch.com/tmp/test_inapp.jpg','hasImage':true,'kind':'image','id':'tmpid','did':'webtest','fullscreen': false,'auto_close': 0,'action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'style':'@import sdk(\\\"image1-detached\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof ImageMessage);
        ImageMessage imageMessage = (ImageMessage) message;
        Assert.assertEquals("https://static.batch.com/tmp/test_inapp.jpg", imageMessage.imageURL);
        Assert.assertEquals("batch.deeplink", imageMessage.globalTapAction.action);
        Assert.assertEquals("@import sdk(\"image1-detached\");", imageMessage.css);
        Assert.assertEquals("webtest", imageMessage.devTrackingIdentifier);
        Assert.assertEquals("tmpid", imageMessage.messageIdentifier);
        Assert.assertEquals(0, imageMessage.autoCloseDelay);
        Assert.assertFalse(imageMessage.isFullscreen);
    }

    @Test
    public void testImageNoImagePayload() throws JSONException {
        String payload =
            "{'hasImage':true,'kind':'image','id':'webtest','did':'webtest','fullscreen': false,'auto_close': 0,'action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'style':'@import sdk(\\\"image1-detached\\\");'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testImageNoStylePayload() throws JSONException {
        String payload =
            "{'image':'https://static.batch.com/tmp/test_inapp.jpg','hasImage':true,'kind':'image','id':'webtest','did':'webtest','fullscreen': false,'auto_close': 0,'action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}}}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testImageInvalidStylePayload() throws JSONException {
        String payload =
            "{'image':'https://static.batch.com/tmp/test_inapp.jpg','hasImage':true,'kind':'image','id':'webtest','did':'webtest','fullscreen': false,'auto_close': 0,'action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'style':'wrong-cs;s;sd'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testImageNegativeDimensionsPayload() throws JSONException {
        String payload =
            "{'image':'https://static.batch.com/tmp/test_inapp.jpg','width':-1,'height':-1,'hasImage':true,'kind':'image','id':'webtest','did':'webtest','fullscreen': false,'auto_close': 0,'action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'style':'@import sdk(\\\"image1-detached\\\");'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testWebViewPayload() throws PayloadParsingException, JSONException {
        String payload =
            "{'url':'https://batch-html-inapp-test.glitch.me/index.html','kind':'webview','id':'tmpid','did':'webtest','style':'@import sdk(\\\"webview1\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof WebViewMessage);
        WebViewMessage webViewMessage = (WebViewMessage) message;
        Assert.assertEquals("https://batch-html-inapp-test.glitch.me/index.html", webViewMessage.url);
        Assert.assertEquals("@import sdk(\"webview1\");", webViewMessage.css);
        Assert.assertEquals("webtest", webViewMessage.devTrackingIdentifier);
        Assert.assertEquals("tmpid", webViewMessage.messageIdentifier);
    }

    @Test
    public void testWebViewNoUrlPayload() throws JSONException {
        String payload = "{'kind':'webview','id':'tmpid','did':'webtest','style':'@import sdk(\\\"webview1\\\");'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testWebViewInvalidUrlPayload() throws JSONException {
        String payload =
            "{'url':'file://je/suis/un/test.html','kind':'webview','id':'tmpid','did':'webtest','style':'@import sdk(\\\"webview1\\\");'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testWebViewInvalidUrlPayload2() throws JSONException {
        String payload =
            "{'url':'testswag22','kind':'webview','id':'tmpid','did':'webtest','style':'@import sdk(\\\"webview1\\\");'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testWebViewNoStylePayload() throws JSONException {
        String payload =
            "{'url':'https://batch-html-inapp-test.glitch.me/index.html','kind':'webview','id':'tmpid','did':'webtest'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testWebViewInvalidStylePayload() throws JSONException {
        String payload =
            "{'url':'https://batch-html-inapp-test.glitch.me/index.html','kind':'webview','id':'tmpid','did':'webtest', 'style':'pdigr;sefs'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testModalPayload() throws PayloadParsingException, JSONException {
        String payload =
            "{'cta':[{'l':'testLabel', 'a': 'batch.deeplink','args':{'l':'https://google.fr'}}],'ctaCount':1,'hasImage':true,'id':'tmpid','did':'webtest','image':'https://test.com/image.jpg','action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'kind':'modal','close':true,'cta_direction':'h','auto_close':0,'style':'@import sdk(\\\"modal1\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof ModalMessage);
        ModalMessage modalMessage = (ModalMessage) message;
        Assert.assertEquals("https://test.com/image.jpg", modalMessage.imageURL);
        Assert.assertEquals("batch.deeplink", modalMessage.globalTapAction.action);
        Assert.assertEquals("@import sdk(\"modal1\");", modalMessage.css);
        Assert.assertEquals("webtest", modalMessage.devTrackingIdentifier);
        Assert.assertEquals("tmpid", modalMessage.messageIdentifier);
        Assert.assertEquals(0, modalMessage.autoCloseDelay);
        Assert.assertEquals(BaseBannerMessage.CTADirection.HORIZONTAL, modalMessage.ctaDirection);
        Assert.assertEquals("testLabel", modalMessage.ctas.get(0).label);
        Assert.assertEquals("batch.deeplink", modalMessage.ctas.get(0).action);
    }

    @Test
    public void testModalNoStylePayload() throws JSONException {
        String payload =
            "{'cta':[{'l':'testLabel', 'a': 'batch.deeplink','args':{'l':'https://google.fr'}}],'ctaCount':1,'hasImage':true,'id':'tmpid','did':'webtest','image':'https://test.com/image.jpg','action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'kind':'modal','close':true,'cta_direction':'h','auto_close':0}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testModalInvalidStylePayload() throws JSONException {
        String payload =
            "{'cta':[{'l':'testLabel', 'a': 'batch.deeplink','args':{'l':'https://google.fr'}}],'ctaCount':1,'hasImage':true,'id':'tmpid','did':'webtest','image':'https://test.com/image.jpg','action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'kind':'modal','close':true,'cta_direction':'h','auto_close':0, 'style':'sefs;sef;s'}";
        JSONObject json = new JSONObject(payload);

        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException e) {}
    }

    @Test
    public void testBannerPayload() throws PayloadParsingException, JSONException {
        String payload =
            "{'cta':[{'l':'testLabel', 'a': 'batch.deeplink','args':{'l':'https://google.fr'}}],'ctaCount':1,'hasImage':true,'id':'tmpid','did':'webtest','image':'https://test.com/image.jpg','action': {'a': 'batch.deeplink','args':{'l':'https://google.fr'}},'kind':'banner','close':true,'cta_direction':'h','auto_close':0,'style':'@import sdk(\\\"banner1\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof BannerMessage);
        BannerMessage bannerMessage = (BannerMessage) message;
        Assert.assertEquals("https://test.com/image.jpg", bannerMessage.imageURL);
        Assert.assertEquals("batch.deeplink", bannerMessage.globalTapAction.action);
        Assert.assertEquals("@import sdk(\"banner1\");", bannerMessage.css);
        Assert.assertEquals("webtest", bannerMessage.devTrackingIdentifier);
        Assert.assertEquals("tmpid", bannerMessage.messageIdentifier);
        Assert.assertEquals(0, bannerMessage.autoCloseDelay);
        Assert.assertEquals(BaseBannerMessage.CTADirection.HORIZONTAL, bannerMessage.ctaDirection);
        Assert.assertEquals("testLabel", bannerMessage.ctas.get(0).label);
        Assert.assertEquals("batch.deeplink", bannerMessage.ctas.get(0).action);
    }

    @Test
    public void testUniversalPayload() throws PayloadParsingException, JSONException {
        String payload =
            "{'cta':[{'l':'testLabel', 'a': 'batch.deeplink','args':{'l':'https://google.fr'}}],'id':'tmpid','did':'webtest','ctaCount':1,'hasImage':false,'kind':'universal','flip_hero_h':false,'flip_hero_v':true,'attach_cta_bottom':true,'stack_cta_h':false,'stretch_cta_h':true,'hero_split_ratio':0.4,'close':false,'style':'@import sdk(\\\"generic1-v-cta\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof UniversalMessage);
        UniversalMessage universalMessage = (UniversalMessage) message;
        Assert.assertEquals("@import sdk(\"generic1-v-cta\");", universalMessage.css);
        Assert.assertEquals("webtest", universalMessage.devTrackingIdentifier);
        Assert.assertEquals("tmpid", universalMessage.messageIdentifier);
        Assert.assertEquals(0, universalMessage.autoCloseDelay);
        Assert.assertTrue(universalMessage.attachCTAsBottom);
        Assert.assertFalse(universalMessage.flipHeroHorizontal);
        Assert.assertEquals("testLabel", universalMessage.ctas.get(0).label);
        Assert.assertEquals("batch.deeplink", universalMessage.ctas.get(0).action);
    }

    @Test
    @Config(sdk = { O })
    public void testMinimumAndroidVersion() throws PayloadParsingException, JSONException {
        // Recent enough
        String payload =
            "{'min_android_sdk': 26, 'url':'https://batch-html-inapp-test.glitch.me/index.html','kind':'webview','id':'tmpid','did':'webtest','style':'@import sdk(\\\"webview1\\\");'}";
        JSONObject json = new JSONObject(payload);

        Message message = PayloadParser.parsePayload(json);
        Assert.assertTrue(message instanceof WebViewMessage);

        // Android version too old
        json.put("min_android_sdk", 27);
        try {
            PayloadParser.parsePayload(json);
            Assert.fail();
        } catch (PayloadParsingException ignored) {}
    }
}
