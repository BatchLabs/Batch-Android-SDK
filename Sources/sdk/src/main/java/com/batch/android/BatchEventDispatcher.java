package com.batch.android;

import androidx.annotation.NonNull;

import com.batch.android.annotation.PublicSDK;

/**
 * Interface used when listening for event to dispatch.
 * See {@link Batch.EventDispatcher#addDispatcher(BatchEventDispatcher)} and {@link Batch.EventDispatcher#removeDispatcher(BatchEventDispatcher)}.
 */
@PublicSDK
public interface BatchEventDispatcher
{
    /**
     * Callback when a new events just happened in the Batch SDK
     *
     * @param eventType
     * @param payload
     */
    void dispatchEvent(@NonNull Batch.EventDispatcher.Type eventType,
                       @NonNull Batch.EventDispatcher.Payload payload);
}


