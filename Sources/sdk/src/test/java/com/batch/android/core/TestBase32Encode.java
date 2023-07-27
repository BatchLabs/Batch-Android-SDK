package com.batch.android.core;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestBase32Encode {

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    new byte[] {
                        0x01,
                        (byte) 0x84,
                        0x5c,
                        (byte) 0xdc,
                        (byte) 0x87,
                        0x2a,
                        0x2d,
                        (byte) 0x9f,
                        0x62,
                        0x1b,
                        0x47,
                        (byte) 0xb6,
                        (byte) 0x91,
                        (byte) 0xcd,
                        0x44,
                        0x73,
                        (byte) 0x90,
                        (byte) 0x8c,
                        0x42,
                        (byte) 0xca,
                    },
                    "0625sq4758psyrgv8yv93ka4ee88rgpa",
                },
                { new byte[] { (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa }, "nananana" },
            }
        );
    }

    private final byte[] input;
    private final String expected;

    public TestBase32Encode(byte[] input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() throws Base32Encoding.EncodeException, UnsupportedEncodingException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Base32Encoding encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        encoding.encode(output, input);

        String encoded = output.toString("UTF-8");
        assertEquals(expected, encoded);
    }
}
