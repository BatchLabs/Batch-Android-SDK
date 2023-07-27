package com.batch.android.core;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestBase32Decode {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "0625sq4758psyrgv8yv93ka4ee88rgpa",
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
                },
                { "nananana", new byte[] { (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa } },
            }
        );
    }

    private final String input;
    private final byte[] expected;

    public TestBase32Decode(String input, byte[] expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void test() throws Base32Encoding.DecodeException {
        Base32Encoding encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);

        ByteArrayOutputStream output = encoding.decode(input);
        byte[] outputBytes = output.toByteArray();

        assertArrayEquals(expected, outputBytes);
    }
}
