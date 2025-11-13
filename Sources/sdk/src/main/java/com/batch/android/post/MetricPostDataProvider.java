package com.batch.android.post;

import com.batch.android.core.ByteArrayHelper;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.metrics.model.Metric;
import java.util.Collection;

public class MetricPostDataProvider implements PostDataProvider<Collection<Metric<?>>> {

    private static final String TAG = "MetricPostDataProvider";

    private final Collection<Metric<?>> metrics;

    public MetricPostDataProvider(Collection<Metric<?>> metrics) {
        if (metrics == null) {
            throw new NullPointerException("Metrics collection is null");
        }
        this.metrics = metrics;
    }

    @Override
    public Collection<Metric<?>> getRawData() {
        return metrics;
    }

    @Override
    public byte[] getData() {
        JSONArray payload = new JSONArray();
        try {
            for (Metric<?> metric : metrics) {
                payload.put(metric.toJson());
            }
            String payloadString = payload.toString();
            return ByteArrayHelper.getUTF8Bytes(payloadString);
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not serialize metrics payload to JSON", e);
            return new byte[0];
        }
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public boolean isEmpty() {
        return this.metrics.isEmpty();
    }
}
