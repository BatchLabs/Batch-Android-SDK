package com.batch.android.webservice.listener;

import com.batch.android.FailReason;
import com.batch.android.query.response.LocalCampaignsResponse;
import java.util.List;

/**
 * Listener for LocalCampaignsWebservice
 */

public interface LocalCampaignsWebserviceListener {
    /**
     * Called on success
     */
    void onSuccess(List<LocalCampaignsResponse> response);

    /**
     * Called on error
     *
     * @param reason
     */
    void onError(FailReason reason);
}
