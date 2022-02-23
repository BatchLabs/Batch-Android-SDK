package com.batch.android.core;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * EAS Base 64 Gzip cryptor
 *
 */
final class EASBase64GzipCryptor extends EASCryptor {

    private static final String TAG = "EASBase64GzipCryptor";

    /**
     * @param key
     */
    protected EASBase64GzipCryptor(String key) {
        super(key);
    }

    // ---------------------------------------------->

    private byte[] gzip(byte[] togzip) throws IOException {
        try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream(togzip.length);
            GZIPOutputStream gzipOS = new GZIPOutputStream(bos)
        ) {
            gzipOS.write(togzip);
            gzipOS.close();

            return bos.toByteArray();
        }
    }

    private byte[] ungzip(byte[] gzipped) throws IOException {
        try (
            ByteArrayInputStream bis = new ByteArrayInputStream(gzipped);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPInputStream gzipIS = new GZIPInputStream(bis)
        ) {
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
            return bos.toByteArray();
        }
    }

    @Override
    public byte[] encrypt(byte[] tocrypt) {
        try {
            return Base64.encode(super.encrypt(gzip(tocrypt)), Base64.DEFAULT);
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
            return ungzip(super.decrypt(Base64.decode(crypted, Base64.DEFAULT)));
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
