package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Install object
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class InstallTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test data generation
     *
     * @throws Exception
     */
    @Test
    public void testInstallIDGenerationTest() throws Exception {
        Install install = new Install(appContext);

        assertNotNull(install.getInstallID());
        assertNotNull(install.getInstallDate());
    }

    /**
     * Test data persistance
     *
     * @throws Exception
     */
    @Test
    public void testInstallIDStorage() throws Exception {
        Install install = new Install(appContext);

        assertNotNull(install.getInstallID());
        assertNotNull(install.getInstallDate());

        String previousInstallID = install.getInstallID();
        Date previousDate = install.getInstallDate();

        // Create a new install object to check persistance
        install = new Install(appContext);

        assertEquals(previousInstallID, install.getInstallID());
        assertEquals(previousDate, install.getInstallDate());
    }
}
