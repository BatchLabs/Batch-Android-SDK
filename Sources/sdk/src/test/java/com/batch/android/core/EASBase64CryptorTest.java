package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test EAS Base 64 Cryptor
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EASBase64CryptorTest {

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecrypt() throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);

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
            "4SDkS5a636psEP7cyhJKnpTr3upYEdG8e5OL/dvf9n93QLlJ4Ti+Gsosk7cNkkyyO54IsUFZVeyi8w0A4tJwwheM426bVTvPlHlEssDsc+4=";
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64, "1234567890123456");
        String decrypted = cryptor.decrypt(encrypted);

        assertEquals(
            "{\"header\":{\"version\":\"1.0\",\"status\":\"OK\"},\"body\":{\"status\":\"INVALID\"}}",
            decrypted
        );
    }
}
