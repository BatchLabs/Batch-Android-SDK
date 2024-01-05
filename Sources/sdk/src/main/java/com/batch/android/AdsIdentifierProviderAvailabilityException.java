package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 *
 * @deprecated Batch doesn't collects the Android Advertising Identifier anymore.
 */
@PublicSDK
@Deprecated
public class AdsIdentifierProviderAvailabilityException extends Exception {

    public AdsIdentifierProviderAvailabilityException(String message) {
        super(message);
    }
}
