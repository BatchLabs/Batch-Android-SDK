package com.batch.android.lisp;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper context that adds a caching layer
 * <p>
 * This is NOT thread safe
 */
public final class CachingContext implements EvaluationContext
{
    private EvaluationContext context;

    private Map<String, Value> cache;

    private static Value NULL_VALUE = new PrimitiveValue("");

    public CachingContext(EvaluationContext context)
    {
        this.context = context;
        this.cache = new HashMap<>();
    }

    @Override
    public Value resolveVariableNamed(String name)
    {
        Value cachedValue = cache.get(name);

        if (cachedValue == null) { // First pass for given `name`.
            Value resolved = context.resolveVariableNamed(name);

            if (resolved != null) {
                cache.put(name, resolved);
            } else {
                cache.put(name, NULL_VALUE);
            }

            return resolved;
        }

        if (cachedValue == CachingContext.NULL_VALUE) {
            // Cache hit, but it was null
            return null;
        }

        return cachedValue;
    }
}

