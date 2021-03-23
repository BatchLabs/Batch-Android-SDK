package com.batch.android;

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
public final class AdvertisingID
{
    private static final String TAG = "AdvertisingID";

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

    public AdvertisingID()
    {
        // Start Advertising ID async get
        initAdvertisingID();
    }

    /**
     * Start the thread to retrieve the advertising ID asynchronously
     */
    private void initAdvertisingID()
    {
        AdsIdentifierProvider provider = PushModuleProvider.get().getAdsIdentifierProvider();
        if (provider != null) {
            try {
                provider.checkAvailability();
            } catch (AdsIdentifierProviderAvailabilityException e) {
                Logger.error(TAG,
                        "Could not get Advertising Id: " + e.getMessage());
                return;
            }

            provider.getAdsIdentifier(new AdsIdentifierProvider.AdsIdentifierListener()
            {
                @Override
                public void onSuccess(String id, boolean limited)
                {
                    advertisingID = id;
                    AdvertisingID.this.limited = limited;
                    advertisingIdReady = true;
                    Logger.internal(TAG, "Advertising ID retrieved");
                }

                @Override
                public void onError(Exception e)
                {
                    Logger.error(TAG, "Error while retrieving Advertising ID", e);
                    advertisingIdReady = true;
                }
            });
        }
    }

// --------------------------------------------->

    /**
     * Tell if the process to retrieve advertising ID is already complete
     *
     * @return
     */
    public boolean isReady()
    {
        return advertisingIdReady;
    }

    /**
     * The advertising ID if available, null otherwise
     *
     * @return
     * @throws IllegalStateException if the advertising id is not available yet (check {@link #isReady()})
     */
    public String get() throws IllegalStateException
    {
        if (!advertisingIdReady) {
            throw new IllegalStateException("Advertising ID is not ready yet");
        }

        return advertisingID;
    }

    /**
     * Is the use of the advertising ID limited
     *
     * @return
     * @throws IllegalStateException if the advertising id is not available yet (check {@link #isReady()})
     */
    public boolean isLimited() throws IllegalStateException
    {
        if (!advertisingIdReady) {
            throw new IllegalStateException("Advertising ID is not ready yet");
        }

        return limited;
    }
}
