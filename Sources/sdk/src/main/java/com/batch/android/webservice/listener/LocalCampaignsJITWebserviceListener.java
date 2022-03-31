package com.batch.android.webservice.listener;

import com.batch.android.core.Webservice;
import java.util.List;

/**
 * Listener for LocalCampaignsJITWebservice
 */

public interface LocalCampaignsJITWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess(List<String> eligibleCampaigns);

    /**
     * Called on error
     *
     * @param error webservice error
     */
    void onFailure(Webservice.WebserviceError error);
}
