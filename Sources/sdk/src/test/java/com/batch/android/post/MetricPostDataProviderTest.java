package com.batch.android.post;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONObject;
import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Metric;
import com.batch.android.metrics.model.Observation;
import java.nio.charset.StandardCharsets;
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
    public void testData() throws Exception {
        List<Metric<?>> metrics = new ArrayList<>();
        Counter counter = new Counter("counter_test_metric");
        Observation observation = new Observation("observation_test_metric");
        counter.inc();
        metrics.add(counter);
        metrics.add(observation);

        MetricPostDataProvider provider = new MetricPostDataProvider(metrics);
        assertEquals("application/json", provider.getContentType());
        assertArrayEquals(metrics.toArray(), provider.getRawData().toArray());

        JSONArray actualPayload = new JSONArray(new String(provider.getData(), StandardCharsets.UTF_8));
        JSONArray expectedPayload = new JSONArray();
        expectedPayload.put(
            new JSONObject()
                .put("name", "counter_test_metric")
                .put("type", "counter")
                .put("values", new JSONArray().put(1))
        );
        expectedPayload.put(
            new JSONObject()
                .put("name", "observation_test_metric")
                .put("type", "observation")
                .put("values", new JSONArray())
        );

        assertEquals(expectedPayload.toString(), actualPayload.toString());
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
