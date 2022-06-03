package com.batch.android.metrics;

import android.content.Context;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.DateProvider;
import com.batch.android.core.Logger;
import com.batch.android.core.SystemDateProvider;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.Webservice;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Metric;
import com.batch.android.metrics.model.Observation;
import com.batch.android.post.MetricPostDataProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.webservice.listener.MetricWebserviceListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Module
@Singleton
public class MetricManager {

    private static final String TAG = "MetricManager";

    /**
     * Delay to wait before calling the metric webservice again after a fail
     */
    private static final int DEFAULT_RETRY_AFTER = 60_000; //ms

    /**
     * Delay to wait if other metrics are to send
     */
    private static final int DELAY_BEFORE_SENDING = 1000; //ms

    /**
     * List of metrics registered
     */
    private final List<Metric<?>> metrics = new ArrayList<>();

    /**
     * Flag indicating whether we has started sending metrics
     */
    private final AtomicBoolean isSending = new AtomicBoolean();

    /**
     * Single thread scheduled executor
     */
    private final ScheduledExecutorService sendExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Timestamp to wait before metrics webservice will be available again
     */
    private long nextMetricServiceAvailableTimestamp;

    /**
     * System date provider
     */
    private final DateProvider dateProvider = new SystemDateProvider();

    @Provide
    public static MetricManager provide() {
        return new MetricManager();
    }

    public void addMetric(Metric<?> metric) {
        synchronized (metrics) {
            this.metrics.add(metric);
        }
    }

    /**
     * Get metrics to send
     *
     * @return metrics
     */
    private List<Metric<?>> getMetricsToSend() {
        synchronized (metrics) {
            List<Metric<?>> metricsToSend = new ArrayList<>();
            for (Metric<?> metric : metrics) {
                if (metric.hasChildren()) {
                    for (Object child : metric.getChildren().values()) {
                        if (child instanceof Counter) {
                            Counter counter = (Counter) child;
                            if (counter.hasChanged()) {
                                metricsToSend.add(new Counter((Counter) child));
                                counter.reset();
                            }
                        } else {
                            Observation observation = ((Observation) child);
                            if (observation.hasChanged()) {
                                metricsToSend.add(new Observation((Observation) child));
                                observation.reset();
                            }
                        }
                    }
                } else {
                    if (metric.hasChanged()) {
                        if (metric instanceof Counter) {
                            metricsToSend.add(new Counter((Counter) metric));
                        } else {
                            metricsToSend.add(new Observation((Observation) metric));
                        }
                        metric.reset();
                    }
                }
            }
            return metricsToSend;
        }
    }

    /**
     * Check if the metric webservice is available.
     *
     * @return true if service is available or false if we have to wait
     */
    private boolean isMetricServiceAvailable() {
        return dateProvider.getCurrentDate().getTime() >= nextMetricServiceAvailableTimestamp;
    }

    /**
     * Send the metrics
     */
    public void sendMetrics() {
        if (isSending.get()) {
            // We are already sending metrics
            return;
        }

        if (!isMetricServiceAvailable()) {
            // Server looks like overloaded, we wait
            return;
        }

        isSending.set(true);
        Context context = RuntimeManagerProvider.get().getContext();

        sendExecutor.schedule(
            () -> {
                List<Metric<?>> metricsToSend = getMetricsToSend();
                if (metricsToSend.isEmpty()) {
                    return;
                }
                MetricPostDataProvider dataProvider = new MetricPostDataProvider(metricsToSend);
                TaskRunnable runnable = WebserviceLauncher.initMetricWebservice(
                    context,
                    dataProvider,
                    new MetricWebserviceListener() {
                        @Override
                        public void onSuccess() {
                            Logger.internal(TAG, "Metrics sent with success.");
                            isSending.set(false);
                        }

                        @Override
                        public void onFailure(Webservice.WebserviceError error) {
                            Logger.internal(TAG, "Fail sending metrics.");
                            long retryAfter = error.getRetryAfterInMillis() != 0
                                ? error.getRetryAfterInMillis()
                                : DEFAULT_RETRY_AFTER;
                            nextMetricServiceAvailableTimestamp = dateProvider.getCurrentDate().getTime() + retryAfter;
                            isSending.set(false);
                        }
                    }
                );
                if (runnable != null) {
                    runnable.run();
                }
            },
            DELAY_BEFORE_SENDING,
            TimeUnit.MILLISECONDS
        );
    }
}
