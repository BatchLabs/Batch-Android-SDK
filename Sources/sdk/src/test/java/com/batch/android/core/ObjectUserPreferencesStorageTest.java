package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test UserPreferences object storage
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ObjectUserPreferencesStorageTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test one write & read
     *
     * @throws Exception
     */
    @Test
    public void testUserPreferencesWriteRead() throws Exception {
        String key = "wrkey";
        ObjectTest obj = generateObject();

        ObjectUserPreferencesStorage storage = new ObjectUserPreferencesStorage(appContext);
        storage.persist(key, obj);

        assertEquals(obj, storage.get(key));
    }

    /**
     * Test write & delete
     *
     * @throws Exception
     */
    @Test
    public void testUserPreferencesDelete() throws Exception {
        String key = "delkey";
        ObjectTest obj = generateObject();

        ObjectUserPreferencesStorage storage = new ObjectUserPreferencesStorage(appContext);
        storage.persist(key, obj);

        assertNotNull(storage.get(key));

        storage.remove(key);

        assertNull(storage.get(key));
    }

    // --------------------------------------------->

    /**
     * A test object that implements Serializable
     *
     */
    private static class ObjectTest implements Serializable {

        private static final long serialVersionUID = 1L;

        public String id;
        public Map<String, String> values = new HashMap<>();
        public List<String> array = new ArrayList<>();

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }

            ObjectTest other = (ObjectTest) obj;
            if (array == null) {
                if (other.array != null) {
                    return false;
                }
            } else if (!array.equals(other.array)) {
                return false;
            }

            if (id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!id.equals(other.id)) {
                return false;
            }

            if (values == null) {
                if (other.values != null) {
                    return false;
                }
            } else if (!values.equals(other.values)) {
                return false;
            }

            return true;
        }
    }

    /**
     * Generate a test object with values
     *
     * @return
     */
    private static ObjectTest generateObject() {
        ObjectTest obj = new ObjectTest();
        obj.id = "defaultID";
        obj.values.put("Test", "Test");
        obj.values.put("Test2", "Test2");
        obj.array.add("Test");
        obj.array.add("Test2");

        return obj;
    }
}
