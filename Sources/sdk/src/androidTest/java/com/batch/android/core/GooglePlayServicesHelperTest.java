package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.android.gms.common.ConnectionResult;
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

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test the version get
     *
     */
    @Test
    public void testGetLibVersion() {
        Integer version = GooglePlayServicesHelper.getGooglePlayServicesLibVersion();

        assertNotNull(version);
        assertTrue(version >= 4030500);
    }

    /**
     * Test the availability get
     *
     */
    @Test
    public void testGetLibAvailability() {
        Integer availability = GooglePlayServicesHelper.getGooglePlayServicesAvailabilityInteger(appContext);
        assertNotNull(availability);
        assertEquals(0, (int) availability);
        assertEquals("SUCCESS", GooglePlayServicesHelper.getGooglePlayServicesAvailabilityString(availability));
    }

    /**
     * Test the google environment availability response
     *
     */
    @Test
    public void testCheckGoogleServicesAvailability() {
        final Integer playServicesAvailability = GooglePlayServicesHelper.getGooglePlayServicesAvailabilityInteger(
            appContext
        );
        assertNotNull(playServicesAvailability);
        assertEquals(ConnectionResult.SUCCESS, (int) playServicesAvailability);
    }
}
