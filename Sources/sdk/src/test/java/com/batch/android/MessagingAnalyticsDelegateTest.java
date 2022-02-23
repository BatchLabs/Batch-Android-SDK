package com.batch.android;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MessagingAnalyticsDelegateTest {

    @Test
    public void testNullDeserialization() {
        MessagingAnalyticsDelegate delegate = new MessagingAnalyticsDelegate(null, null, null, null, null);
        delegate.restoreState(null);
    }

    @Test
    public void testSerialization() {
        MessagingAnalyticsDelegate delegate = new MessagingAnalyticsDelegate(null, null, null, null, null);
        delegate.calledMethods.add("foo");
        delegate.calledMethods.add("BaR");

        Bundle outState = new Bundle();
        delegate.onSaveInstanceState(outState);

        delegate = new MessagingAnalyticsDelegate(null, null, null, null, null);
        Assert.assertEquals(delegate.calledMethods.size(), 0);

        delegate.restoreState(outState);
        Assert.assertEquals(delegate.calledMethods.size(), 2);
        Assert.assertTrue(delegate.calledMethods.contains("foo"));
        Assert.assertTrue(delegate.calledMethods.contains("BaR"));
        Assert.assertFalse(delegate.calledMethods.contains("Baz"));
    }
}
