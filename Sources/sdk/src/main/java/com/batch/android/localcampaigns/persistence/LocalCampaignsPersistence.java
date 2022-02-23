package com.batch.android.localcampaigns.persistence;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONObject;

public interface LocalCampaignsPersistence {
    boolean hasSavedData(@NonNull Context context, @NonNull String dataKey) throws PersistenceException;

    /**
     * @param json the "campaigns" json object of the response
     */
    void persistData(@NonNull Context context, @NonNull JSONObject json, @NonNull String dataKey)
        throws PersistenceException;

    @Nullable
    JSONObject loadData(@NonNull Context context, @NonNull String dataKey) throws PersistenceException;

    void deleteData(@NonNull Context context, @NonNull String dataKey) throws PersistenceException;
}
