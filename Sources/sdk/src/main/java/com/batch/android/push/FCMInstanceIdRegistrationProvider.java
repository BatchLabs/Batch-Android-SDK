package com.batch.android.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.batch.android.util.MetaDataUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class FCMInstanceIdRegistrationProvider extends FCMAbstractRegistrationProvider {

    FCMInstanceIdRegistrationProvider(Context context) {
        super(context);
    }

    @Override
    public String getShortname() {
        return "FCM";
    }

    @Override
    public String fetchSenderID(Context context) {
        int valueResource = MetaDataUtils.getIntMetaData(context, MetaDataUtils.MANIFEST_SENDER_ID_KEY);
        if (valueResource != -1) {
            String manifestSenderID = context.getString(valueResource);
            if (!TextUtils.isEmpty(manifestSenderID)) {
                Logger.info(PushModule.TAG, "Using FCM Sender ID from manifest");
                Logger.internal(PushModule.TAG, "Using FCM Sender ID from manifest: " + manifestSenderID);
                return manifestSenderID;
            }
        }
        return super.fetchSenderID(context);
    }

    @Nullable
    @Override
    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    public String getRegistration() {
        try {
            if (senderID == null) {
                return null;
            }
            FirebaseInstanceId fbIID = FirebaseInstanceId.getInstance();
            if (fbIID == null) {
                Logger.error(
                    PushModule.TAG,
                    "Could not register for FCM Push: Could not get the FirebaseInstanceId." +
                    " Is your Firebase project configured and initialized?"
                );
                return null;
            }
            return fbIID.getToken(senderID, FirebaseMessaging.INSTANCE_ID_SCOPE);
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push.", e);
        }
        return null;
    }
}
