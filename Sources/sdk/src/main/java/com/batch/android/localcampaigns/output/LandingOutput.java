package com.batch.android.localcampaigns.output;

import androidx.annotation.NonNull;
import com.batch.android.BatchInAppMessage;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.module.LocalCampaignsModule;
import com.batch.android.module.MessagingModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;

@Module
public class LandingOutput extends LocalCampaign.Output {

    private MessagingModule messagingModule;

    public LandingOutput(MessagingModule messagingModule, @NonNull JSONObject payload) {
        super(payload);
        this.messagingModule = messagingModule;
    }

    @Provide
    public static LandingOutput provide(@NonNull JSONObject payload) {
        return new LandingOutput(MessagingModuleProvider.get(), payload);
    }

    @Override
    public boolean displayMessage(LocalCampaign campaign) {
        try {
            // Copy event data before making the BatchInAppMessage
            JSONObject mergedPayload = new JSONObject(payload);
            mergedPayload.put("ed", campaign.eventData);
            JSONObject customPayload = new JSONObject(
                campaign.customPayload != null ? new JSONObject(campaign.customPayload) : new JSONObject()
            );

            BatchInAppMessage message = new BatchInAppMessage(
                campaign.publicToken,
                campaign.id,
                campaign.eventData,
                mergedPayload,
                customPayload
            );

            messagingModule.displayInAppMessage(message);
            return true;
        } catch (JSONException e) {
            Logger.internal(LocalCampaignsModule.TAG, "Landing Output: Could not copy custom payload", e);
        }
        return false;
    }
}
