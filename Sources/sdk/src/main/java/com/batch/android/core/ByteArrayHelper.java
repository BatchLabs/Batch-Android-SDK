package com.batch.android.core;

import android.util.Base64;
import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper to manipulate byte arrays and string encoding
 *
 */
public class ByteArrayHelper {

    /**
     * Const to UTF-8
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Array of hex chars
     */
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    // ----------------------------------------->

    /**
     * Concat 2 byte arrays
     *
     * @param original
     * @param toConcat
     * @return
     */
    public static byte[] concat(byte[] original, byte[] toConcat) {
        byte[] result = new byte[original.length + toConcat.length];

        // copy a to result
        System.arraycopy(original, 0, result, 0, original.length);
        // copy b to result
        System.arraycopy(toConcat, 0, result, original.length, toConcat.length);

        return result;
    }

    // ------------------------------------------>

    /**
     * Get UTF-8 string value
     *
     * @param bytes
     * @return
     */
    public static String getUTF8String(byte[] bytes) {
        try {
            return new String(bytes, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported");
        }
    }

    /**
     * Convert string to UTF-8 bytes
     *
     * @param string
     * @return
     */
    public static byte[] getUTF8Bytes(String string) {
        try {
            return string.getBytes(UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported");
        }
    }

    // ---------------------------------------------->

    /**
     * Convert an array of byte to an hex string
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    /**
     * Convert an hex string to a byte array
     *
     * @param s
     * @return
     */
    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int char1 = (Character.digit(s.charAt(i), 16) << 4);
            int char2 = Character.digit(s.charAt(i + 1), 16);
            data[i / 2] = (byte) (char1 + char2);
        }
        return data;
    }

    // ---------------------------------------------->

    /**
     * Return the base64 encoded SHA1 digest of a string
     *
     * @param s
     * @return
     */
    public static String SHA1Base64Encoded(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("s==null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            return getUTF8String(Base64.encode(digest.digest(bytes), Base64.DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA1 is not supported");
        }
    }

    public static byte[] fromInputStream(@NonNull InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }
            baos.flush();
            return baos.toByteArray();
        }
    }
}
