package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test EAS Cryptor
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EASCryptorTest {

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecrypt() throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS);

        String string =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد";
        byte[] encrypted = cryptor.encrypt(string.getBytes(StandardCharsets.UTF_8));
        byte[] decrypted = cryptor.decrypt(encrypted);

        assertEquals(string, new String(decrypted, StandardCharsets.UTF_8));
    }
}
