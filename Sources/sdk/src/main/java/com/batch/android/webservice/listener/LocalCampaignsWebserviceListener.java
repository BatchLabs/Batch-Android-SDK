package com.batch.android.webservice.listener;

import com.batch.android.FailReason;
import com.batch.android.query.response.AbstractLocalCampaignsResponse;

import java.util.List;

/**
 * Listener for LocalCampaignsWebservice
 */

public interface LocalCampaignsWebserviceListener
{
    /**
     * Called on success
     */
    void onSuccess(List<AbstractLocalCampaignsResponse> response);

    /**
     * Called on error
     *
     * @param reason
     */
    void onError(FailReason reason);
}
