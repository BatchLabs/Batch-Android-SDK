package com.batch.android.localcampaigns.signal;

import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.trigger.NextSessionTrigger;

/**
 * Signal representing a new session start
 * Functionally equal to the {@link com.batch.android.runtime.SessionManager#INTENT_NEW_SESSION} local
 * broadcast
 */
public class NewSessionSignal implements Signal {

    @Override
    public boolean satisfiesTrigger(LocalCampaign.Trigger trigger) {
        return trigger instanceof NextSessionTrigger;
    }
}
