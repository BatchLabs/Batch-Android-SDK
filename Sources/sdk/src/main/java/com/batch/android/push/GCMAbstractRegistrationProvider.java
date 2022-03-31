package com.batch.android.push;

import android.content.Context;
import com.batch.android.AdsIdentifierProvider;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.adsidentifier.GCMAdsIdentifierProvider;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.module.PushModule;

public abstract class GCMAbstractRegistrationProvider implements PushRegistrationProvider {

    private static final String TAG = "GCMAbstractRegistrationProvider";

    private GCMAdsIdentifierProvider adsIdentifierProvider;

    protected Context context;
    protected String senderID;

    GCMAbstractRegistrationProvider(Context c, String senderID) {
        this.context = c.getApplicationContext();
        this.senderID = senderID;
        this.adsIdentifierProvider = new GCMAdsIdentifierProvider(this.context);
    }

    @Override
    public String getSenderID() {
        return senderID;
    }

    @Override
    public void checkServiceAvailability() throws PushRegistrationProviderAvailabilityException {
        // We do nothing here because GCM is checked in the factory
    }

    @Override
    public void checkLibraryAvailability() throws PushRegistrationProviderAvailabilityException {
        if (!PushModule.isBatchPushServiceAvailable()) {
            throw new PushRegistrationProviderAvailabilityException(
                "Unable to use GCM: BatchPushService is not declared in Manifest. Subclasses of it will not be taken into consideration: please add Batch's BatchPushService."
            );
        }

        Integer pushAvailability = getGMSAvailability();

        if (pushAvailability == null) {
            throw new PushRegistrationProviderAvailabilityException(
                "Unable to use GCM because the Google Play Services library is not integrated correctly or not up-to-date. Please include GooglePlayServices into your app (at least -base and -gcm modules)."
            );
        }

        if (pushAvailability != 0) {
            throw new PushRegistrationProviderAvailabilityException(
                "Unable to use GCM because the Google Play Services are not available or not up-to-date on this device. (" +
                GooglePlayServicesHelper.getGooglePlayServicesAvailabilityString(pushAvailability) +
                ") Please update your Google Play Services, more info: " +
                Parameters.DOMAIN_URL
            );
        }

        if (!isReceivePermissionAvailable()) {
            throw new PushRegistrationProviderAvailabilityException(
                "Permission com.google.android.c2dm.permission.RECEIVE is missing."
            );
        }

        if (!isC2DMessagePermissionAvailable()) {
            throw new PushRegistrationProviderAvailabilityException("Batch.Push : Permission C2D_MESSAGE is missing.");
        }

        if (!GenericHelper.isWakeLockPermissionAvailable(context)) {
            throw new PushRegistrationProviderAvailabilityException("Batch.Push : Permission WAKE_LOCK is missing.");
        }
    }

    @Override
    public AdsIdentifierProvider getAdsIdentifierProvider() {
        return adsIdentifierProvider;
    }

    protected abstract Integer getGMSAvailability();

    private boolean isReceivePermissionAvailable() {
        try {
            return GenericHelper.checkPermission("com.google.android.c2dm.permission.RECEIVE", context);
        } catch (Exception e) {
            Logger.error(TAG, "Error while checking com.google.android.c2dm.permission.RECEIVE permission", e);
            return false;
        }
    }

    private boolean isC2DMessagePermissionAvailable() {
        try {
            return GenericHelper.checkPermission(context.getPackageName() + ".permission.C2D_MESSAGE", context);
        } catch (Exception e) {
            Logger.error(TAG, "Error while checking C2D_MESSAGE permission", e);
            return false;
        }
    }
}
