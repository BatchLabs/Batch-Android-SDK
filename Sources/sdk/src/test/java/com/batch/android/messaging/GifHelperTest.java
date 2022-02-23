package com.batch.android.messaging;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.TestUtils;
import com.batch.android.core.ForwardReadableInputStream;
import com.batch.android.messaging.gif.GifHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GifHelperTest {

    @Test
    public void testTheoreticalHeaderCheck() throws IOException {
        byte[] validTest1 = "GIF87aidqjfsidfjar".getBytes(StandardCharsets.US_ASCII);
        byte[] validTest2 = "GIF89aidqjf  dsfr".getBytes(StandardCharsets.US_ASCII);
        byte[] invalidTest = "qspofjaopedfkop".getBytes(StandardCharsets.US_ASCII);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(validTest1)) {
            Assert.assertTrue(
                GifHelper.isPotentiallyAGif(
                    new ForwardReadableInputStream(bis, GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK).getFirstBytes()
                )
            );
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(validTest2)) {
            Assert.assertTrue(
                GifHelper.isPotentiallyAGif(
                    new ForwardReadableInputStream(bis, GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK).getFirstBytes()
                )
            );
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(invalidTest)) {
            Assert.assertFalse(
                GifHelper.isPotentiallyAGif(
                    new ForwardReadableInputStream(bis, GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK).getFirstBytes()
                )
            );
        }
    }

    @Test
    public void testRealGifFile() throws IOException {
        try (InputStream gifStream = TestUtils.getResourceAsStream("test.gif")) {
            Assert.assertTrue(
                GifHelper.isPotentiallyAGif(
                    new ForwardReadableInputStream(gifStream, GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK).getFirstBytes()
                )
            );
        }

        try (InputStream gifStream = TestUtils.getResourceAsStream("test.jpg")) {
            Assert.assertFalse(
                GifHelper.isPotentiallyAGif(
                    new ForwardReadableInputStream(gifStream, GifHelper.NEEDED_BYTES_FOR_TYPE_CHECK).getFirstBytes()
                )
            );
        }
    }
}
