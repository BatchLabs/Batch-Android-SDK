package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * @deprecated Batch doesn't collects the Android Advertising Identifier anymore.
 */
@PublicSDK
@Deprecated
public interface AdsIdentifierProvider {
    /**
     * Is this provider available to use? This is the place to check if the libraries are here at
     * runtime and if the user enabled/disabled your provider.
     * <p>
     * If not available, you should throw a {@link AdsIdentifierProviderAvailabilityException} with
     * the human-readable error message.
     */
    void checkAvailability() throws AdsIdentifierProviderAvailabilityException;

    /**
     * Retrieve the advertising id value and the opt-out state asynchronously
     *
     * @param listener
     */
    void getAdsIdentifier(AdsIdentifierListener listener);

    /**
     * Interface used to callback from
     * @deprecated Batch doesn't collects the Android Advertising Identifier anymore.
     */
    @PublicSDK
    @Deprecated
    interface AdsIdentifierListener {
        /**
         * Called on success with the id & if the user selected to opt-out from ads
         *
         * @param id
         * @param limited
         */
        void onSuccess(String id, boolean limited);

        /**
         * Called on error
         *
         * @param e
         */
        void onError(Exception e);
    }
}
