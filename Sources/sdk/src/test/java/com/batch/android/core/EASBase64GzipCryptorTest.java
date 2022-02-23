package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test EAS Base 64 Gzip Cryptor
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EASBase64GzipCryptorTest {

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecrypt() throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64_GZIP);

        String string =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد";
        String encrypted = cryptor.encrypt(string);
        String decrypted = cryptor.decrypt(encrypted);

        assertEquals(string, decrypted);
    }

    /**
     * Test decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testDecrypt() throws Exception {
        String encrypted =
            "Ro/5e5ta9GG7YIWF4BPEQvJfGjsqSJ9CtJb+1aLJfw0RL7V5mioiAhuUtyDCwt2Bwda9/0GQDImk+gYS46kLSPYqIv33DfXu+evQbPri27I=";
        Cryptor cryptor = CryptorFactory.getCryptorForType(
            CryptorFactory.CryptorType.EAS_BASE64_GZIP,
            "1234567890123456"
        );
        String decrypted = cryptor.decrypt(encrypted);

        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"status\":\"INVALID\"}}",
            decrypted
        );
    }
}
