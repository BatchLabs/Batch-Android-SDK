package com.batch.android.eventdispatcher;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.CTA;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MessagingEventPayloadTest {

    private static CTA parseCTA(JSONObject ctaJSON) {
        try {
            String label = ctaJSON.getString("l");
            String actionString = ctaJSON.reallyOptString("a", null);
            JSONObject args = ctaJSON.optJSONObject("args");

            if (args == null) {
                args = new JSONObject();
            }

            return new CTA(label, actionString, args);
        } catch (JSONException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    @Test
    public void testValidData() {
        String stringPayload =
            "{\"title\":\"test\",\"id\":\"9242259\",\"l-campaignid\":\"9242259\",\"did\":\"content-id\",\"hero\":null,\"hdesc\":null,\"h3\":null,\"body\":\"TEST\",\"ed\":{\"t\":\"l\",\"v\":\"0\",\"lth\":\"SIMPLE-TOP-BANNER\",\"i\":\"1569579362154\"},\"kind\":\"banner\",\"attach_cta_bottom\":true,\"hero_split_ratio\":0.4,\"close\":true,\"cta_direction\":\"h\",\"auto_close\":10000,\"style\":\"@import sdk(\\\"banner1\\\");\\n*{--valign:top;--countdown-color:#191E5E;--countdown-valign:bottom;--ios-shadow:15 0.5 #000000;--android-shadow:10;--title-color:#DB243B;--body-color:#666666;--bg-color:#FFFFFF;--close-color:#666666;--cta-android-shadow:auto;--cta1-color:#DB243B;--cta1-text-color:#FFFFFF;--cta2-color:#ffffff;--cta2-text-color:#E3959F;--margin:15;--corner-radius:5;--mode:dark;}\",\"cta\":[{\"a\":\"batch.deeplink\",\"args\":{\"l\":\"https://batch.com/test?utm_campaign=test&utm_content=main_button\"},\"l\":\"TEST\"},{\"a\":\"batch.deeplink\",\"args\":{\"l\":\"https:\\/\\/batch.com\\/test?utm_campaign=test&utm_content=second_button\"},\"l\":\"TEST\"},{\"a\":null,\"args\":{},\"l\":\"CANCEL\"}]}";
        String stringCustomPayload = "{\"hip\":\"hop\",\"hop\":\"hip\"}";
        JSONObject payload = null;
        JSONObject customPayload = null;
        try {
            payload = new JSONObject(stringPayload);
            customPayload = new JSONObject(stringCustomPayload);
        } catch (JSONException e) {
            Assert.fail("Error while parsing test payload: " + e.getMessage());
        }
        //TODO Test BatchMessage
        MessagingEventPayload eventPayload = new MessagingEventPayload(null, payload, customPayload);

        Assert.assertNull(eventPayload.getDeeplink());
        Assert.assertEquals("content-id", eventPayload.getTrackingId());
        Assert.assertNull(eventPayload.getCustomValue("title"));
        Assert.assertNull(eventPayload.getPushPayload());
        Assert.assertNull(eventPayload.getMessagingPayload());
        Assert.assertEquals("hop", eventPayload.getCustomValue("hip"));
        Assert.assertEquals("hip", eventPayload.getCustomValue("hop"));
        Assert.assertFalse(eventPayload.isPositiveAction());

        final JSONArray jsonCTAs = payload.optJSONArray("cta");
        Assert.assertNotNull(jsonCTAs);
        Assert.assertEquals(jsonCTAs.length(), 3);

        JSONObject jsonCTA1 = jsonCTAs.optJSONObject(0);
        Assert.assertNotNull(jsonCTA1);
        CTA cta1 = parseCTA(jsonCTA1);
        MessagingEventPayload eventPayloadCTA1 = new MessagingEventPayload(null, payload, null, cta1);

        Assert.assertTrue(eventPayloadCTA1.isPositiveAction());
        Assert.assertEquals(
            eventPayloadCTA1.getDeeplink(),
            "https://batch.com/test?utm_campaign=test&utm_content=main_button"
        );

        JSONObject jsonCTA2 = jsonCTAs.optJSONObject(1);
        Assert.assertNotNull(jsonCTA2);
        CTA cta2 = parseCTA(jsonCTA2);
        MessagingEventPayload eventPayloadCTA2 = new MessagingEventPayload(null, payload, null, cta2);

        Assert.assertTrue(eventPayloadCTA2.isPositiveAction());
        Assert.assertEquals(
            eventPayloadCTA2.getDeeplink(),
            "https://batch.com/test?utm_campaign=test&utm_content=second_button"
        );
        Assert.assertNull(eventPayloadCTA2.getCustomValue("hop"));

        JSONObject jsonCTA3 = jsonCTAs.optJSONObject(2);
        Assert.assertNotNull(jsonCTA3);
        CTA cta3 = parseCTA(jsonCTA3);
        MessagingEventPayload eventPayloadCTA3 = new MessagingEventPayload(null, payload, null, cta3);

        Assert.assertFalse(eventPayloadCTA3.isPositiveAction());
        Assert.assertNull(eventPayloadCTA3.getDeeplink());
    }
}
