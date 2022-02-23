package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test UserPreferences key/value storage
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class KVUserPreferencesStorageTest {

    /**
     * Default key used to test
     */
    private String key = "key";
    /**
     * Default value used to test
     */
    private String value = "value";

    private Context appContext;

    // -------------------------------------->

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() throws Exception {
        /*
         * Clear UserPreferences
         */
        KVTestUserPreferencesStorage storage = new KVTestUserPreferencesStorage(appContext);
        storage.removeAll();
    }

    // -------------------------------------->

    /**
     * Test one write & read
     *
     * @throws Exception
     */
    @Test
    public void testUserPreferencesWriteRead() throws Exception {
        KVUserPreferencesStorage storage = new KVUserPreferencesStorage(appContext);
        storage.persist(key, value);

        assertEquals(value, storage.get(key));
    }

    /**
     * Test write & delete
     *
     * @throws Exception
     */
    @Test
    public void testUserPreferencesDelete() throws Exception {
        KVUserPreferencesStorage storage = new KVUserPreferencesStorage(appContext);
        storage.persist(key, value);

        assertNotNull(storage.get(key));

        storage.remove(key);

        assertNull(storage.get(key));
    }

    /**
     * Test default value getter
     *
     * @throws Exception
     */
    @Test
    public void testUserPreferencesDefaultValue() throws Exception {
        KVUserPreferencesStorage storage = new KVUserPreferencesStorage(appContext);

        assertNull(storage.get(key));
        assertEquals(value, storage.get(key, value));
    }

    /**
     * Test batch of write & read
     *
     * @throws Exception
     */
    @Test
    public void testBatchWriteRead() throws Exception {
        int size = 100;

        KVTestUserPreferencesStorage storage = new KVTestUserPreferencesStorage(appContext);

        storage.removeAll();

        for (int i = 0; i < size; i++) {
            storage.persist("key" + i, "value" + i);
        }

        assertEquals(size, storage.getAll().size());

        for (int i = 0; i < size; i++) {
            assertEquals("value" + i, storage.get("key" + i));
        }
    }

    // ----------------------------------------------->

    /**
     * Test extends of KV User Storage to wipe & get all data
     *
     */
    private static class KVTestUserPreferencesStorage extends KVUserPreferencesStorage {

        protected KVTestUserPreferencesStorage(Context context) {
            super(context);
        }

        @SuppressWarnings("unchecked")
        protected Map<String, String> getAll() {
            return (Map<String, String>) preferences.getAll();
        }

        protected void removeAll() {
            Map<String, ?> all = preferences.getAll();
            for (String key : all.keySet()) {
                remove(key);
            }
        }
    }
}
