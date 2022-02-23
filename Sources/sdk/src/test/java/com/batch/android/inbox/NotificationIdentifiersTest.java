package com.batch.android.inbox;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationIdentifiersTest {

    @Test
    public void testIsValid() {
        NotificationIdentifiers identifiers = new NotificationIdentifiers(
            "3698fa3e-70e4-88ea-b5ac-bf587d342484",
            "65846434-e5c3-4b33-28a2-18e8f520140a"
        );
        Assert.assertTrue(identifiers.isValid());

        identifiers = new NotificationIdentifiers("test-id", "");
        Assert.assertFalse(identifiers.isValid());

        identifiers = new NotificationIdentifiers("", "test-send-id");
        Assert.assertFalse(identifiers.isValid());

        identifiers = new NotificationIdentifiers("", "");
        Assert.assertFalse(identifiers.isValid());

        identifiers = new NotificationIdentifiers("test-id", null);
        Assert.assertFalse(identifiers.isValid());

        identifiers = new NotificationIdentifiers(null, "test-send-id");
        Assert.assertFalse(identifiers.isValid());

        identifiers = new NotificationIdentifiers(null, null);
        Assert.assertFalse(identifiers.isValid());
    }
}
