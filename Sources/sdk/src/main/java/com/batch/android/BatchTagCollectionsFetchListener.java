package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import java.util.Map;
import java.util.Set;

/**
 * Listener used when fetching tag collections using {@link Batch.User#fetchTagCollections(Context, BatchTagCollectionsFetchListener)}.
 */
@PublicSDK
public interface BatchTagCollectionsFetchListener {
    /**
     * @param tagCollections A map of set of tag collections. The keys are the ones used when setting the tag collections.
     */
    void onSuccess(@NonNull Map<String, Set<String>> tagCollections);

    void onError();
}
