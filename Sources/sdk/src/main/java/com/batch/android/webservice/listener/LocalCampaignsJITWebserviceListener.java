package com.batch.android.webservice.listener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Webservice;
import java.util.List;

/**
 * Listener for LocalCampaignsJITWebservice
 */
public interface LocalCampaignsJITWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess(@NonNull List<String> eligibleCampaigns);

    /**
     * Called on error
     *
     * @param error webservice error
     */
    void onFailure(@Nullable Webservice.WebserviceError error);
}
