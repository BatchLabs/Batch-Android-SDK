package com.batch.android;

import com.batch.android.annotation.PublicSDK;

@PublicSDK
public class AdsIdentifierProviderAvailabilityException extends Exception {

    public AdsIdentifierProviderAvailabilityException(String message) {
        super(message);
    }
}
