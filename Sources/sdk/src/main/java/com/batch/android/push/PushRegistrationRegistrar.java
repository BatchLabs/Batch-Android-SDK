package com.batch.android.push;

import android.content.Context;
import androidx.annotation.Keep;
import com.batch.android.PushRegistrationProvider;

/**
 * Class used to init registration provider
 */
@Keep
public interface PushRegistrationRegistrar {
    /**
     * Instantiate the push registration provider
     *
     * @param context Android's context
     * @return A push registration provider
     */
    PushRegistrationProvider getPushRegistrationProvider(Context context);
}
