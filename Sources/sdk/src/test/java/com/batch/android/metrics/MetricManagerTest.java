package com.batch.android.metrics;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.di.DITest;
import com.batch.android.di.DITestUtils;
import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Metric;
import com.batch.android.metrics.model.Observation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class MetricManagerTest extends DITest {

    @Test
    public void testAddMetric() {
        MetricManager manager = PowerMockito.spy(DITestUtils.mockSingletonDependency(MetricManager.class, null));
        Counter counter = new Counter("counter_test_metric");
        manager.addMetric(counter);
        Mockito.verify(manager, Mockito.times(1)).addMetric(Mockito.argThat(new CounterMatcher(counter)));
    }

    @Test
    public void testGetMetricsToSend() throws Exception {
        MetricManager manager = PowerMockito.spy(DITestUtils.mockSingletonDependency(MetricManager.class, null));
        PowerMockito.doNothing().when(manager).sendMetrics();
        Counter counter = new Counter("counter_test_metric").register();
        counter.inc();

        Observation observation = new Observation("observation_test_metric").labelNames("label1", "label2").register();
        observation.labels("value1", "value2").startTimer();
        observation.labels("value1", "value2").observeDuration();
        observation.labels("value3", "value3").startTimer();

        //Making copy of metrics because the reset method will be called when getMetricsToSend is done
        List<Metric<?>> expected = new ArrayList<>();
        expected.add(new Counter(counter));
        expected.add(new Observation(observation.labels("value1", "value2")));

        List<Metric<?>> actual = Whitebox.invokeMethod(manager, "getMetricsToSend");

        Assert.assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            Metric<?> expectedMetric = expected.get(i);
            Metric<?> actualMetric = actual.get(i);
            Assert.assertEquals(expectedMetric.getName(), actualMetric.getName());
            Assert.assertEquals(expectedMetric.getType(), actualMetric.getType());
            Assert.assertEquals(expectedMetric.getLabelNames(), actualMetric.getLabelNames());
            Assert.assertEquals(expectedMetric.getLabelValues(), actualMetric.getLabelValues());
            Assert.assertEquals(expectedMetric.getValues(), actualMetric.getValues());
        }
    }

    @Test
    public void testIsWaiting() throws NoSuchFieldException, IllegalAccessException {
        MetricManager manager = DITestUtils.mockSingletonDependency(MetricManager.class, null);

        Field isWaitingField = MetricManager.class.getDeclaredField("isSending");
        isWaitingField.setAccessible(true);
        AtomicBoolean isSending = (AtomicBoolean) isWaitingField.get(manager);

        Counter counter = new Counter("counter_test_metric").register();
        Assert.assertFalse(isSending.get());
        counter.inc();
        Assert.assertTrue(isSending.get());
    }
}
