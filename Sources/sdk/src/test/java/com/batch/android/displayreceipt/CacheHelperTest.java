package com.batch.android.displayreceipt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CacheHelperTest {

    private Context appContext;
    private File testCache;
    private File testFile;
    private byte[] testData;

    public CacheHelperTest() {
        appContext = ApplicationProvider.getApplicationContext();
        testCache = new File(appContext.getCacheDir(), "com.batch.displayreceipts");
        testFile = new File(testCache, "test-write.bin");
        testData =
            (
                "Lorem ipsum dolor sit amet," +
                " consectetur adipiscing elit," +
                " sed do eiusmod tempor incididunt " +
                "ut labore et dolore magna aliqua."
            ).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Before
    public void setUp() {}

    @After
    public void tearDown() {
        testFile.delete();
    }

    @Test
    public void testWrite() {
        File result = CacheHelper.write(appContext, 321654L, testData);
        assertNotNull(result);

        assertTrue(testCache.isDirectory());
        assertTrue(result.exists());
        assertTrue(result.isFile());
        assertEquals(123, result.length());
        result.delete();
    }

    @Test
    public void testDeleteAll() {
        File result = CacheHelper.write(appContext, 321654L, testData);
        assertNotNull(result);
        assertTrue(result.exists());

        assertTrue(CacheHelper.deleteAll(appContext));
        assertFalse(result.exists());
        assertFalse(testCache.exists());
    }

    @Test
    public void testRead() {
        assertTrue(testCache.mkdirs());
        try {
            OutputStream out = new FileOutputStream(testFile, false);
            out.write(testData);
            out.close();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertTrue(testFile.exists());

        byte[] readData = CacheHelper.read(testFile);
        assertArrayEquals(testData, readData);
    }

    @Test
    public void testReadFilesMax() throws IOException {
        String filename1 = Long.toString(System.currentTimeMillis() - 3659800L);
        String filename2 = Long.toString(System.currentTimeMillis() - 2266500L);
        String filename3 = Long.toString(System.currentTimeMillis() - 1190L);
        String filename4 = Long.toString(System.currentTimeMillis() - 20150L);
        String filename5 = Long.toString(System.currentTimeMillis() - 6546548L);
        String filename6 = Long.toString(System.currentTimeMillis() - 15484L);
        File testReceipt1 = new File(testCache, filename1 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt2 = new File(testCache, filename2 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt3 = new File(testCache, filename3 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt4 = new File(testCache, filename4 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt5 = new File(testCache, filename5 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt6 = new File(testCache, filename6 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");

        assertTrue(testCache.mkdirs());
        assertTrue(testReceipt1.createNewFile());
        assertTrue(testReceipt2.createNewFile());
        assertTrue(testReceipt3.createNewFile());
        assertTrue(testReceipt4.createNewFile());
        assertTrue(testReceipt5.createNewFile());
        assertTrue(testReceipt6.createNewFile());

        List<File> receipts = CacheHelper.getCachedFiles(appContext, false);

        assertNotNull(receipts);
        // Max is 5
        assertEquals(5, receipts.size());

        List<File> debugReceipts = CacheHelper.getCachedFiles(appContext, true);
        assertNotNull(debugReceipts);
        assertEquals(6, debugReceipts.size());

        testReceipt1.delete();
        testReceipt2.delete();
        testReceipt3.delete();
        testReceipt4.delete();
        testReceipt5.delete();
        testReceipt6.delete();
    }

    @Test
    public void testReadFilesOrder() throws IOException {
        String filename1 = Long.toString(System.currentTimeMillis() - 3600000L);
        String filename2 = Long.toString(System.currentTimeMillis() - 2400000L);
        String filename3 = Long.toString(System.currentTimeMillis() - 1000L);
        String filename4 = Long.toString(System.currentTimeMillis() - 10L);
        File testReceipt1 = new File(testCache, filename2 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin");
        File testReceipt2 = new File(testCache, filename4 + "-d095a149-d15e-459d-a4a0-f999ebd71993.bin");
        File testReceipt3 = new File(testCache, "null-fb73fea5-1b33-4475-ae69-07fc12021d03.bin"); // Null
        File testReceipt4 = new File(testCache, filename3 + "-1af0f7ca-534e-43d6-873d-81bcae798061");
        File testReceipt5 = new File(testCache, "a69509aef65540-88756088-b13f-4f27-9da6-3c3e8116caa5"); // Invalid
        File testReceipt6 = new File(testCache, "-987-88756088-b13f-4f27-9da6-3c3e8116caa5"); // Negative
        File testReceipt7 = new File(testCache, "654654654654654654654654654-88756088-b13f-4f27-9da6-3c3e8116caa5"); // Overflow
        File testReceipt8 = new File(testCache, filename1 + "-1af0f7ca-534e-43d6-873d-81bcae798061");

        assertTrue(testCache.mkdirs());
        assertTrue(testReceipt1.createNewFile());
        assertTrue(testReceipt2.createNewFile());
        assertTrue(testReceipt3.createNewFile());
        assertTrue(testReceipt4.createNewFile());
        assertTrue(testReceipt5.createNewFile());
        assertTrue(testReceipt6.createNewFile());
        assertTrue(testReceipt7.createNewFile());
        assertTrue(testReceipt8.createNewFile());

        List<File> receipts = CacheHelper.getCachedFiles(appContext, false);

        assertNotNull(receipts);
        assertEquals(4, receipts.size());

        // Check ordering
        assertEquals(filename1 + "-1af0f7ca-534e-43d6-873d-81bcae798061", receipts.get(0).getName());
        assertEquals(filename2 + "-1af0f7ca-534e-43d6-873d-81bcae798061.bin", receipts.get(1).getName());
        assertEquals(filename3 + "-1af0f7ca-534e-43d6-873d-81bcae798061", receipts.get(2).getName());
        assertEquals(filename4 + "-d095a149-d15e-459d-a4a0-f999ebd71993.bin", receipts.get(3).getName());

        assertFalse(testReceipt3.exists());
        assertFalse(testReceipt5.exists());
        assertFalse(testReceipt6.exists());
        assertFalse(testReceipt7.exists());

        testReceipt1.delete();
        testReceipt2.delete();
        testReceipt4.delete();
        testReceipt8.delete();
    }
}
