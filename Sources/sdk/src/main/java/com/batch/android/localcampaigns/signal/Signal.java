package com.batch.android.localcampaigns.signal;

import com.batch.android.localcampaigns.model.LocalCampaign;

/**
 * Represents an Application Event
 * <p>
 * An application event is anything that happens during the lifecycle of an app that can trigger an
 * In-App campaign
 */

public interface Signal {
    boolean satisfiesTrigger(LocalCampaign.Trigger trigger);
}
