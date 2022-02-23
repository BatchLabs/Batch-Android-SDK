package com.batch.android.push;

import android.content.Context;
import android.text.TextUtils;
import com.batch.android.AdsIdentifierProvider;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.adsidentifier.GCMAdsIdentifierProvider;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.firebase.FirebaseApp;

public abstract class FCMAbstractRegistrationProvider implements PushRegistrationProvider {

    protected GCMAdsIdentifierProvider adsIdentifierProvider;
    protected String senderID;

    FCMAbstractRegistrationProvider(Context context) {
        this.senderID = fetchSenderID(context);
        this.adsIdentifierProvider = new GCMAdsIdentifierProvider(context.getApplicationContext());
    }

    public String fetchSenderID(Context context) {
        try {
            FirebaseApp fbApp = FirebaseApp.getInstance();
            if (fbApp == null) {
                Logger.error(
                    PushModule.TAG,
                    "Could not register for FCM Push: Could not get a Firebase instance. Is your Firebase project configured?"
                );
                return null;
            }

            String senderID = fbApp.getOptions().getGcmSenderId();
            if (TextUtils.isEmpty(senderID)) {
                Logger.error(
                    PushModule.TAG,
                    "Could not register for FCM Push: Could not get a Sender ID for this project. Are notifications well configured in the project's console and your google-services.json up to date?"
                );
                return null;
            }

            Logger.internal(PushModule.TAG, "Using FCM Sender ID from builtin configuration: " + senderID);
            return senderID;
        } catch (NoClassDefFoundError | Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push: Firebase has thrown an exception", e);
        }
        return senderID;
    }

    @Override
    public String getSenderID() {
        return senderID;
    }

    @Override
    public void checkServiceAvailability() throws PushRegistrationProviderAvailabilityException {
        // We do nothing here because FCM is checked in the factory
    }

    @Override
    public void checkLibraryAvailability() throws PushRegistrationProviderAvailabilityException {
        Logger.internal(PushModule.TAG, "Checking FCM librairies availability");

        if (!isFirebaseCorePresent()) {
            throw new PushRegistrationProviderAvailabilityException(
                "Firebase Core is missing. Did you add 'com.google.firebase:firebase-core' to your gradle dependencies?"
            );
        }

        if (!isFirebaseMessagingPresent()) {
            throw new PushRegistrationProviderAvailabilityException(
                "Firebase Messaging is missing. Did you add 'com.google.firebase:firebase-messaging' to your gradle dependencies?"
            );
        }

        if (!PushModule.isBatchPushServiceAvailable()) {
            throw new PushRegistrationProviderAvailabilityException(
                "com.batch.android.BatchPushService is missing from the manifest."
            );
        }
    }

    @Override
    public AdsIdentifierProvider getAdsIdentifierProvider() {
        return adsIdentifierProvider;
    }

    private boolean isFirebaseCorePresent() {
        try {
            Class.forName("com.google.firebase.FirebaseApp");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    public static boolean isFirebaseMessagingPresent() {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
