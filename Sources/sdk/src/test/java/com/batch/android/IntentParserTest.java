package com.batch.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link BatchPushPayload} model
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class IntentParserTest {

    private static final String FROM_PUSH_KEY = "com.batch.from_push";
    private static final String ALREADY_TRACKED_OPEN_KEY = "com.batch.open.tracked";
    private static final String ALREADY_SHOWN_LANDING_KEY = "com.batch.messaging.push.shown";

    @Test
    public void testHasPushPayload() throws Exception {
        Intent i = new Intent();

        Bundle payload = new Bundle();
        payload.putString("com.batch", getMockBatchData());

        Bundle b = new Bundle();
        b.putBundle(Batch.Push.PAYLOAD_KEY, payload);

        IntentParser parser = new IntentParser(i);
        Assert.assertFalse(parser.hasPushPayload());

        i.putExtras(b);
        parser = new IntentParser(i);
        Assert.assertTrue(parser.hasPushPayload());
    }

    @Test
    public void testFromPush() {
        Intent intentFromPush = new Intent();
        intentFromPush.putExtra(FROM_PUSH_KEY, true);
        Intent intentNotFromPush = new Intent();
        intentNotFromPush.putExtra(FROM_PUSH_KEY, false);

        IntentParser parser = new IntentParser(intentFromPush);
        Assert.assertTrue(parser.comesFromPush());

        parser = new IntentParser(intentNotFromPush);
        Assert.assertFalse(parser.comesFromPush());
    }

    @Test
    public void testIsOpenAlreadyTracked() {
        Intent intentAlreadyTracked = new Intent();
        intentAlreadyTracked.putExtra(ALREADY_TRACKED_OPEN_KEY, true);
        Intent intentNotAlreadyTracked = new Intent();
        intentNotAlreadyTracked.putExtra(ALREADY_TRACKED_OPEN_KEY, false);

        IntentParser parser = new IntentParser(intentAlreadyTracked);
        Assert.assertTrue(parser.isOpenAlreadyTracked());

        parser = new IntentParser(intentNotAlreadyTracked);
        Assert.assertFalse(parser.isOpenAlreadyTracked());
    }

    @Test
    public void testMarkOpenAsAlreadyTracked() {
        Intent intent = new Intent();
        intent.putExtra(FROM_PUSH_KEY, true);
        intent.putExtra(ALREADY_TRACKED_OPEN_KEY, false);

        IntentParser parser = new IntentParser(intent);

        Assert.assertFalse(parser.isOpenAlreadyTracked());
        parser.markOpenAsAlreadyTracked();
        Assert.assertTrue(parser.isOpenAlreadyTracked());
    }

    @Test
    public void testHasLanding() throws Exception {
        Intent i = new Intent();

        Bundle payload = new Bundle();
        payload.putString("com.batch", getMockBatchData());

        Bundle b = new Bundle();
        b.putBundle(Batch.Push.PAYLOAD_KEY, payload);

        IntentParser parser = new IntentParser(i);
        Assert.assertFalse(parser.hasLanding());

        i.putExtras(b);
        parser = new IntentParser(i);
        Assert.assertTrue(parser.hasLanding());
    }

    @Test
    public void testIsLandingAlreadyShown() throws Exception {
        Intent intentShown = new Intent();
        intentShown.putExtra(ALREADY_SHOWN_LANDING_KEY, true);

        Intent intentNotShown = new Intent();
        intentNotShown.putExtra(ALREADY_SHOWN_LANDING_KEY, false);

        IntentParser parser = new IntentParser(intentNotShown);
        Assert.assertFalse(parser.isLandingAlreadyShown());

        parser = new IntentParser(intentShown);
        Assert.assertTrue(parser.isLandingAlreadyShown());
    }

    @Test
    public void testGetLanding() throws Exception {
        Bundle payload = new Bundle();
        payload.putString("com.batch", getMockBatchData());

        Bundle b = new Bundle();
        b.putBundle(Batch.Push.PAYLOAD_KEY, payload);

        Intent i = new Intent();

        IntentParser parser = new IntentParser(i);

        BatchMessage message = parser.getLanding();
        Assert.assertNull(message);

        i.putExtras(b);

        parser = new IntentParser(i);

        message = parser.getLanding();
        Assert.assertNotNull(message);
    }

    @Test
    public void testMarkLandingAsAlreadyShown() throws Exception {
        Intent i = new Intent();
        i.putExtra(ALREADY_SHOWN_LANDING_KEY, false);

        IntentParser parser = new IntentParser(i);
        Assert.assertFalse(parser.isLandingAlreadyShown());

        parser.markLandingAsAlreadyShown();

        Assert.assertTrue(parser.isLandingAlreadyShown());
    }

    /**
     * Get a mock batch push payload json. Should be put in com.batch
     */
    private static String getMockBatchData() throws Exception {
        JSONObject batchData = new JSONObject();

        JSONObject largeIconObject = new JSONObject();
        batchData.put("bi", largeIconObject);

        JSONObject bigPictureObject = new JSONObject();
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
