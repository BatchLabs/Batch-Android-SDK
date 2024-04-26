package com.batch.android.runtime;

/**
 * Action to change the state with knowledge of the current state
 *
 */
public interface ChangeStateAction {
    /**
     * Action to run
     *
     * @param state The current state
     * @param config The current configuration
     * @return the new state to set, if null will do nothing
     */
    State run(State state, Config config);
}
