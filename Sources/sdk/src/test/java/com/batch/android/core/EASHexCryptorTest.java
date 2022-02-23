package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test EAS Hex Cryptor
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EASHexCryptorTest {

    /**
     * Test encrypt & decrypt a string
     *
     * @throws Exception
     */
    @Test
    public void testEncryptDecrypt() throws Exception {
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_HEX);

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
            "EAD9703181B1358E12DD0BB2EF7DCE2FEFCF9E4B7C5D82DA5BFD0F50E30AB74F1F6000F8F4F71FBC243F537CDB2A7D2F";
        Cryptor cryptor = CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_HEX, "1234567890123456");
        String decrypted = cryptor.decrypt(encrypted);

        assertEquals("Coucou mec, c'est trop trop cool.", decrypted);
    }
}
