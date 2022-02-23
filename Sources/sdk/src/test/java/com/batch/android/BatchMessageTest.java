package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.content.Intent;
import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link BatchMessage}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchMessageTest {

    /**
     * Get a mock batch push payload json. Should be put in com.batch
     *
     * @return
     */
    private static Bundle getMockBatchLandingMessageBundle() throws Exception {
        String message =
            "{\"ld\":{\"kind\":\"universal\",\"id\":\"webtest\",\"did\":\"webtest\",\"hero\":\"http://batch.com/favicon.png\"," +
            "\"h1\":\"Hi\",\"h2\":\"Ho\",\"h3\":\"Subtitle\"," +
            "\"body\":\"Lorem ipsum.\",\"close\":true,\"cta\":[" +
            "{\"id\": \"okay\", \"label\": \"Okay!\", \"action\": \"callback\", \"actionString\": \"okaycallback\"}]," +
            "\"style\":\"#image-cnt {blur: 200;}\"}}";
        final Bundle batchDataBundle = new Bundle();
        batchDataBundle.putString("com.batch", message);
        batchDataBundle.putString("custom", "payload");

        final Bundle pushPayloadBundle = new Bundle();
        pushPayloadBundle.putBundle(Batch.Push.PAYLOAD_KEY, batchDataBundle);

        final Bundle messageBundle = new Bundle();
        messageBundle.putBundle("data", pushPayloadBundle);
        messageBundle.putString("kind", BatchLandingMessage.KIND);

        final Bundle rootBundle = new Bundle();
        rootBundle.putBundle("com.batch.messaging.payload", messageBundle);
        return rootBundle;
    }

    @Test
    public void testPreconditions() throws Exception {
        try {
            //noinspection ConstantConditions
            BatchMessage.getMessageForBundle(null);
            fail();
        } catch (IllegalArgumentException ignored) {}
    }

    @Test
    public void testWrongBundles() throws Exception {
        final Bundle b = new Bundle();

        try {
            BatchMessage.getMessageForBundle(b);
            fail();
        } catch (BatchPushPayload.ParsingException ignored) {}

        final Bundle message = new Bundle();
        message.putBundle("data", new Bundle());
        message.putString("kind", "invalid");
        b.putBundle("com.batch.messaging.payload", message);
        try {
            BatchMessage.getMessageForBundle(b);
            fail();
        } catch (BatchPushPayload.ParsingException ignored) {}
    }

    @Test
    public void testLandingMessageFromBundle() throws Exception {
        BatchMessage message = BatchMessage.getMessageForBundle(getMockBatchLandingMessageBundle());

        if (!(message instanceof BatchLandingMessage)) {
            fail();
        }
    }

    @Test
    public void testLandingMessageBundleSerialization() throws Exception {
        BatchLandingMessage msg = (BatchLandingMessage) BatchMessage.getMessageForBundle(
            getMockBatchLandingMessageBundle()
        );

        JSONObject customPayload = msg.getCustomPayloadInternal();
        assertEquals("payload", customPayload.getString("custom"));

        final Bundle b = new Bundle();
        msg.writeToBundle(b);
        checkIfBundleIsLanding(b);

        final Intent i = new Intent();
        msg.writeToIntent(i);
        checkIfBundleIsLanding(i.getExtras());
    }

    @Test
    public void testLandingMessageDeserialization() throws Exception {
        BatchMessage msg = BatchMessage.getMessageForBundle(getMockBatchLandingMessageBundle());
        JSONObject customPayload = msg.getCustomPayloadInternal();
        assertEquals("payload", customPayload.getString("custom"));

        final Bundle b = new Bundle();
        msg.writeToBundle(b);

        assertNotNull(BatchMessage.getMessageForBundle(b));

        final Intent i = new Intent();
        msg.writeToIntent(i);

        assertNotNull(BatchMessage.getMessageForBundle(i.getExtras()));
    }

    private void checkIfBundleIsLanding(Bundle bundle) throws Exception {
        final Bundle msgBundle = bundle.getBundle("com.batch.messaging.payload");

        assertNotNull(msgBundle);
        final Bundle data = msgBundle.getBundle("data");
        assertNotNull(data);
        final Bundle pushData = data.getBundle(Batch.Push.PAYLOAD_KEY);
        assertNotNull(pushData);
        assertNotNull(pushData.getString("com.batch"));
        assertEquals("landing", msgBundle.getString("kind"));
    }
}
