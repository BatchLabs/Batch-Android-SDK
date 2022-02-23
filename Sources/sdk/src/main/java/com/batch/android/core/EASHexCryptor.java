package com.batch.android.core;

/**
 * EAS Cryptor with hex string output
 *
 */
final class EASHexCryptor extends EASCryptor {

    private static final String TAG = "EASHexCryptor";

    /**
     * @param key
     */
    protected EASHexCryptor(String key) {
        super(key);
    }

    // ----------------------------------------------->

    @Override
    public byte[] encrypt(byte[] tocrypt) {
        try {
            return ByteArrayHelper.getUTF8Bytes(ByteArrayHelper.bytesToHex(super.encrypt(tocrypt)));
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
            return super.decrypt(ByteArrayHelper.hexToBytes(ByteArrayHelper.getUTF8String(crypted)));
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
            return super.decrypt(ByteArrayHelper.hexToBytes(string));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while decrypting AES string to bytes", e);
            return null;
        }
    }
}
