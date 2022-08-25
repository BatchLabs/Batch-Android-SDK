package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

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

    @Test
    public void testMigrationSucceed() {
        String storageVersionKey = Parameters.PARAMETERS_KEY_PREFIX + ParameterKeys.SHARED_PREFS_STORAGE_VERSION;

        // Write encrypted data into legacy storage
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        SharedPreferences oldPreferences = appContext
            .getApplicationContext()
            .getSharedPreferences("bastion_kv", Context.MODE_PRIVATE);

        String encryptedValue = cryptor.encrypt("value");
        oldPreferences.edit().putString("test_encrypted", encryptedValue).apply();

        // Ensure the encrypted value in legacy storage is accessible in clear from the new one
        KVTestUserPreferencesStorage storage = new KVTestUserPreferencesStorage(appContext);
        assertEquals(storage.get("test_encrypted"), "value");

        // Ensure the storage version has been written after the migration
        String storageVersion = storage.get(storageVersionKey);
        assertEquals("2", storageVersion);

        // Ensure the data is still accessible from the legacy storage
        oldPreferences = appContext.getApplicationContext().getSharedPreferences("bastion_kv", Context.MODE_PRIVATE);
        assertEquals(encryptedValue, oldPreferences.getString("test_encrypted", null));
    }

    @Test
    public void testMigrationFailed() {
        String storageVersionKey = Parameters.PARAMETERS_KEY_PREFIX + ParameterKeys.SHARED_PREFS_STORAGE_VERSION;

        // Write encrypted data into legacy storage
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        SharedPreferences oldPreferences = appContext
            .getApplicationContext()
            .getSharedPreferences("bastion_kv", Context.MODE_PRIVATE);
        String encryptedValue = cryptor.encrypt("value");
        oldPreferences.edit().putString("test_encrypted", encryptedValue).apply();

        // Mock migration failed
        KVTestUserPreferencesStorage storage = PowerMockito.spy(new KVTestUserPreferencesStorage(appContext));
        PowerMockito.doReturn(false).when(storage).migrate(appContext);
        storage.remove(storageVersionKey);
        storage.migrateIfNeeded(appContext);

        // Ensure the storage version has not been written after the migration
        String storageVersion = storage.get(storageVersionKey);
        assertNull(storageVersion);

        // Ensure we use the legacy storage
        assertTrue(Whitebox.getInternalState(storage, "useLegacyStorage"));
        assertEquals(storage.get("test_encrypted"), "value");
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
