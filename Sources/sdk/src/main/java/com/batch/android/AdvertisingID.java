package com.batch.android;

import android.content.Context;
import androidx.annotation.Nullable;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;

/**
 * Object that encapsulate advertising ID
 *
 * @deprecated Batch doesn't collects the Android Advertising Identifier anymore.
 * @hide
 */
@Module
@Singleton
@Deprecated
public final class AdvertisingID {

    public AdvertisingID() {}

    /**
     * Tell if the process to retrieve advertising ID is already complete
     *
     * @return This method always return false.
     * @deprecated Batch doesn't support advertising id anymore.
     */
    @Deprecated
    public boolean isReady() {
        return false;
    }

    /**
     * Get the advertising ID
     *
     * @return This method always return null.
     * @throws IllegalStateException Cannot throw
     * @deprecated Batch doesn't support advertising id anymore.
     */
    @Deprecated
    @Nullable
    public String get() throws IllegalStateException {
        return null;
    }

    /**
     * Is the use of the advertising ID limited
     *
     * @return This method always return false.
     * @throws IllegalStateException Cannot throw
     * @deprecated Batch doesn't support advertising id anymore.
     */
    @Deprecated
    public boolean isLimited() throws IllegalStateException {
        return false;
    }

    /**
     * Is the advertising ID not null
     *
     * @return This method always return false.
     * @deprecated Batch doesn't support advertising id anymore.
     */
    @Deprecated
    public boolean isNotNull() {
        return false;
    }
}
