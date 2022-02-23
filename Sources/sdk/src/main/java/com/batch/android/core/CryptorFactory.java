package com.batch.android.core;

import android.util.Base64;
import java.util.Locale;

/**
 * A factory to retrieve cryptor by type
 *
 */
class CryptorFactory {

    /**
     * The part of the default private key used to build the entire key (base 64 encoded)
     */
    private static final String DEFAULT_PRIVATE_KEY_PART = "RmY5bWJTd2J1U3Jw"; //Ff9mbSwbuSrp

    // -------------------------------------------------->

    /**
     * Return a cryptor for the given type
     *
     * @param typeString string representation of the type
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForType(String typeString) {
        return getCryptorForType(typeString, null);
    }

    /**
     * Return a cryptor for the given type
     *
     * @param typeString string representation of the type
     * @param key        the key to use
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForType(String typeString, String key) {
        return getCryptorForType(CryptorType.fromString(typeString), key);
    }

    /**
     * Return a cryptor for the given type value
     *
     * @param value
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForTypeValue(int value) {
        return getCryptorForTypeValue(value, null);
    }

    /**
     * Return a cryptor for the given type value
     *
     * @param value
     * @param key   the key to use
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForTypeValue(int value, String key) {
        return getCryptorForType(CryptorType.fromValue(value), key);
    }

    /**
     * Return a cryptor for the given type
     *
     * @param type
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForType(CryptorType type) {
        return getCryptorForType(type, null);
    }

    /**
     * Return a cryptor for the given type and key
     *
     * @param type
     * @param key  the key to use
     * @return cryptor if found, null otherwise
     */
    protected static Cryptor getCryptorForType(CryptorType type, String key) {
        if (type == null) {
            return null;
        }

        if (key == null) {
            key = ByteArrayHelper.getUTF8String(buildDefaultKey());
        }

        switch (type) {
            case EAS:
                return new EASCryptor(key);
            case EAS_HEX:
                return new EASHexCryptor(key);
            case EAS_BASE64:
                return new EASBase64Cryptor(key);
            case EAS_BASE64_GZIP:
                return new EASBase64GzipCryptor(key);
            default:
                return null;
        }
    }

    /**
     * Build the default key for encryption
     *
     * @return
     */
    private static byte[] buildDefaultKey() {
        byte[] common = Base64.decode(Parameters.COMMON_INTERNAL_CRYPT_BASE_KEY, Base64.DEFAULT);
        byte[] priv = Base64.decode(DEFAULT_PRIVATE_KEY_PART, Base64.DEFAULT);

        return ByteArrayHelper.concat(common, priv);
    }

    // -------------------------------------------->

    /**
     * Type of cryptors
     *
     */
    protected enum CryptorType {
        /**
         * EAS cryptor
         */
        EAS(2),
        /**
         * EAS Hexa cryptor
         */
        EAS_HEX(3),
        /**
         * EAS Base 64 cryptor
         */
        EAS_BASE64(4),
        /**
         * EAS Base 64 GZIP cryptor
         */
        EAS_BASE64_GZIP(5);

        // -------------------------------------->

        private int value;

        CryptorType(int value) {
            this.value = value;
        }

        // -------------------------------------->

        public int getValue() {
            return value;
        }

        // -------------------------------------->

        /**
         * retrieve the cryptortype for the given string representation
         *
         * @param input
         * @return CryptorType if found, null otherwise
         */
        public static CryptorType fromString(String input) {
            try {
                return valueOf(input.toUpperCase(Locale.US));
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * retrieve the cryptor type for given value
         *
         * @param value
         * @return CryptorType if found, null otherwise
         */
        public static CryptorType fromValue(int value) {
            for (CryptorType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }

            return null;
        }
    }
}
