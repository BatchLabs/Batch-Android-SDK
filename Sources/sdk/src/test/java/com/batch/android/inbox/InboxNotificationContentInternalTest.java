package com.batch.android.inbox;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.BatchNotificationSource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InboxNotificationContentInternalTest {

    private String payloadJson =
        "{\"t\":\"c\",\"l\":\"https://batch.com\",\"i\":\"79946434-e5c3-4b22-1a1a-68e8f556740a\",\"od\":{\"n\":\"02521fa3e-70e4-11ea-b5ac-bf057dd56464\",\"ct\":\"2918f6a1d46ff48c9e74bfded5d5d9c9e\"}}";

    @Test
    public void testValidPayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        InboxNotificationContentInternal content = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        Assert.assertTrue(content.isValid());

        Bundle payloadBundle = content.getReceiverLikePayload();
        Assert.assertEquals(payloadBundle.getString("com.batch"), payloadJson);
        Assert.assertEquals(payloadBundle.getString("test"), "test");
        Assert.assertEquals(payloadBundle.getString("hip"), "hop");
    }

    @Test
    public void testInvalidPayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");
        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");

        InboxNotificationContentInternal content = new InboxNotificationContentInternal(
            null,
            now,
            payload,
            identifiers
        );
        Assert.assertFalse(content.isValid());

        content = new InboxNotificationContentInternal(BatchNotificationSource.CAMPAIGN, null, payload, identifiers);
        Assert.assertFalse(content.isValid());

        content = new InboxNotificationContentInternal(BatchNotificationSource.CAMPAIGN, now, null, identifiers);
        Assert.assertFalse(content.isValid());

        content = new InboxNotificationContentInternal(BatchNotificationSource.CAMPAIGN, now, payload, null);
        Assert.assertFalse(content.isValid());
    }
}
