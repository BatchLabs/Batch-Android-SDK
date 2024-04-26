package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test various deeplink features, such as the generation of a custom tab intent
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeeplinkTest {

    @Test
    public void testValidDeeplink() {
        final String deeplink = "https://batch.com";

        Intent intent = DeeplinkHelper.getIntent(deeplink, false, true);
        assertNotNull(intent);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());

        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK));

        intent = DeeplinkHelper.getIntent(deeplink, false, false);
        assertNotNull(intent);

        assertEquals(0, (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Test
    public void testCustomTabSchemes() {
        assertTrue(DeeplinkHelper.customTabSupportsURI(Uri.parse("https://batch.com")));
        assertTrue(DeeplinkHelper.customTabSupportsURI(Uri.parse("HTTPS://batch.com")));
        assertTrue(DeeplinkHelper.customTabSupportsURI(Uri.parse("http://batch.com")));
        assertTrue(DeeplinkHelper.customTabSupportsURI(Uri.parse("HTTP://batch.com")));

        assertFalse(DeeplinkHelper.customTabSupportsURI(Uri.parse("batch.com")));

        assertFalse(DeeplinkHelper.customTabSupportsURI(Uri.parse("foobar://batch.com")));
    }
}
