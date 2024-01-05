package com.batch.android.push;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.firebase.FirebaseApp;

public abstract class FCMAbstractRegistrationProvider implements PushRegistrationProvider {

    protected String senderID = null;
    protected String fcmProjectID = null;

    FCMAbstractRegistrationProvider(Context context) {
        loadProjectInformation(context);
    }

    public void loadProjectInformation(Context context) {
        try {
            FirebaseApp fbApp = FirebaseApp.getInstance();
            if (fbApp == null) {
                Logger.error(
                    PushModule.TAG,
                    "Could not register for FCM Push: Could not get a Firebase instance. Is your Firebase project configured?"
                );
                return;
            }

            String senderID = fbApp.getOptions().getGcmSenderId();
            if (TextUtils.isEmpty(senderID)) {
                Logger.error(
                    PushModule.TAG,
                    "Could not register for FCM Push: Could not get a Sender ID for this project. Are notifications well configured in the project's console and your google-services.json up to date?"
                );
                return;
            }

            this.senderID = senderID;
            this.fcmProjectID = fbApp.getOptions().getProjectId();

            Logger.internal(
                PushModule.TAG,
                "Using FCM Sender ID from builtin configuration: " + senderID + ", Project ID: " + fcmProjectID
            );
        } catch (NoClassDefFoundError | Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push: Firebase has thrown an exception", e);
        }
    }

    @Override
    public String getSenderID() {
        return senderID;
    }

    @Nullable
    @Override
    public String getGCPProjectID() {
        return fcmProjectID;
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
