package com.batch.android.core;

import android.content.Context;
import android.content.Intent;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor for tasks that guarantee that only one instance with the same identifier will be run
 *
 */
@Module
@Singleton
public final class TaskExecutor extends ThreadPoolExecutor {

    /**
     * Intent action when work is over
     */
    public static final String INTENT_WORK_FINISHED = Parameters.LIBRARY_BUNDLE + ".executor.finished";

    // ----------------------------------------->

    /**
     * Map of currently in queue or executing Future
     */
    private final Map<Future<?>, String> futures = new HashMap<>();

    /**
     * App context
     */
    private Context context;

    // ------------------------------------------>

    /**
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @see {@link ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue)}
     */
    protected TaskExecutor(
        Context context,
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new NamedThreadFactory());
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        this.context = context.getApplicationContext();
    }

    @Provide
    public static TaskExecutor provide(Context context) {
        Parameters parameters = ParametersProvider.get(context);
        int minSize = Integer.parseInt(parameters.get(ParameterKeys.TASK_EXECUTOR_MIN_POOL, "0"));
        int maxSize = Integer.parseInt(parameters.get(ParameterKeys.TASK_EXECUTOR_MAX_POOL, "5"));
        int ttl = Integer.parseInt(parameters.get(ParameterKeys.TASK_EXECUTOR_THREADTTL, "1000"));

        return new TaskExecutor(context, minSize, maxSize, ttl, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * Submit a	new task to the executor.<br>
     * If a task with the same identifier is running, it will be cancel<br>
     * If a task with the same identifier is in queue, it will be removed.
     *
     * @param task
     * @return
     */
    public Future<?> submit(TaskRunnable task) {
        if (task == null) {
            throw new NullPointerException("Null task");
        }

        synchronized (futures) {
            /*
             * Remove scheduled tasks with the same identifier
             */
            Iterator<Runnable> runnables = getQueue().iterator();
            while (runnables.hasNext()) {
                Runnable r = runnables.next();
                if (r instanceof Future<?>) {
                    Future<?> future = (Future<?>) r;

                    String taskID = futures.get(future);
                    if (taskID != null && taskID.equals(task.getTaskIdentifier())) {
                        future.cancel(true);
                        runnables.remove();

                        futures.remove(future);
                    }
                }
            }

            /*
             * Remove tasks that are already running with the same identifier
             */
            Iterator<Future<?>> runnings = futures.keySet().iterator();
            while (runnings.hasNext()) {
                Future<?> running = runnings.next();
                String taskid = futures.get(running);

                if (taskid.equals(task.getTaskIdentifier())) {
                    running.cancel(true);
                    runnings.remove();
                }
            }

            /*
             * Submit task
             */
            Future<?> future = super.submit(task);
            futures.put(future, task.getTaskIdentifier());

            return future;
        }
    }

    /**
     * Is the task executor currently busy at running tasks
     *
     * @return true is working, false otherwise
     */
    public boolean isBusy() {
        synchronized (futures) {
            return !futures.isEmpty();
        }
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    // --------------------------------------------->

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            if (!(r instanceof FutureTask)) {
                return; // Not our job
            }

            /*
             * Try to retrieve this task and delete it from the map
             */
            synchronized (futures) {
                futures.remove(r);
                if (futures.isEmpty()) {
                    LocalBroadcastManagerProvider.get(context).sendBroadcast(new Intent(INTENT_WORK_FINISHED));
                }
            }
        } finally {
            super.afterExecute(r, t);
        }
    }
}
