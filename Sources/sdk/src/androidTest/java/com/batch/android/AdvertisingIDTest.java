package com.batch.android;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.batch.android.di.providers.AdvertisingIDProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test AdvertisingID object
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class AdvertisingIDTest
{
    /**
     * Test advertising ID value
     *
     * @throws Exception
     */
    @Test
    public void testAdvertisingID() throws Exception
    {
        AdvertisingID device = AdvertisingIDProvider.get();

        Thread.sleep(2000);

        assertTrue(device.isReady());
        assertNotNull(device.get());
    }
}
