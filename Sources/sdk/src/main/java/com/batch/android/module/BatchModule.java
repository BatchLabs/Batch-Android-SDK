package com.batch.android.module;

import com.batch.android.runtime.State;

/**
 * Abstract class of a Batch Module
 *
 */
public abstract class BatchModule
{
    /**
     * ID of the module
     *
     * @return
     */
    public abstract String getId();

    /**
     * Should return the state of the module (usually 0 for deactivated, 1 for activated)
     *
     * @return
     */
    public abstract int getState();

// ----------------------------------->

    /**
     * Called by Batch before batch start<br>
     * NB : Context & activity are already available from the runtimeManager
     */
    public void batchWillStart()
    {
        // Override this method
    }

    /**
     * Called by Batch right after batch start<br>
     * NB : Same context and activity that in willStart but with the new state {@link State#READY} set
     */
    public void batchDidStart()
    {
        // Override this method
    }

    /**
     * Called by Batch before switching to {@link State#FINISHING}<br>
     * NB : Context and activity are still available from the runtimeManager
     */
    public void batchIsFinishing()
    {
        // Override this method
    }

    /**
     * Called by Batch before switching to {@link State#OFF}<br>
     * NB : Context is still available from runtimeManager (not activity)
     */
    public void batchWillStop()
    {
        // Override this method
    }

    /**
     * Called by Batch right after batch stop<br>
     * NB : No context or activity are available
     */
    public void batchDidStop()
    {
        // Override this method
    }
}
