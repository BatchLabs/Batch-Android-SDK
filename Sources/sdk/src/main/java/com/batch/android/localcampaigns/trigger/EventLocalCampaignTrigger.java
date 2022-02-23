package com.batch.android.localcampaigns.trigger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.localcampaigns.model.LocalCampaign;
import java.util.Locale;

/**
 * Event based trigger for local campaigns
 */

public class EventLocalCampaignTrigger implements LocalCampaign.Trigger {

    /**
     * Watched event name
     */
    @NonNull
    public String name;

    /**
     * Watched event label
     * Optional
     */
    @Nullable
    public String label;

    public EventLocalCampaignTrigger(@NonNull String name, @Nullable String label) {
        this.name = name.toUpperCase(Locale.US);
        this.label = label;
    }

    /**
     * Checks if this triggers is satisfied for a given event
     */
    public boolean isSatisfied(@Nullable String eventName, @Nullable String eventLabel) {
        if (!this.name.equalsIgnoreCase(eventName)) {
            return false;
        }

        if (this.label != null && !this.label.equalsIgnoreCase(eventLabel)) {
            return false;
        }

        return true;
    }

    @Override
    public String getType() {
        return "EVENT";
    }
}
