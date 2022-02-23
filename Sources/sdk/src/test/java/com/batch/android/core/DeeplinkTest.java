package com.batch.android.core;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR2;
import static android.os.Build.VERSION_CODES.P;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

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

    @Test
    @Config(sdk = { JELLY_BEAN_MR2, P })
    public void testCustomTabIntent() {
        final String deeplink = "https://batch.com";

        final Intent intent = DeeplinkHelper.getIntent(deeplink, true, true);
        assertNotNull(intent);
        assertEquals(0, (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK));

        assertTrue(intent.getBooleanExtra("android.support.customtabs.extra.SHARE_MENU_ITEM", false));
        assertTrue(Objects.requireNonNull(intent.getExtras()).containsKey("android.support.customtabs.extra.SESSION"));
    }

    @Test
    @Config(sdk = { JELLY_BEAN, JELLY_BEAN_MR1 })
    public void testCustomTabOnOlderAPI() {
        final String deeplink = "https://batch.com";

        assertNull(DeeplinkHelper.getCustomTabIntent(Uri.parse(deeplink)));

        final Intent intent = DeeplinkHelper.getIntent(deeplink, true, true);
        assertNotNull(intent);
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK));

        assertNull(intent.getExtras());
    }
}
