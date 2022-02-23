package com.batch.android;

import com.batch.android.annotation.PublicSDK;
import com.batch.android.di.providers.PushModuleProvider;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * Batch's service for handling Google's Instance ID token refresh
 *
 */
@PublicSDK
public class BatchPushInstanceIDService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        Batch.onServiceCreate(this, false);
        PushModuleProvider.get().refreshRegistration();
        Batch.onServiceDestroy(this);
    }
}
