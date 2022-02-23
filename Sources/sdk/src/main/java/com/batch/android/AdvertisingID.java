package com.batch.android;

import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;

/**
 * Object that encapsulate advertising ID
 *
 * @hide
 */
@Module
@Singleton
public final class AdvertisingID {

    private static final String TAG = "AdvertisingID";

    /**
     * Advertising ID value return when not available :
     *
     * - For Apps with target API level set to 31 (Android 12) or later must declare the normal
     * permission com.google.android.gms.AD_ID in the AndroidManifest.xml in order to use
     * the getId API
     *
     * - For all users who have opted out of ads personalization in their device settings
     */
    private static final String UNAVAILABLE_AD_ID = "00000000-0000-0000-0000-000000000000";

    /**
     * Advertising ID
     */
    private String advertisingID;

    /**
     * Is use of advertising id limited
     */
    private boolean limited;

    /**
     * Is advertising ID already available
     */
    private boolean advertisingIdReady = false;

    // ------------------------------------------------->

    public AdvertisingID() {
        // Start Advertising ID async get
        initAdvertisingID();
    }

    /**
     * Start the thread to retrieve the advertising ID asynchronously
     */
    private void initAdvertisingID() {
        AdsIdentifierProvider provider = PushModuleProvider.get().getAdsIdentifierProvider();
        if (provider != null) {
            try {
                provider.checkAvailability();
            } catch (AdsIdentifierProviderAvailabilityException e) {
                Logger.error(TAG, "Could not get Advertising Id: " + e.getMessage());
                return;
            }

            provider.getAdsIdentifier(
                new AdsIdentifierProvider.AdsIdentifierListener() {
                    @Override
                    public void onSuccess(String id, boolean limited) {
                        advertisingID = UNAVAILABLE_AD_ID.equals(id) ? null : id;
                        AdvertisingID.this.limited = limited;
                        advertisingIdReady = true;
                        Logger.internal(TAG, "Advertising ID retrieved");
                    }

                    @Override
                    public void onError(Exception e) {
                        Logger.error(TAG, "Error while retrieving Advertising ID", e);
                        advertisingIdReady = true;
                    }
                }
            );
        }
    }

    // --------------------------------------------->

    /**
     * Tell if the process to retrieve advertising ID is already complete
     *
     * @return true if the process is completed
     */
    public boolean isReady() {
        return advertisingIdReady;
    }

    /**
     * Get the advertising ID
     *
     * @return The advertising ID if available, null otherwise
     * @throws IllegalStateException if the advertising id is not available yet (check {@link #isReady()})
     */
    @Nullable
    public String get() throws IllegalStateException {
        if (!advertisingIdReady) {
            throw new IllegalStateException("Advertising ID is not ready yet");
        }

        return advertisingID;
    }

    /**
     * Is the use of the advertising ID limited
     *
     * @return true if the advertising ID limited
     * @throws IllegalStateException if the advertising id is not available yet (check {@link #isReady()})
     */
    public boolean isLimited() throws IllegalStateException {
        if (!advertisingIdReady) {
            throw new IllegalStateException("Advertising ID is not ready yet");
        }

        return limited;
    }

    /**
     * Is the advertising ID not null
     *
     * @return true if its not
     */
    public boolean isNotNull() {
        return advertisingID != null;
    }
}
