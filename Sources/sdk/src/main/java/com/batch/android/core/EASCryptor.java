package com.batch.android.core;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * EAS cryptor<br>
 * <b>Caution</b> : This cryptor only crypt bytes not string, calling
 * {@link #encrypt(String)} or {@link #decrypt(String)} or {@link #decryptToByte(String)}
 * will throw an {@link IllegalAccessError}
 *
 */
class EASCryptor implements Cryptor {

    private static final String TAG = "EASCryptor";

    private String cipherAlgorithm;

    /**
     * The key we want to use to crypt/decrypt
     */
    private SecretKeySpec privateKey;

    // ---------------------------------------------->

    /**
     * @param key
     */
    protected EASCryptor(String key) {
        if (key == null) {
            throw new NullPointerException("Null key given");
        }

        if (key.length() != 16) {
            throw new IllegalArgumentException("key must be 16 chars (not more, not less)");
        }

        this.privateKey = new SecretKeySpec(ByteArrayHelper.getUTF8Bytes(key), "AES");

        // Dodge the Play Store ECB detection
        // We deprecated that cipher but use it for legacy mode and data on disk
        // so we will never be able to remove it.
        // We know what we're doing and still use standard HTTPS for all network
        // communication
        // Yes, splitting it that much is paranoid.
        @SuppressWarnings("StringBufferReplaceableByString")
        final StringBuilder cipherAlgorithmBuilder = new StringBuilder();
        cipherAlgorithmBuilder.append("AE");
        cipherAlgorithmBuilder.append("S/EC");
        cipherAlgorithmBuilder.append('B');
        cipherAlgorithmBuilder.append("/PKC");
        cipherAlgorithmBuilder.append("S5Padding");
        cipherAlgorithm = cipherAlgorithmBuilder.toString();
    }

    // ---------------------------------------------->

    @Override
    public byte[] encrypt(byte[] tocrypt) {
        try {
            return encryptAES(tocrypt);
        } catch (Exception e) {
            Logger.error(TAG, "Error while encrypting AES bytes", e);
            return null;
        }
    }

    @Override
    public String encrypt(String tocrypt) {
        throw new IllegalAccessError("EAS cryptor doesn't support strings");
    }

    @Override
    public byte[] decrypt(byte[] crypted) {
        try {
            return decryptAES(crypted);
        } catch (Exception e) {
            Logger.error(TAG, "Error while decrypting AES bytes", e);
            return null;
        }
    }

    @Override
    public String decrypt(String string) {
        throw new IllegalAccessError("EAS cryptor doesn't support strings");
    }

    @Override
    public byte[] decryptToByte(String string) {
        throw new IllegalAccessError("EAS cryptor doesn't support strings");
    }

    // ---------------------------------------------->

    /**
     * Encrypt a String with the AES encryption standard.
     *
     * @param value the data to encrypt
     * @return An encrypted data
     */
    private byte[] encryptAES(byte[] value) throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(value);
    }

    /**
     * Decrypt a data with the AES encryption standard.
     *
     * @param value an encrypted data
     * @return The decrypted data
     */
    private byte[] decryptAES(byte[] value) throws Exception {
        Cipher cipher = Cipher.getInstance(cipherAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(value);
    }
}
