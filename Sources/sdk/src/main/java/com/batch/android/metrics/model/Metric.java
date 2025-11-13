package com.batch.android.metrics.model;

import com.batch.android.di.providers.MetricManagerProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Metric<Child> {

    protected interface Type {
        String COUNTER = "counter";
        String OBSERVATION = "observation";
    }

    protected final String name;

    protected String type;

    protected List<Float> values;

    protected List<String> labelNames;

    protected List<String> labelValues;

    protected ConcurrentMap<List<String>, Child> children = new ConcurrentHashMap<>();

    public Metric(String name) {
        this.name = name;
    }

    public Child register() {
        MetricManagerProvider.get().addMetric(this);
        return (Child) this;
    }

    public Child labelNames(String... labels) {
        this.labelNames = Arrays.asList(labels);
        return (Child) this;
    }

    public Child labels(String... labels) {
        Child child = this.children.get(Arrays.asList(labels));
        if (child == null) {
            List<String> labelsValues = Arrays.asList(labels);
            child = newChild(labelsValues);
            this.children.put(labelsValues, child);
        }
        return child;
    }

    public abstract void reset();

    protected abstract Child newChild(List<String> labels);

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("type", type);

        JSONArray valuesArray = new JSONArray();
        if (values != null) {
            for (Float value : values) {
                if (value != null) {
                    valuesArray.put(value.doubleValue());
                }
            }
        }
        json.put("values", valuesArray);

        if (
            labelNames != null &&
            labelValues != null &&
            labelNames.size() == labelValues.size() &&
            !labelNames.isEmpty()
        ) {
            JSONObject labels = new JSONObject();
            for (int i = 0; i < labelNames.size(); i++) {
                String key = labelNames.get(i);
                String value = labelValues.get(i);
                if (key != null && value != null) {
                    labels.put(key, value);
                }
            }

            if (labels.length() > 0) {
                json.put("labels", labels);
            }
        }

        return json;
    }

    protected void update() {
        MetricManagerProvider.get().sendMetrics();
    }

    public boolean hasChanged() {
        return values.size() > 0;
    }

    public boolean hasChildren() {
        return children.size() > 0;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<Float> getValues() {
        return values;
    }

    public List<String> getLabelNames() {
        return labelNames;
    }

    public List<String> getLabelValues() {
        return labelValues;
    }

    public ConcurrentMap<List<String>, Child> getChildren() {
        return children;
    }
}
