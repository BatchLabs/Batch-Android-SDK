package com.batch.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UserPreferences implementation of key/value Storage
 */
@Module
@Singleton
public class KVUserPreferencesStorage {

    private static final String TAG = "KVUserPreferencesStorage";

    /**
     * Name of the legacy shared preferences file (use eas encryption)
     */
    private static final String LEGACY_SHARED_PREFERENCES_FILENAME = "bastion_kv";

    /**
     * Name of the current shared preferences file (use no encryption)
     */
    private static final String SHARED_PREFERENCES_FILENAME = "batch";

    /**
     * Version of the storage
     */
    private static final int STORAGE_VERSION = 2;

    /**
     * Reference of SharedPreferences
     */
    protected SharedPreferences preferences;

    /**
     * Whether we should use the legacy storage
     */
    private boolean useLegacyStorage;

    /**
     * Single thread executor for reading shared prefs
     */
    ExecutorService executor = Executors.newSingleThreadExecutor(new NamedThreadFactory());

    /**
     * Constructor
     * @param context used to get shared prefs
     */
    public KVUserPreferencesStorage(@NonNull Context context) {
        // Get shared prefs
        preferences = getPreferences(context, SHARED_PREFERENCES_FILENAME);

        // Check if a data migration is needed
        migrateIfNeeded(context);
    }

    /**
     * Save value into the Shared Preferences
     *
     * @param key The name of the preference to add.
     * @param value The value of the preference to modify. If null equals to remove.
     * @return true if operation succeed
     */
    @AnyThread
    public boolean persist(@NonNull String key, @Nullable String value) {
        if (useLegacyStorage) {
            return persistOnLegacyStorage(key, value);
        }
        try {
            preferences.edit().putString(key, value).apply();
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while persisting value for key " + key, e);
            return false;
        }
    }

    /**
     * Get a value from the Shared Preferences
     *
     * @param key The name of the preference to get.
     * @return the value or null
     */
    @Nullable
    @AnyThread
    public String get(@NonNull String key) {
        return get(key, null);
    }

    /**
     * Get a value from the Shared Preferences
     *
     * @param key The name of the preference to get.
     * @param defaultValue The value to fallback if operation failed or key doesn't exist.
     * @return the value found or default value
     */
    @Nullable
    @AnyThread
    public String get(@NonNull String key, @Nullable String defaultValue) {
        if (useLegacyStorage) {
            return getOnLegacyStorage(key, defaultValue);
        }
        String value = preferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * Check if a value is in the Shared Preferences
     * @param key The name of the preference to get.
     * @return true if exists
     * @throws Exception exception
     */
    @AnyThread
    public boolean contains(@NonNull String key) throws Exception {
        return executor.submit(() -> preferences.contains(key)).get();
    }

    /**
     * Remove value from the Shared Preferences
     * @param key The name of the preference to remove.
     */
    @AnyThread
    public void remove(@NonNull String key) {
        preferences.edit().remove(key).apply();
    }

    /**
     * Clear all values from the Shared Preferences
     */
    @AnyThread
    public void clear() {
        preferences.edit().clear().apply();
    }

    /**
     * Get the Shared Preferences in a Future
     *
     * @param context Android's context
     * @param name The name of the shared preferences file
     * @return the shared prefs
     */
    @AnyThread
    private SharedPreferences getPreferences(@NonNull Context context, @NonNull String name) {
        Context applicationContext = context.getApplicationContext();
        return applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    @Nullable
    @AnyThread
    private String getOnLegacyStorage(@NonNull String key, @Nullable String defaultValue) {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        String value = preferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return cryptor.decrypt(value);
    }

    @AnyThread
    private boolean persistOnLegacyStorage(String key, String value) {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        try {
            preferences.edit().putString(key, cryptor.encrypt(value)).apply();
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while persisting value for key " + key, e);
            return false;
        }
    }

    /**
     * Check if the shared preferences still used the AES cryptor and process the data
     * migration if needs.
     */
    @VisibleForTesting
    protected void migrateIfNeeded(Context context) {
        String versionKey = Parameters.PARAMETERS_KEY_PREFIX + ParameterKeys.SHARED_PREFS_STORAGE_VERSION;
        String version = get(versionKey);
        if (version == null) {
            Logger.internal(TAG, "Data storage use deprecated cryptor, starting migration.");

            // migrate data
            boolean succeed = migrate(context);

            if (succeed) {
                // Update storage version
                persist(versionKey, String.valueOf(STORAGE_VERSION));
                Logger.internal(TAG, "Data encryption has been successfully migrated");
            } else {
                // Fallback on legacy storage
                preferences = getPreferences(context, LEGACY_SHARED_PREFERENCES_FILENAME);
                useLegacyStorage = true;
            }
        }
    }

    /**
     * Migrate data with new cryptor
     */
    @VisibleForTesting
    protected boolean migrate(Context context) {
        try {
            // Use legacy cryptor to read data
            Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);

            // Get data to migrate
            SharedPreferences oldPreferences = getPreferences(context, LEGACY_SHARED_PREFERENCES_FILENAME);

            Map<String, String> decryptedData = new HashMap<>();
            for (Map.Entry<String, ?> entry : oldPreferences.getAll().entrySet()) {
                decryptedData.put(entry.getKey(), cryptor.decrypt(entry.getValue().toString()));
            }

            SharedPreferences.Editor editor = preferences.edit();
            for (Map.Entry<String, String> entry : decryptedData.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Data encryption migration has failed, fallback on legacy shared prefs.", e);
            return false;
        }
    }
}
