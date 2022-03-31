package com.batch.android.post;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Metric;
import com.batch.android.metrics.model.Observation;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for MetricPostDataProvider
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MetricPostDataProviderTest {

    @Test
    public void testData() {
        List<Metric<?>> metrics = new ArrayList<>();
        Counter counter = new Counter("counter_test_metric");
        Observation observation = new Observation("observation_test_metric");
        counter.inc();
        metrics.add(counter);
        metrics.add(observation);

        // prettier-ignore
        byte[] body = new byte[]{-110, -125, -90, 118, 97, 108, 117, 101, 115, -111, -54, 63, -128,
                0, 0, -92, 110, 97, 109, 101, -77, 99, 111, 117, 110, 116, 101, 114, 95, 116, 101,
                115, 116, 95, 109, 101, 116, 114, 105, 99, -92, 116, 121, 112, 101, -89, 99, 111,
                117, 110, 116, 101, 114, -125, -90, 118, 97, 108, 117, 101, 115, -112, -92, 110, 97,
                109, 101, -73, 111, 98, 115, 101, 114, 118, 97, 116, 105, 111, 110, 95, 116, 101,
                115, 116, 95, 109, 101, 116, 114, 105, 99, -92, 116, 121, 112, 101, -85, 111, 98,
                115, 101, 114, 118, 97, 116, 105, 111, 110};

        MetricPostDataProvider provider = new MetricPostDataProvider(metrics);
        assertEquals("application/msgpack", provider.getContentType());
        assertArrayEquals(metrics.toArray(), provider.getRawData().toArray());
        assertArrayEquals(body, provider.getData());
    }

    @Test
    public void testIsEmpty() {
        List<Metric<?>> metrics = new ArrayList<>();
        MetricPostDataProvider provider = new MetricPostDataProvider(metrics);
        assertTrue(provider.isEmpty());

        Counter counter = new Counter("test_metric");
        metrics.add(counter);
        provider = new MetricPostDataProvider(metrics);
        assertFalse(provider.isEmpty());
    }
}
