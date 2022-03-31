package com.batch.android.metrics.model;

import com.batch.android.di.providers.MetricManagerProvider;
import com.batch.android.msgpack.MessagePackHelper;
import com.batch.android.msgpack.core.MessageBufferPacker;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public void pack(MessageBufferPacker packer) throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("name", name);
        objectMap.put("type", type);
        objectMap.put("values", values);
        if (labelNames != null && labelValues != null && labelNames.size() == labelValues.size()) {
            Map<String, String> labels = new HashMap<>();
            for (int i = 0; i < labelNames.size(); i++) {
                labels.put(labelNames.get(i), labelValues.get(i));
            }
            objectMap.put("labels", labels);
        }
        MessagePackHelper.packObject(packer, objectMap);
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
