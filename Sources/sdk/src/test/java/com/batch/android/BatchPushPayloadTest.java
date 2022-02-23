package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.InternalPushData;
import com.batch.android.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link BatchPushPayload} model
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchPushPayloadTest {

    private static final String DEEPLINK = "sdoifhsoif://oisdhf";
    private static final String LARGE_ICON_URL = "http://osdihsfoih.com/jqiopqj.png";
    private static final String BIG_PICTURE_URL = "http://oisdfhsof.com/sdfhsf.png";

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * This test checks that wrong arguments throw the right errors
     *
     * @throws Exception
     */
    @Test
    public void testPreconditions() throws Exception {
        try {
            //noinspection ConstantConditions
            BatchPushPayload.payloadFromBundle(null);
            fail();
        } catch (IllegalArgumentException ignored) {}

        try {
            //noinspection ConstantConditions
            BatchPushPayload.payloadFromReceiverIntent(null);
            fail();
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * This test checks that a bundle that does not, at some point, contain "com.batch", will fail.
     * For performance reasons, there's no full integrity check done in BatchPushPayload
     *
     * @throws Exception
     */
    @Test
    public void testIncorrectData() throws Exception {
        final Bundle b = new Bundle();
        try {
            BatchPushPayload.payloadFromBundle(b);
        } catch (BatchPushPayload.ParsingException ignored) {}

        b.putBundle(Batch.Push.PAYLOAD_KEY, new Bundle());
        try {
            BatchPushPayload.payloadFromBundle(b);
        } catch (BatchPushPayload.ParsingException ignored) {}

        final Intent i = new Intent();
        try {
            BatchPushPayload.payloadFromReceiverIntent(i);
        } catch (IllegalArgumentException ignored) {}

        i.putExtra("foo", "bar");
        try {
            BatchPushPayload.payloadFromReceiverIntent(i);
        } catch (BatchPushPayload.ParsingException ignored) {}
    }

    /**
     * This test checks that a valid bundle is parsable and gets accurate information
     *
     * @throws Exception
     */
    @Test
    public void testValidDataForBundle() throws Exception {
        final Bundle payload = new Bundle();
        payload.putString("com.batch", getMockBatchData());
        final Bundle b = new Bundle();
        b.putBundle(Batch.Push.PAYLOAD_KEY, payload);
        performSharedDataAssertions(BatchPushPayload.payloadFromBundle(b));
    }

    /**
     * This test checks that a valid bundle is parsable and gets accurate information
     *
     * @throws Exception
     */
    @Test
    public void testValidDataForReceiverIntent() throws Exception {
        final Intent i = new Intent();
        i.putExtra("com.batch", getMockBatchData());
        performSharedDataAssertions(BatchPushPayload.payloadFromReceiverIntent(i));
    }

    private void performSharedDataAssertions(BatchPushPayload payload) throws Exception {
        assertTrue(payload.hasDeeplink());
        assertEquals(DEEPLINK, payload.getDeeplink());

        assertTrue(payload.hasCustomLargeIcon());
        assertEquals(LARGE_ICON_URL, payload.getCustomLargeIconURL(appContext));

        assertTrue(payload.hasBigPicture());
        assertEquals(BIG_PICTURE_URL, payload.getBigPictureURL(appContext));

        assertEquals(60L, payload.getInternalData().getReceiptMinDelay());
        assertEquals(3600L, payload.getInternalData().getReceiptMaxDelay());
        assertEquals(InternalPushData.ReceiptMode.DISPLAY, payload.getInternalData().getReceiptMode());

        assertTrue(payload.hasLandingMessage());

        final BatchMessage msg = payload.getLandingMessage();
        assertNotNull(msg);
        assertEquals("landing", msg.getKind());
    }

    /**
     * Get a mock batch push payload json. Should be put in com.batch
     *
     * @return
     */
    private static String getMockBatchData() throws Exception {
        JSONObject batchData = new JSONObject();
        batchData.put("l", DEEPLINK);

        JSONObject largeIconObject = new JSONObject();
        largeIconObject.put("u", LARGE_ICON_URL);
        batchData.put("bi", largeIconObject);

        JSONObject bigPictureObject = new JSONObject();
        bigPictureObject.put("u", BIG_PICTURE_URL);
        batchData.put("bp", bigPictureObject);

        JSONObject receipt = new JSONObject();
        receipt.put("dmi", 60);
        receipt.put("dma", 3600);
        receipt.put("m", 1);
        batchData.put("r", receipt);

        String message =
            "{\"kind\":\"universal\",\"id\":\"webtest\",\"did\":\"webtest\",\"hero\":\"http://batch.com/favicon.png\"," +
            "\"h1\":\"Hi\",\"h2\":\"Ho\",\"h3\":\"Subtitle\"," +
            "\"body\":\"Lorem ipsum.\",\"close\":true,\"cta\":[" +
            "{\"id\": \"okay\", \"label\": \"Okay!\", \"action\": \"callback\", \"actionString\": \"okaycallback\"}]," +
            "\"style\":\"#image-cnt {blur: 200;}\"}";
        batchData.put("ld", new JSONObject(message));

        return batchData.toString();
    }
}
