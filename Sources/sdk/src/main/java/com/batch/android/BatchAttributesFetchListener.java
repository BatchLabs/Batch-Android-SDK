package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import java.util.Map;

/**
 * Listener used when fetching attributes using {@link Batch.User#fetchAttributes(Context, BatchAttributesFetchListener)}.
 */
@PublicSDK
public interface BatchAttributesFetchListener {
    /**
     * @param attributes A map of attributes. The keys are the ones used when setting the attributes.
     *                   The values are of type {@link BatchUserAttribute}.
     */
    void onSuccess(@NonNull Map<String, BatchUserAttribute> attributes);

    void onError();
}
