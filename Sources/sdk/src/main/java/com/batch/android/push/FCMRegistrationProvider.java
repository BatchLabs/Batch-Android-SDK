package com.batch.android.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.batch.android.AdsIdentifierProvider;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.adsidentifier.GCMAdsIdentifierProvider;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMRegistrationProvider implements PushRegistrationProvider
{
    private static final String MANIFEST_SENDER_ID_KEY = "batch_push_fcm_sender_id_override";

    private GCMAdsIdentifierProvider adsIdentifierProvider;
    private String senderID;

    FCMRegistrationProvider(Context context)
    {
        this.senderID = fetchSenderID(context);
        this.adsIdentifierProvider = new GCMAdsIdentifierProvider(context.getApplicationContext());
    }

    public String fetchSenderID(Context context)
    {
        try {
            final Bundle metaData = context
                    .getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;

            int valueResource = metaData.getInt(MANIFEST_SENDER_ID_KEY, -1);
            if (valueResource != -1) {
                String manifestSenderID = context.getString(valueResource);
                if (!TextUtils.isEmpty(manifestSenderID)) {
                    Logger.info(PushModule.TAG, "Using FCM Sender ID from manifest");
                    Logger.internal(PushModule.TAG,
                            "Using FCM Sender ID from manifest: " + manifestSenderID);
                    return manifestSenderID;
                }
            }

        } catch (Exception ignored) {
        }

        try {
            FirebaseApp fbApp = FirebaseApp.getInstance();
            if (fbApp == null) {
                Logger.error(PushModule.TAG,
                        "Could not register for FCM Push: Could not get a Firebase instance. Is your Firebase project configured?");
                return null;
            }

            String senderID = fbApp.getOptions().getGcmSenderId();
            if (TextUtils.isEmpty(senderID)) {
                Logger.error(PushModule.TAG,
                        "Could not register for FCM Push: Could not get a Sender ID for this project. Are notifications well configured in the project's console and your google-services.json up to date?");
                return null;
            }

            Logger.internal(PushModule.TAG,
                    "Using FCM Sender ID from builtin configuration: " + senderID);

            return senderID;
        } catch (NoClassDefFoundError | Exception e) {
            Logger.error(PushModule.TAG,
                    "Could not register for FCM Push: Firebase has thrown an exception",
                    e);
        }


        return senderID;
    }

    @Override
    public String getSenderID()
    {
        return senderID;
    }

    @Override
    public String getShortname()
    {
        return "FCM";
    }

    @Override
    public void checkServiceAvailability() throws PushRegistrationProviderAvailabilityException
    {
        // We do nothing here because FCM is checked in the factory
    }

    @Override
    public void checkLibraryAvailability() throws PushRegistrationProviderAvailabilityException
    {
        Logger.internal(PushModule.TAG, "Checking FCM librairies availability");

        if (!isFirebaseCorePresent()) {
            throw new PushRegistrationProviderAvailabilityException(
                    "Firebase Core is missing. Did you add 'com.google.firebase:firebase-core' to your gradle dependencies?");
        }

        if (!isFirebaseMessagingPresent()) {
            throw new PushRegistrationProviderAvailabilityException(
                    "Firebase Messaging is missing. Did you add 'com.google.firebase:firebase-messaging' to your gradle dependencies?");
        }

        if (!PushModule.isBatchPushServiceAvailable()) {
            throw new PushRegistrationProviderAvailabilityException(
                    "com.batch.android.BatchPushService is missing from the manifest.");
        }
    }

    @Nullable
    @Override
    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    public String getRegistration()
    {
        try {
            if (senderID == null) {
                return null;
            }

            FirebaseInstanceId fbIID = FirebaseInstanceId.getInstance();
            if (fbIID == null) {
                Logger.error(PushModule.TAG,
                        "Could not register for FCM Push: Could not get the FirebaseInstanceId. Is your Firebase project configured and initialized?");
                return null;
            }

            return fbIID.getToken(senderID, FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push.", e);
        }
        return null;
    }

    @Override
    public AdsIdentifierProvider getAdsIdentifierProvider()
    {
        return adsIdentifierProvider;
    }

    private boolean isFirebaseCorePresent()
    {
        try {
            Class.forName("com.google.firebase.FirebaseApp");
            Class.forName("com.google.firebase.iid.FirebaseInstanceId");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private boolean isFirebaseMessagingPresent()
    {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
