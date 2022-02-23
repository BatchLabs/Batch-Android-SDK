package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DI;
import com.batch.android.di.providers.ParametersProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of parameters
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParametersTest {

    /**
     * Key used for tests
     */
    private static final String key = "key";
    /**
     * Value used for tests
     */
    private static final String value = "value";

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    // ---------------------------------------------->

    @After
    public void tearDown() throws Exception {
        DI.reset();
    }

    // ---------------------------------------------->

    /**
     * Test basic add of a parameter without save
     *
     * @throws Exception
     */
    @Test
    public void testAddParameter() throws Exception {
        Parameters params = ParametersProvider.get(appContext);

        // Insert and check that it's available
        assertNull(params.get(key));
        params.set(key, value, false);
        assertEquals(value, params.get(key));

        // Clean instance
        DI.reset();

        // Check that it's erase at rebuild (no save)
        params = ParametersProvider.get(appContext);
        assertNull(params.get(key));
    }

    /**
     * Test add of a parameter with save
     *
     * @throws Exception
     */
    @Test
    public void testAddSavedParameter() throws Exception {
        Parameters params = ParametersProvider.get(appContext);

        // Insert and check that it's available
        assertNull(params.get(key));
        params.set(key, value, true);
        assertEquals(value, params.get(key));

        // Clean instance
        DI.reset();

        // Check that it's still available at rebuild
        params = ParametersProvider.get(appContext);
        assertEquals(value, params.get(key));
    }

    /**
     * Test remove of a parameter
     *
     * @throws Exception
     */
    @Test
    public void testRemoveParameter() throws Exception {
        Parameters params = ParametersProvider.get(appContext);

        // Insert and check that it's available
        assertNull(params.get(key));
        params.set(key, value, true);
        assertEquals(value, params.get(key));

        // Remove it and test result
        params.remove(key);
        assertNull(params.get(key));

        // Clean instance
        DI.reset();

        // Check that it's still removed
        params = ParametersProvider.get(appContext);
        assertNull(params.get(key));
    }

    /**
     * Test get with default value
     *
     * @throws Exception
     */
    @Test
    public void testDefaultValue() throws Exception {
        Parameters params = ParametersProvider.get(appContext);

        assertNull(params.get(key));
        assertEquals(value, params.get(key, value));
    }
}
