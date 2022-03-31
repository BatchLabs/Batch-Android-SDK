package com.batch.android.metrics;

import com.batch.android.metrics.model.Counter;
import org.mockito.ArgumentMatcher;

public class CounterMatcher implements ArgumentMatcher<Counter> {

    private final Counter expected;

    public CounterMatcher(Counter counter) {
        this.expected = counter;
    }

    @Override
    public boolean matches(Counter argument) {
        return (
            expected.getName().equals(argument.getName()) &&
            expected.getType().equals(argument.getType()) &&
            expected.getValues().equals(argument.getValues())
        );
    }
}
