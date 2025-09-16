package com.batch.android.localcampaigns.output;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.module.LocalCampaignsModule;
import com.batch.android.module.MessagingModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;

@Module
public class LandingOutputCEP extends LandingOutput {

    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    public LandingOutputCEP(MessagingModule messagingModule, @NonNull JSONObject payload) {
        super(messagingModule, payload);
    }

    @Provide
    public static LandingOutputCEP provide(@NonNull JSONObject payload) {
        return new LandingOutputCEP(MessagingModuleProvider.get(), payload);
    }

    @Override
    public boolean displayMessage(LocalCampaign campaign) {
        if (campaign.shouldBeDelayed()) {
            Logger.internal(
                LocalCampaignsModule.TAG,
                "Scheduling In-App message with delay: " + campaign.displayDelay + " seconds"
            );
            return handler.postDelayed(
                () -> {
                    if (!RuntimeManagerProvider.get().isApplicationInForeground()) {
                        Logger.internal(
                            LocalCampaignsModule.TAG,
                            "Application is in background, skipping message display"
                        );
                        return;
                    }
                    super.displayMessage(campaign);
                },
                campaign.displayDelay * 1000L
            );
        }
        return super.displayMessage(campaign);
    }
}
