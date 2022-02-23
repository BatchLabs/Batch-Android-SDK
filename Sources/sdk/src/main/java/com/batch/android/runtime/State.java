package com.batch.android.runtime;

/**
 * State of the Batch lib
 *
 */
public enum State {
    /**
     * The lib is off, no action should be performed since all context and storage are null
     */
    OFF,

    /**
     * The lib is ready for action
     */
    READY,

    /**
     * The lib is finishing, no app listener or UI action must be called
     */
    FINISHING,
}
