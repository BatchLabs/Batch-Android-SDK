package com.batch.android.core;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ForwardReadableInputStreamTest {

    @Test
    public void testForwardReading() throws IOException {
        byte[] testBytes = "DEADBEEF".getBytes(StandardCharsets.ISO_8859_1);

        try (
            ByteArrayInputStream bis = new ByteArrayInputStream(testBytes);
            ForwardReadableInputStream fris = new ForwardReadableInputStream(bis, 4)
        ) {
            Assert.assertArrayEquals(new int[] { 'D', 'E', 'A', 'D' }, fris.getFirstBytes());

            byte[] output = new byte[8];

            int i = 0;
            int b;
            while ((b = fris.read()) != -1) {
                Assert.assertTrue(i < testBytes.length);
                Assert.assertFalse(b > 255 || b < 0);
                output[i] = (byte) b;
                i++;
            }

            Assert.assertArrayEquals(testBytes, output);
        }
    }

    @Test
    public void testNotBigEnoughStream() throws IOException {
        byte[] testBytes = "DE".getBytes(StandardCharsets.ISO_8859_1);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(testBytes)) {
            try {
                new ForwardReadableInputStream(bis, 4);
                Assert.fail("An IOException should have been thrown");
            } catch (IOException expected) {}
        }
    }
}
