package com.batch.android.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;

/**
 * UserPreferences implementation of object Storage
 *
 */
@Module
@Singleton
public class ObjectUserPreferencesStorage {

    private static final String TAG = "ObjectUserPreferencesStorage";

    /**
     * Name of the shared preferences file
     */
    private static final String SHARED_PREFERENCES_FILENAME = "bastion_o";

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
    public ObjectUserPreferencesStorage(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        preferences =
            context.getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
        cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
    }

    // ------------------------------------->

    public boolean persist(String key, Serializable value) {
        try {
            return preferences.edit().putString(key, serialize(value)).commit();
        } catch (Exception e) {
            Logger.internal(TAG, "Error while persisting object for key " + key, e);
            return false;
        }
    }

    public Object get(String key) throws IOException {
        try {
            return deserialize(preferences.getString(key, null));
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    public boolean contains(String key) {
        return preferences.contains(key);
    }

    @SuppressLint("ApplySharedPref")
    public void remove(String key) {
        preferences.edit().remove(key).commit();
    }

    // ------------------------------------->

    /**
     * Serialize the object to a String using Base64 encode
     *
     * @param object
     * @return
     * @throws IOException
     */
    private String serialize(Serializable object) throws IOException {
        if (object == null) {
            throw new NullPointerException("Null object");
        }

        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            return ByteArrayHelper.getUTF8String(cryptor.encrypt(baos.toByteArray()));
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while closing ObjectOutputStream", e);
                }
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while closing ByteArrayOutputStream", e);
                }
            }
        }
    }

    /**
     * Deserialize the serialized String using Base64
     *
     * @param serialized
     * @return
     * @throws StreamCorruptedException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Object deserialize(String serialized) throws StreamCorruptedException, IOException, ClassNotFoundException {
        if (serialized == null) {
            return null;
        }

        ByteArrayInputStream ais = null;
        ObjectInputStream ois = null;
        try {
            byte[] data = cryptor.decryptToByte(serialized);
            ais = new ByteArrayInputStream(data);
            ois = new ObjectInputStream(ais);
            Object value = ois.readObject();
            return value;
        } finally {
            if (ais != null) {
                try {
                    ais.close();
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while closing ArrayInputStream", e);
                }
            }

            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while closing ObjectInputStream", e);
                }
            }
        }
    }
}
