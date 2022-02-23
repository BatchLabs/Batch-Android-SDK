package com.batch.android.core;

import java.util.ArrayDeque;

/**
 * A simple Promise-like implementation that is not thread-safe.
 * then() can't mutate the value.
 * <p>
 * Be careful: catch only works for an explicit rejection, NOT automatically for exceptions thrown in
 * ThenRunnables
 */
public class Promise<T> {

    private Status status = Status.PENDING;
    private T resolvedValue = null;
    private Exception rejectException = null;

    private ArrayDeque<ThenRunnable<T>> thenQueue = new ArrayDeque<>(1);
    private ArrayDeque<CatchRunnable> catchQueue = new ArrayDeque<>(1);

    public Promise() {}

    public Promise(ExecutorRunnable<T> executor) {
        T result;
        try {
            result = executor.run();
        } catch (Exception e) {
            reject(e);
            return;
        }
        // Isolate "resolve()" from the try catch, as we do not want internal exceptions to result in
        // a rejection
        resolve(result);
    }

    public Promise(DeferredResultExecutorRunnable<T> executor) {
        try {
            executor.run(this);
        } catch (Exception e) {
            reject(e);
        }
    }

    public static <T> Promise<T> resolved(T value) {
        final Promise<T> promise = new Promise<>();
        promise.resolve(value);
        return promise;
    }

    public static <T> Promise<T> rejected(Exception exception) {
        final Promise<T> promise = new Promise<>();
        promise.reject(exception);
        return promise;
    }

    public synchronized void resolve(T value) {
        if (status != Status.PENDING) {
            return;
        }

        status = Status.RESOLVED;
        resolvedValue = value;

        ThenRunnable<T> thenRunnable;
        while (!thenQueue.isEmpty()) {
            thenRunnable = thenQueue.removeLast();
            thenRunnable.run(value);
        }
    }

    public synchronized void reject(Exception exception) {
        if (status != Status.PENDING) {
            return;
        }

        status = Status.REJECTED;
        rejectException = exception;

        CatchRunnable catchRunnable;
        while (!catchQueue.isEmpty()) {
            catchRunnable = catchQueue.removeLast();
            catchRunnable.run(exception);
        }
    }

    public synchronized Promise<T> then(ThenRunnable<T> thenRunnable) {
        switch (status) {
            case PENDING:
                thenQueue.push(thenRunnable);
                break;
            case RESOLVED:
                thenRunnable.run(resolvedValue);
                break;
        }

        return this;
    }

    public synchronized Promise<T> catchException(CatchRunnable catchRunnable) {
        switch (status) {
            case PENDING:
                catchQueue.push(catchRunnable);
                break;
            case REJECTED:
                catchRunnable.run(rejectException);
                break;
        }

        return this;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Executor that automatically resolves the promise with the returned value once done, even
     * if null
     */
    public interface ExecutorRunnable<T> {
        T run();
    }

    /**
     * Executor that does not automatically resolve the promise once done
     */
    public interface DeferredResultExecutorRunnable<T> {
        void run(Promise<T> promise);
    }

    public interface ThenRunnable<T> {
        void run(T value);
    }

    public interface CatchRunnable {
        void run(Exception e);
    }

    public enum Status {
        PENDING,
        RESOLVED,
        REJECTED,
    }
}
