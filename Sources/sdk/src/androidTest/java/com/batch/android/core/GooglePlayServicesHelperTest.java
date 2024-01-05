package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.android.gms.common.ConnectionResult;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test GooglePlayServicesHelper
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class GooglePlayServicesHelperTest {

    private Context appContext;
    private CountDownLatch latch;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test the version get
     *
     * @throws Exception
     */
    @Test
    public void testGetLibVersion() throws Exception {
        Integer version = GooglePlayServicesHelper.getGooglePlayServicesLibVersion(appContext);

        assertNotNull(version);
        assertTrue(version >= 4030500);
    }

    /**
     * Test the availability get
     *
     * @throws Exception
     */
    @Test
    public void testGetLibAvailability() throws Exception {
        Integer availability = GooglePlayServicesHelper.getGooglePlayServicesAvailabilityInteger(appContext);

        assertNotNull(availability);
        assertTrue(availability == 0);
        assertTrue("SUCCESS".equals(GooglePlayServicesHelper.getGooglePlayServicesAvailabilityString(availability)));
    }

    /**
     * Test the google environnement availability response
     *
     * @throws Exception
     */
    @Test
    public void testCheckGoogleServicesAvailability() throws Exception {
        final Integer playServicesAvailability = GooglePlayServicesHelper.getGooglePlayServicesAvailabilityInteger(
            appContext
        );
        assertNotNull(playServicesAvailability);
        assertEquals(ConnectionResult.SUCCESS, (int) playServicesAvailability);
    }
}
