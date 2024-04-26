package com.batch.android.runtime;

import androidx.annotation.NonNull;

/**
 * Action callback to update or read the current configuration object
 */
public interface ConfigAction {
    /**
     * Action to run
     *
     * @param config The configuration object to update
     */
    void run(@NonNull Config config);
}
