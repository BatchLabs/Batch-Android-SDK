package com.batch.android.core;

import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SecureDateProviderTest {

    @Test
    public void testProvidesDateWithNoServerTS() {
        SecureDateProvider provider = new SecureDateProvider();
        Assert.assertNotNull(provider.getDate());
        Assert.assertNotNull(provider.getCurrentDate());
        Assert.assertFalse(provider.isSecureDateAvailable());
    }

    @Test
    public void testProvidesDateWithAServerTS() {
        SecureDateProvider provider = new SecureDateProvider();
        provider.initServerDate(new Date());
        Assert.assertNotNull(provider.getDate());
        Assert.assertNotNull(provider.getCurrentDate());
        Assert.assertTrue(provider.isSecureDateAvailable());
    }

    @Test
    public void testUnavailableOnDisabledDevices() {
        SecureDateProvider provider = new SecureDateProvider();
        provider.initServerDate(new Date());
        Assert.assertEquals(provider.isSecureDateAvailable(), !"samsung".equalsIgnoreCase(Build.BRAND));
        Assert.assertNotNull(provider.getDate());
        Assert.assertNotNull(provider.getCurrentDate());

        provider = new ForciblyDisabledControllableSecureDateProvider();
        provider.initServerDate(new Date());
        Assert.assertFalse(provider.isSecureDateAvailable());
        Assert.assertNotNull(provider.getDate());
        Assert.assertNotNull(provider.getCurrentDate());
    }

    private static class ForciblyDisabledControllableSecureDateProvider extends SecureDateProvider {

        @Override
        protected boolean canEnableSecureDate() {
            return false;
        }
    }
}
