package com.batch.android.runtime;

/**
 * Action with knowledge of the current state
 *
 */
public interface StateAction {
    /**
     * Action to run
     *
     * @param state current state
     */
    void run(State state);
}
