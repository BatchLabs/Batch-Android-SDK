package com.batch.android.push;

import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.concurrent.TimeUnit;

public class FCMTokenRegistrationProvider extends FCMAbstractRegistrationProvider {

    FCMTokenRegistrationProvider() {
        super();
    }

    @Override
    public String getShortname() {
        return "FCM-Token";
    }

    @Nullable
    @Override
    public String getRegistration() {
        try {
            if (senderID == null) {
                return null;
            }
            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            Task<String> getTokenTask = firebaseMessaging.getToken();
            Tasks.await(getTokenTask, 30000L, TimeUnit.MILLISECONDS);
            if (!getTokenTask.isSuccessful()) {
                Logger.internal("Fetching FCM registration token failed", getTokenTask.getException());
                return null;
            }
            return getTokenTask.getResult();
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push.", e);
        }
        return null;
    }
}
