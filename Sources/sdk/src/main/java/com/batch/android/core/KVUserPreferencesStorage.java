package com.batch.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.HashMap;
import java.util.Map;

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
     * @param context used to get shared prefs
     */
    public KVUserPreferencesStorage(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }
        preferences =
            context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);

        // Check if a data migration is needed
        migrateIfNeeded(context);
    }

    public boolean persist(String key, String value) {
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

    @Nullable
    public String get(String key) {
        return get(key, null);
    }

    @Nullable
    public String get(String key, String defaultValue) {
        if (useLegacyStorage) {
            return getOnLegacyStorage(key, defaultValue);
        }
        String value = preferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }

    @Nullable
    private String getOnLegacyStorage(String key, String defaultValue) {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        String value = preferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return cryptor.decrypt(value);
    }

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
                preferences =
                    context
                        .getApplicationContext()
                        .getSharedPreferences(LEGACY_SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
                useLegacyStorage = true;
                Logger.internal(TAG, "Data encryption migration has failed, fallback on legacy shared prefs.");
            }
        }
    }

    /**
     * Migrate data with new cryptor
     */
    @VisibleForTesting
    protected boolean migrate(Context context) {
        // Use legacy cryptor to read data
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);

        // Get data to migrate
        SharedPreferences oldPreferences = context
            .getApplicationContext()
            .getSharedPreferences(LEGACY_SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);

        Map<String, String> decryptedData = new HashMap<>();
        for (Map.Entry<String, ?> entry : oldPreferences.getAll().entrySet()) {
            decryptedData.put(entry.getKey(), cryptor.decrypt(entry.getValue().toString()));
        }

        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, String> entry : decryptedData.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        return editor.commit();
    }
}
