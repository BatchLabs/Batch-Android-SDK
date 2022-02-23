package com.batch.android.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test ByteArrayHelper
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ByteArrayHelperTest {

    @Test
    public void testBytesConcat() {
        byte[] partOne = "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿".getBytes(StandardCharsets.UTF_8);
        byte[] partTwo =
            "¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes(StandardCharsets.UTF_8);
        byte[] expected =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes(
                    StandardCharsets.UTF_8
                );

        byte[] output = ByteArrayHelper.concat(partOne, partTwo);
        assertArrayEquals(expected, output);

        output = ByteArrayHelper.concat(new byte[0], partTwo);
        assertArrayEquals(partTwo, output);

        output = ByteArrayHelper.concat(partOne, new byte[0]);
        assertArrayEquals(partOne, output);
    }

    @Test
    public void testBytesToString() {
        byte[] input =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes(
                    StandardCharsets.UTF_8
                );

        String string = ByteArrayHelper.getUTF8String(input);
        byte[] output = ByteArrayHelper.getUTF8Bytes(string);

        assertEquals(
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد",
            string
        );
        assertArrayEquals(input, output);
    }

    @Test
    public void testBytesToHex() {
        byte[] input =
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes(
                    StandardCharsets.UTF_8
                );
        String hex = ByteArrayHelper.bytesToHex(input);

        byte[] output = ByteArrayHelper.hexToBytes(hex);

        assertEquals(
            "2126C3A9222728C2A7C3A821C3A7C3A0292D3132353637383930C2B05F255E246DC3B9603D2A2F2E2C3F2C3B3A3DE280987BC2ABC387C3B8C387C3B87DE28094EFA3BFC3ABE28098C2B6C387C2A140237CC2BFC2A1C3AFC592E282ACC2A3C2B57EC2A9C2AEE280A0E284A2E289883C3EE289A4E289A5C3AAC38AE280A2CF80E280A1E28882C692C38CC2ACE2978AC39FE2889EC3B7E289A02BE288ABE2889AC2A2E280B0E28886E28891CEA9C2A5E2888FC2AAC5B85B5DC3A5E2809DE2809EE38383E382AFE98791E59E8BD0B8D0BBD0B4D0B6D981D98AD8ADD8A9D8ADD8AFD98AD8AF",
            hex
        );
        assertArrayEquals(input, output);
    }

    @Test
    public void testSHA1() {
        String sha1 = ByteArrayHelper.SHA1Base64Encoded(
            "!&é\"'(§è!çà)-12567890°_%^$mù`=*/.,?,;:=‘{«ÇøÇø}—ë‘¶Ç¡@#|¿¡ïŒ€£µ~©®†™≈<>≤≥êÊ•π‡∂ƒÌ¬◊ß∞÷≠+∫√¢‰∆∑Ω¥∏ªŸ[]å”„ック金型илджفيحةحديد".getBytes(
                    StandardCharsets.UTF_8
                )
        );

        assertEquals("pG+tIWKFrPjoZ4RHGLE4/mQllCE=\n", sha1);
    }
}
