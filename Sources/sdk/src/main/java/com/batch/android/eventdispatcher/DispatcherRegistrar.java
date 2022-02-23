package com.batch.android.eventdispatcher;

import android.content.Context;
import androidx.annotation.Keep;
import com.batch.android.BatchEventDispatcher;

/**
 * Class used to init dispatcher from manifest meta-data
 */
@Keep
public interface DispatcherRegistrar {
    /**
     * Instantiate the dispatcher
     *
     * @param context
     * @return
     */
    BatchEventDispatcher getDispatcher(Context context);
}
