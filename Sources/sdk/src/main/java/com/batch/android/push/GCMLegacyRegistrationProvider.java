package com.batch.android.push;

import android.content.Context;
import androidx.annotation.Nullable;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.module.PushModule;

public class GCMLegacyRegistrationProvider extends GCMAbstractRegistrationProvider implements PushRegistrationProvider {

    GCMLegacyRegistrationProvider(Context c, String senderID) {
        super(c, senderID);
    }

    @Override
    public String getShortname() {
        return "GCM-Legacy";
    }

    protected Integer getGMSAvailability() {
        return GooglePlayServicesHelper.isPushAvailable(context);
    }

    @Nullable
    @Override
    public String getRegistration() {
        Logger.error(
            PushModule.TAG,
            "Using Legacy GCM (pre-Instance ID) registration. This compatibility behaviour is deprecated and will be removed in a future release: Please update to a newer provider, such as FCM."
        );

        return GooglePlayServicesHelper.getPushToken(RuntimeManagerProvider.get().getContext(), senderID);
    }
}
