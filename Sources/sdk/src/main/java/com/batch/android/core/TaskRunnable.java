package com.batch.android.core;

/**
 * Interface that you must implement if you want to submit to the {@link TaskExecutor}
 *
 */
public interface TaskRunnable extends Runnable {
    /**
     * Return a string that identify this task
     *
     * @return
     */
    String getTaskIdentifier();
}
