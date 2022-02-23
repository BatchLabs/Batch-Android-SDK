package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Webservice tests
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class WebserviceSignatureTest {

    @Test
    public void testEncrypt() throws Exception {
        String string =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد";
        String encrypted = WebserviceSignature.encryptSignatureData(string);

        assertEquals("UhgFoGqfScB40XgrUdrxla74Ue9H87bH28e3Co+JZoU=\n", encrypted);
    }
}
