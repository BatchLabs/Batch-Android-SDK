package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;

/**
 * References com.google.android.gms
 *
 * @hide
 */
public final class GoogleApiAvailabilityContainer {

    private static final String TAG = "GoogleApiAvailabilityContainer";

    private GoogleApiAvailabilityContainer() {}

    public static int getGooglePlayServicesVersionCode() {
        try {
            //TODO: This probably doesn't work the way we expect it: there is a good chance this resolves at compile time
            return com.google.android.gms.common.GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;
        } catch (Throwable e) {
            Logger.internal(TAG, "Could not get version code", e);
        }
        return -1;
    }

    public static boolean mustDeviceUpdatePlayServices(Context context) {
        try {
            com.google.android.gms.common.GoogleApiAvailability apiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
            return (
                resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS &&
                apiAvailability.isUserResolvableError(resultCode)
            );
        } catch (Throwable e) {
            Logger.internal(TAG, "Could not get version code", e);
        }
        return false;
    }

    public static boolean deviceNotSupportPlayServiceVersion(Context context) {
        try {
            com.google.android.gms.common.GoogleApiAvailability apiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
            return (
                resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS &&
                !apiAvailability.isUserResolvableError(resultCode)
            );
        } catch (Throwable e) {
            Logger.internal(TAG, "Could not get version code", e);
        }
        return true;
    }
}
