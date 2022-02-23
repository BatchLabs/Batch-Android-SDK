package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the {@link BatchPushData} object
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class BatchPushDataTest {

    private Context appContext;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
    }

    /**
     * Test that null pointer exeptions are correctly thrown
     *
     * @throws Exception
     */
    @Test
    public void testNullPointers() throws Exception {
        try {
            new BatchPushData(null, new Intent());
            fail();
        } catch (NullPointerException e) {
            // Normal
        }

        try {
            new BatchPushData(appContext, null);
            fail();
        } catch (NullPointerException e) {
            // Normal
        }
    }

    /**
     * Test that illegal argument exception is correctly thrown if intent is not a batch push one
     *
     * @throws Exception
     */
    @Test
    public void testBadIntent() throws Exception {
        try {
            new BatchPushData(appContext, new Intent());
        } catch (IllegalArgumentException e) {
            // Normal
        }
    }

    /**
     * Test data retrieving
     *
     * @throws Exception
     */
    @Test
    public void testCompleteIntent() throws Exception {
        String deeplink = "sdoifhsoif://oisdhf";
        String largeIconURL = "http://osdihsfoih.com/jqiopqj.png";
        String bigPictureURL = "http://oisdfhsof.com/sdfhsf.png";

        JSONObject batchData = new JSONObject();
        batchData.put("l", deeplink);

        JSONObject largeIconObject = new JSONObject();
        largeIconObject.put("u", largeIconURL);
        batchData.put("bi", largeIconObject);

        JSONObject bigPictureObject = new JSONObject();
        bigPictureObject.put("u", bigPictureURL);
        batchData.put("bp", bigPictureObject);

        Intent intent = new Intent();
        intent.putExtra("com.batch", batchData.toString());

        BatchPushData pushData = new BatchPushData(appContext, intent);

        assertTrue(pushData.hasDeeplink());
        assertEquals(deeplink, pushData.getDeeplink());

        assertTrue(pushData.hasCustomLargeIcon());
        assertEquals(largeIconURL, pushData.getCustomLargeIconURL());

        assertTrue(pushData.hasBigPicture());
        assertEquals(bigPictureURL, pushData.getBigPictureURL());
    }
}
