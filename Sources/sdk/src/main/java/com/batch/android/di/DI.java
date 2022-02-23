package com.batch.android.di;

import androidx.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class DI {

    private static final String TAG = "DI";

    // Singleton instance
    private static DI instance;

    public static DI getInstance() {
        if (instance == null) {
            instance = new DI();
        }
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            instance.clear();
        }
    }

    protected Map<Class<?>, Object> singletonInstances;

    private DI() {
        singletonInstances = new HashMap<>();
    }

    private void clear() {
        singletonInstances.clear();
    }

    /**
     * Return the instance of a singleton if it exists, null otherwise
     *
     * @param key
     * @param <T>
     * @return
     */
    @Nullable
    public synchronized <T> T getSingletonInstance(Class<T> key) {
        if (singletonInstances.containsKey(key)) {
            return (T) singletonInstances.get(key);
        }
        return null;
    }

    /**
     * Return the instance of a singleton if it exists, null otherwise
     *
     * @param key
     * @param <T>
     * @return
     */
    @Nullable
    public synchronized <T> void addSingletonInstance(Class<T> key, T instance) {
        singletonInstances.put(key, instance);
    }
}
