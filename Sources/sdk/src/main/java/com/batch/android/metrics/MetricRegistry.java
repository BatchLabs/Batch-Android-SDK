package com.batch.android.metrics;

import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Observation;

/**
 * Simple class to centralize registered metrics
 */
public final class MetricRegistry {

    // Monitor local campaigns JIT response time
    public static final Observation localCampaignsJITResponseTime = new Observation(
        "sdk_local_campaigns_jit_ws_duration"
    )
        .register();

    // Monitor local campaign ws call by status ("OK", "KO")
    public static final Counter localCampaignsJITCount = new Counter("sdk_local_campaigns_jit_ws_count")
        .labelNames("status")
        .register();

    // Monitor local campaigns sync response time
    public static final Observation localCampaignsSyncResponseTime = new Observation(
        "sdk_local_campaigns_sync_ws_duration"
    )
        .register();
}
