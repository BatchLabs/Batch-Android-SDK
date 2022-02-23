package com.batch.android.core;

import android.util.Base64;
import com.batch.android.core.CryptorFactory.CryptorType;
import java.net.HttpURLConnection;
import java.util.Random;

/**
 * Wrapper of cryptor that can read and write stuff from/for webservices
 *
 */
class WebserviceCryptor {

    /**
     * Part of the private key (base 64 EAS encoded)
     */
    private static final String PRIVATE_KEY_PART = "JY+Cn0qwsdiDNm7bRMKW8A=="; //33rM

    /**
     * Part of the private key v2 (base 64 EAS encoded)
     */
    private static final String PRIVATE_KEY_PART_V2 = "cJCf4WfJaP7qOvOxkZmYCQ=="; //4HVJ

    /**
     * Version of the cryptor, used into the public key (must be 1 char only)
     */
    private static final String VERSION = "2";

    /**
     * Type of cryptor to use
     */
    private CryptorType cryptorType;

    // -------------------------------------------->

    /**
     * @param cryptorType value of the cryptor type (0, 1, 2...)
     */
    protected WebserviceCryptor(int cryptorType) {
        this(CryptorType.fromValue(cryptorType));
    }

    /**
     * @param cryptorType
     */
    protected WebserviceCryptor(CryptorType cryptorType) {
        if (cryptorType == null) {
            throw new NullPointerException("Null type");
        }

        this.cryptorType = cryptorType;
    }

    // --------------------------------------------->

    /**
     * Decrypt the data according to the key in the given data
     *
     * @param data
     * @param ws   the webservice that received the data
     * @return
     * @throws Exception
     */
    public byte[] decryptData(byte[] data, Webservice ws, final HttpURLConnection connection) throws Exception {
        if (data == null) {
            throw new NullPointerException("Null data");
        }

        String dataString = ByteArrayHelper.getUTF8String(data);
        if (dataString.length() <= 8) {
            throw new IllegalArgumentException("The data as string should be at least 9 char long");
        }

        String publicKey = dataString.substring(0, 8);
        return decryptDataForVersion(
            connection.getHeaderField("X-Batch-Content-Cipher"),
            publicKey,
            dataString.substring(8),
            ws
        );
    }

    /**
     * Decrypt data for the given version of crypting (contains in the public key)
     *
     * @param version   version extracted from the public key
     * @param publicKey the complete public key
     * @param data      the data to decrypt
     * @param ws        the webservice that received the data
     * @return
     */
    private byte[] decryptDataForVersion(String version, String publicKey, String data, Webservice ws) {
        if ("2".equals(version)) {
            Cryptor cryptor = CryptorFactory.getCryptorForType(cryptorType, buildKeyV2(publicKey, ws));
            return cryptor.decryptToByte(data);
        } else {
            Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorType.EAS_BASE64, buildKey(publicKey, ws));
            return cryptor.decryptToByte(data);
        }
    }

    /**
     * Encrypt the data and prepend the public key
     *
     * @param data
     * @param ws   the webservice that want to send data
     * @return
     * @throws Exception
     */
    public byte[] encryptData(byte[] data, Webservice ws) throws Exception {
        String publicKey;
        Cryptor cryptor;
        if (ws.isDowngradedCipher) {
            publicKey = generatePublicKey("1", ws);
            cryptor = CryptorFactory.getCryptorForType(cryptorType, buildKey(publicKey, ws));
        } else {
            publicKey = generatePublicKey(VERSION, ws);
            cryptor = CryptorFactory.getCryptorForType(cryptorType, buildKeyV2(publicKey, ws));
        }

        String result = publicKey + cryptor.encrypt(ByteArrayHelper.getUTF8String(data));
        return ByteArrayHelper.getUTF8Bytes(result);
    }

    // ---------------------------------------------->

    /**
     * Build the private key
     *
     * @param ws the webservice that received the data
     * @return
     */
    private byte[] buildPrivateKey(Webservice ws) {
        String commonExternalKey = Parameters.COMMON_EXTERNAL_CRYPT_BASE_KEY;
        String privateKeyPart = PRIVATE_KEY_PART;

        byte[] common = Base64.decode(commonExternalKey, Base64.DEFAULT);
        byte[] priv = CryptorFactory.getCryptorForType(CryptorType.EAS_BASE64).decryptToByte(privateKeyPart);

        return ByteArrayHelper.concat(common, priv);
    }

    /**
     * Build the private key v2
     *
     * @param ws the webservice that received the data
     * @return
     */
    private byte[] buildPrivateKeyV2(Webservice ws) {
        String commonExternalKey = Parameters.COMMON_EXTERNAL_CRYPT_BASE_KEY_V2;
        String privateKeyPart = PRIVATE_KEY_PART_V2;

        byte[] common = Base64.decode(commonExternalKey, Base64.DEFAULT);
        byte[] priv = CryptorFactory.getCryptorForType(CryptorType.EAS_BASE64).decryptToByte(privateKeyPart);

        return ByteArrayHelper.concat(common, priv);
    }

    /**
     * Build the key to crypt/decrypt
     *
     * @param publicKey
     * @param ws        the webservice that received the data
     * @return
     */
    private String buildKey(String publicKey, Webservice ws) {
        return ByteArrayHelper.getUTF8String(buildPrivateKey(ws)) + publicKey;
    }

    /**
     * Build the key to crypt/decrypt
     *
     * @param publicKey
     * @param ws        the webservice that received the data
     * @return
     */
    private String buildKeyV2(String publicKey, Webservice ws) {
        return ByteArrayHelper.getUTF8String(buildPrivateKeyV2(ws)) + publicKey;
    }

    /**
     * Generate a new public key
     *
     * @return a key, 8 char long
     */
    private static String generatePublicKey(String version, Webservice ws) {
        return version + randomChars(7);
    }

    /**
     * Generates the given amount of random chars.
     *
     * @param count the count of random chars to generate
     * @return the given amount of random chars.
     */
    private static String randomChars(int count) {
        Random random = new Random();

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            boolean min = random.nextInt() % 2 == 0;
            builder.append((char) ((min ? 'a' : 'A') + random.nextInt(26)));
        }

        return builder.toString();
    }
}
