package com.batch.android.push;

import android.content.Context;
import androidx.annotation.Nullable;
import com.batch.android.BatchPushInstanceIDService;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.module.PushModule;

public class GCMIidRegistrationProvider extends GCMAbstractRegistrationProvider {

    GCMIidRegistrationProvider(Context c, String senderID) {
        super(c, senderID);
    }

    @Override
    public String getShortname() {
        return "GCM";
    }

    @Override
    protected Integer getGMSAvailability() {
        return GooglePlayServicesHelper.isInstanceIdPushAvailable(context);
    }

    @Override
    public void checkLibraryAvailability() throws PushRegistrationProviderAvailabilityException {
        try {
            @SuppressWarnings("unused")
            BatchPushInstanceIDService ignored = new BatchPushInstanceIDService();
            // The service is registered, the superclass is here, enable the use of the instance ID if the dev wants it
        } catch (Exception | NoClassDefFoundError e) {
            Logger.internal(PushModule.TAG, "Error while instantiating BatchPushInstanceIDService", e);
            throw new PushRegistrationProviderAvailabilityException(
                "BatchPushInstanceIDService is declared in the Manifest, but the Play Services appear to be too old! This can cause CRASHES in your app: remove it or update your Play Services to version 10.2.9 or higher. This error can also be caused by an incorrect proguard configuration. Falling back on classic GCM, please read the documentation for more info: " +
                Parameters.DOMAIN_URL
            );
        }

        super.checkLibraryAvailability();
    }

    @Nullable
    @Override
    public String getRegistration() {
        return GooglePlayServicesHelper.getInstancePushToken(context, senderID);
    }
}
