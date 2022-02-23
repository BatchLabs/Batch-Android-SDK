package com.batch.android;

import com.batch.android.annotation.PublicSDK;

@PublicSDK
public class PushRegistrationProviderAvailabilityException extends Exception {

    public PushRegistrationProviderAvailabilityException(String message) {
        super(message);
    }
}
