package com.batch.android;

import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;

/**
 * Defines a push notification registration provider
 */
@PublicSDK
public interface PushRegistrationProvider {
    /**
     * Returns the Sender ID of the provider, or equivalent. For example: "8122930293"
     */
    String getSenderID();

    /**
     * Returns the short name of the provider. For example: "FCM".
     */
    String getShortname();

    /**
     * Is this provider installed on the device?
     * This is the place to check if the service is installed on the device and if its version
     * match the feature we need.
     * <p>
     * This method will be called when electing the provider for the current start of the app.
     * <p>
     * If not available, you should throw a {@link PushRegistrationProviderAvailabilityException} with
     * the human-readable error message.
     */
    void checkServiceAvailability() throws PushRegistrationProviderAvailabilityException;

    /**
     * Is this provider implemented in the app?
     * This is the place to check if the libraries are here at runtime and
     * if the user enabled/disabled your provider.
     * <p>
     * This method will be called right before requesting a push token.
     * <p>
     * If not available, you should throw a {@link PushRegistrationProviderAvailabilityException} with
     * the human-readable error message.
     */
    void checkLibraryAvailability() throws PushRegistrationProviderAvailabilityException;

    /**
     * Returns the registration if possible.
     * Some providers might not have it right away. Return null in these cases.
     */
    @Nullable
    String getRegistration();

    /**
     * Return the ads identifier provider associated with the push registration provider
     *
     * @return
     */
    AdsIdentifierProvider getAdsIdentifierProvider();
}
