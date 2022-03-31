package com.batch.android.metrics.model;

import static java.lang.System.currentTimeMillis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Observation extends Metric<Observation> {

    private long startTime;

    private AtomicBoolean observing = new AtomicBoolean();

    public Observation(String name) {
        super(name);
        type = Type.OBSERVATION;
        values = new ArrayList<>();
    }

    public Observation(Observation observation) {
        super(observation.name);
        this.type = observation.type;
        this.values = observation.values;
        this.values = new ArrayList<>(observation.values);
        this.children = new ConcurrentHashMap<>(observation.children);
        this.labelNames = observation.labelNames;
        this.labelValues = observation.labelValues;
        this.observing = observation.observing;
        this.startTime = observation.startTime;
    }

    @Override
    protected Observation newChild(List<String> labels) {
        Observation observation = new Observation(name).labelNames(labelNames.toArray(new String[0]));
        observation.labelValues = labels;
        return observation;
    }

    @Override
    public void reset() {
        values.clear();
        children.clear();
    }

    public void startTimer() {
        startTime = currentTimeMillis();
        observing.set(true);
    }

    public void observeDuration() {
        observing.set(false);
        float duration = (currentTimeMillis() - startTime) / 1000f;
        values.add(duration);
        update();
    }

    public boolean isObserving() {
        return observing.get();
    }
}
