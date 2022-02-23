package com.batch.android.adsidentifier;

import android.content.Context;
import com.batch.android.AdsIdentifierProvider;
import com.batch.android.AdsIdentifierProviderAvailabilityException;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.TaskExecutorProvider;
import java.lang.reflect.Method;

public class GCMAdsIdentifierProvider implements AdsIdentifierProvider {

    private Context context;

    public GCMAdsIdentifierProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void checkAvailability() throws AdsIdentifierProviderAvailabilityException {
        if (!GooglePlayServicesHelper.isAdvertisingIDAvailable(context)) {
            throw new AdsIdentifierProviderAvailabilityException(
                "Google Play Services Advertising ID seems to be unavailable or too old."
            );
        }

        if (!isGMSAdvertisingIdClientPresent()) {
            throw new AdsIdentifierProviderAvailabilityException(
                "Google Play Services Ads Identifier is missing. Did you add 'com.google.android.gms:play-services-ads-identifier' to your gradle dependencies?"
            );
        }
    }

    private boolean isGMSAdvertisingIdClientPresent() {
        try {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public void getAdsIdentifier(AdsIdentifierListener listener) {
        if (listener == null) {
            throw new NullPointerException("Null listener");
        }

        /*
         * Must be done in another thread
         */
        TaskExecutorProvider
            .get(context)
            .submit(
                new TaskRunnable() {
                    @Override
                    public void run() {
                        try {
                            /*
                             * retrieve infos
                             */
                            Class<?> clazz = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                            Method method = clazz.getMethod("getAdvertisingIdInfo", Context.class);
                            Object response = method.invoke(null, context);

                            /*
                             * Extract methods to get id & limited value
                             */
                            Class<?> infoClazz = Class.forName(
                                "com.google.android.gms.ads.identifier.AdvertisingIdClient$Info"
                            );
                            Method getValueMethod = infoClazz.getMethod("getId");
                            Method getLimitedMethod = infoClazz.getMethod("isLimitAdTrackingEnabled");

                            /*
                             * Get values
                             */
                            String value = (String) getValueMethod.invoke(response, (Object[]) null);
                            boolean limited = (Boolean) getLimitedMethod.invoke(response, (Object[]) null);

                            /*
                             * Call the listener
                             */
                            listener.onSuccess(value, limited);
                        } catch (Exception e) {
                            listener.onError(e);
                            return;
                        }
                    }

                    @Override
                    public String getTaskIdentifier() {
                        return "advertisingid/get";
                    }
                }
            );
    }
}
