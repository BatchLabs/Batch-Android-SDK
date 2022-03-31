package com.batch.android.metrics.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Counter extends Metric<Counter> {

    private float value;

    public Counter(Counter counter) {
        super(counter.name);
        this.type = counter.type;
        this.value = counter.value;
        this.values = new ArrayList<>(counter.values);
        this.children = new ConcurrentHashMap<>(counter.children);
        this.labelNames = counter.labelNames;
        this.labelValues = counter.labelValues;
    }

    public Counter(String name) {
        super(name);
        type = Type.COUNTER;
        values = new ArrayList<>();
    }

    @Override
    protected Counter newChild(List<String> labels) {
        Counter counter = new Counter(name).labelNames(labelNames.toArray(new String[0]));
        counter.labelValues = labels;
        return counter;
    }

    @Override
    public void reset() {
        value = 0f;
        values.clear();
        children.clear();
    }

    public void inc() {
        value++;
        values.clear();
        values.add(value);
        update();
    }
}
