package com.batch.android.messaging;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WebViewHelperTest {

    @Test
    public void testAnalyticsID() {
        Assert.assertNull(WebViewHelper.getAnalyticsIDFromURL(""));
        Assert.assertNull(WebViewHelper.getAnalyticsIDFromURL("https://batch.com"));
        Assert.assertNull(WebViewHelper.getAnalyticsIDFromURL("https://batch.com/batchAnalyticsID=foo"));
        Assert.assertNull(WebViewHelper.getAnalyticsIDFromURL("https://batch.com/?batchAnalyticsid=foo"));
        Assert.assertEquals("foo", WebViewHelper.getAnalyticsIDFromURL("https://batch.com/?batchAnalyticsID=foo"));
        Assert.assertEquals(
            "foo",
            WebViewHelper.getAnalyticsIDFromURL("https://batch.com/index.html?batchAnalyticsID=foo")
        );
        Assert.assertEquals(
            "foo",
            WebViewHelper.getAnalyticsIDFromURL("https://batch.com/?test=test&batchAnalyticsID=foo")
        );
        Assert.assertEquals(
            "space example",
            WebViewHelper.getAnalyticsIDFromURL("https://batch.com/?batchAnalyticsID=space%20example")
        );
    }
}
