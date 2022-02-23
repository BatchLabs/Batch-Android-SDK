package com.batch.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;

/**
 * UserPreferences implementation of key/value Storage
 *
 */
@Module
@Singleton
public class KVUserPreferencesStorage {

    private static final String TAG = "KVUserPreferencesStorage";
    /**
     * Name of the shared preferences file
     */
    private static final String SHARED_PREFERENCES_FILENAME = "bastion_kv";

    // ------------------------------------->

    /**
     * Reference of SharedPreferences
     */
    protected SharedPreferences preferences;
    /**
     * Cryptor used to crypt/decrypt data
     */
    private Cryptor cryptor;

    // ------------------------------------->

    /**
     * @param context
     */
    public KVUserPreferencesStorage(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        preferences =
            context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
    }

    // ------------------------------------->

    public boolean persist(String key, String value) {
        try {
            preferences.edit().putString(key, cryptor.encrypt(value)).apply();
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while persisting value for key " + key, e);
            return false;
        }
    }

    public String get(String key) {
        return get(key, null);
    }

    public String get(String key, String defaultValue) {
        String value = preferences.getString(key, null);
        if (value == null) {
            return defaultValue;
        }

        return cryptor.decrypt(value);
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    @SuppressLint("ApplySharedPref")
    public void remove(String key) {
        preferences.edit().remove(key).apply();
    }
}
