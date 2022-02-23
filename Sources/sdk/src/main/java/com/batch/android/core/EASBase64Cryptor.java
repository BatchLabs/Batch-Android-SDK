package com.batch.android.core;

import android.util.Base64;

/**
 * EAS Base 64 cryptor
 *
 */
final class EASBase64Cryptor extends EASCryptor {

    private static final String TAG = "EASBase64Cryptor";

    /**
     * @param key
     */
    protected EASBase64Cryptor(String key) {
        super(key);
    }

    // ---------------------------------------------->

    @Override
    public byte[] encrypt(byte[] tocrypt) {
        try {
            return Base64.encode(super.encrypt(tocrypt), Base64.DEFAULT);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while encrypting AES bytes", e);
            return null;
        }
    }

    @Override
    public String encrypt(String tocrypt) {
        try {
            return ByteArrayHelper.getUTF8String(encrypt(ByteArrayHelper.getUTF8Bytes(tocrypt)));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while encrypting AES string", e);
            return null;
        }
    }

    @Override
    public byte[] decrypt(byte[] crypted) {
        try {
            return super.decrypt(Base64.decode(crypted, Base64.DEFAULT));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while decrypting AES bytes", e);
            return null;
        }
    }

    @Override
    public String decrypt(String string) {
        try {
            return ByteArrayHelper.getUTF8String(decrypt(ByteArrayHelper.getUTF8Bytes(string)));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while decrypting AES string", e);
            return null;
        }
    }

    @Override
    public byte[] decryptToByte(String string) {
        try {
            return decrypt(ByteArrayHelper.getUTF8Bytes(string));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while decrypting AES string to bytes", e);
            return null;
        }
    }
}
