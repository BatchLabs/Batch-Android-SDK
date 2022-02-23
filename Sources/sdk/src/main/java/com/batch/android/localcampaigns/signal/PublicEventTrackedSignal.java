package com.batch.android.localcampaigns.signal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.module.LocalCampaignsModule;
import com.batch.android.module.UserModule;

/**
 * Represents the event tracked signal for public events
 */
public class PublicEventTrackedSignal extends EventTrackedSignal {

    @Nullable
    public String label;

    public PublicEventTrackedSignal(@NonNull EventTrackedSignal from) {
        super(from.name, from.parameters);
        if (from.parameters != null) {
            Object labelObj = from.parameters.opt(UserModule.PARAMETER_KEY_LABEL);

            if (labelObj != null) {
                if (labelObj instanceof String) {
                    label = (String) labelObj;
                } else {
                    Logger.internal(
                        LocalCampaignsModule.TAG,
                        "onEventTracked Found an event label, but was not a string. Value: " + labelObj.toString()
                    );
                }
            }
        }
    }

    public boolean satisfiesTrigger(LocalCampaign.Trigger trigger) {
        return (
            trigger instanceof EventLocalCampaignTrigger &&
            ((EventLocalCampaignTrigger) trigger).isSatisfied(name, label)
        );
    }

    public static boolean isPublic(@NonNull EventTrackedSignal signal) {
        return signal.name != null && signal.name.startsWith("E.");
    }
}
