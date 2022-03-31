package com.batch.android.webservice.listener.impl;

import com.batch.android.FailReason;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.module.LocalCampaignsModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.webservice.listener.LocalCampaignsWebserviceListener;
import java.util.List;

/**
 * Listener for the local campaigns webservice. It will redirect the campaigns to the right modules depending on their type
 */
@Module
public class LocalCampaignsWebserviceListenerImpl implements LocalCampaignsWebserviceListener {

    private LocalCampaignsModule localCampaignsModule;

    private CampaignManager campaignManager;

    private LocalCampaignsWebserviceListenerImpl(
        LocalCampaignsModule localCampaignsModule,
        CampaignManager campaignManager
    ) {
        this.localCampaignsModule = localCampaignsModule;
        this.campaignManager = campaignManager;
    }

    @Provide
    public static LocalCampaignsWebserviceListenerImpl provide() {
        return new LocalCampaignsWebserviceListenerImpl(
            LocalCampaignsModuleProvider.get(),
            CampaignManagerProvider.get()
        );
    }

    @Override
    public void onSuccess(List<LocalCampaignsResponse> responses) {
        for (LocalCampaignsResponse response : responses) {
            handleInAppResponse(response);
        }
    }

    @Override
    public void onError(FailReason reason) {
        Logger.internal(LocalCampaignsModule.TAG, "Error while refreshing local campaigns: " + reason.toString());
        localCampaignsModule.onLocalCampaignsWebserviceFinished();
    }

    private void handleInAppResponse(LocalCampaignsResponse response) {
        campaignManager.setCappings(response.getCappings());
        campaignManager.updateCampaignList(response.getCampaigns());
        localCampaignsModule.onLocalCampaignsWebserviceFinished();
    }
}
