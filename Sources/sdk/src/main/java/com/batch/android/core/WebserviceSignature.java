package com.batch.android.core;

import android.annotation.SuppressLint;
import android.util.Base64;
import java.security.Key;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class WebserviceSignature {

    private static final String TAG = "WebserviceSignature";

    /**
     * Part of the private signature key (base 64 EAS encoded)
     */
    private static final String PRIVATE_SIGNATURE_KEY_PART = "UJYLuqVx7jwx/9hIJP0U/+PwG8rZjPfC9q0WTpGGfoo="; //@kE]ZQMtZv-3OikNIVZ&aOq2

    public static String encryptSignatureData(String data) {
        try {
            SecretKeySpec privateKey = new SecretKeySpec(buildPrivateSignatureKey(), "HmacSHA256");
            byte[] signature = Base64.encode(
                encryptHMAC(privateKey, ByteArrayHelper.getUTF8Bytes(data)),
                Base64.DEFAULT
            );

            return ByteArrayHelper.getUTF8String(signature);
        } catch (Exception e) {
            Logger.error(TAG, "Error while encrypting HmacSHA256 bytes", e);
            return null;
        }
    }

    /**
     * Build the private signature key
     *
     * @return
     */
    private static byte[] buildPrivateSignatureKey() {
        String commonExternalKey = Parameters.COMMON_EXTERNAL_CRYPT_SIGNATURE_KEY;
        String privateKeyPart = PRIVATE_SIGNATURE_KEY_PART;

        byte[] common = Base64.decode(commonExternalKey, Base64.DEFAULT);
        byte[] priv = CryptorFactory
            .getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64)
            .decryptToByte(privateKeyPart);

        return ByteArrayHelper.concat(common, priv);
    }

    /**
     * Encrypt a String with the AES encryption standard.
     *
     * @param value the data to encrypt
     * @return An encrypted data
     */
    private static byte[] encryptHMAC(Key key, byte[] value) throws Exception {
        @SuppressLint("GetInstance")
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        return mac.doFinal(value);
    }
}
