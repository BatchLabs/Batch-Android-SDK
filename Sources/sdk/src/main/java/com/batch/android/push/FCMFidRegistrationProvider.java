package com.batch.android.push;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FCM registration based on the Firebase Installation ID (FID), introduced in firebase-messaging 25.1.0.
 * Versions before 25.1.2 fail to register restored installations whose FID is already used.
 * <p>
 * Only used when the app opted into FID registration through the
 * "firebase_messaging_installation_id_enabled" manifest flag.
 */
public class FCMFidRegistrationProvider extends FCMAbstractRegistrationProvider {

    private static final long TASK_TIMEOUT_MS = 30000L;

    /**
     * The FID FirebaseMessaging.register() last succeeded with in this process.
     */
    private final AtomicReference<String> registeredFid = new AtomicReference<>(null);

    FCMFidRegistrationProvider() {
        super();
    }

    @Override
    public String getShortname() {
        return "FCM-FID";
    }

    @Nullable
    @Override
    public String getRegistration() {
        try {
            if (senderID == null) {
                return null;
            }
            Task<String> fidTask = fetchInstallationId();
            Tasks.await(fidTask, TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!fidTask.isSuccessful()) {
                Logger.internal("Fetching Firebase Installation ID failed", fidTask.getException());
                return null;
            }
            String fid = fidTask.getResult();

            if (fid != null && !fid.equals(registeredFid.get())) {
                Task<Void> registerTask = registerWithFCM();
                Tasks.await(registerTask, TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!registerTask.isSuccessful()) {
                    Logger.internal(
                        "FCM registration with Firebase Installation ID failed",
                        registerTask.getException()
                    );
                    return null;
                }
                // register() may replace the FID (e.g. on FID_ALREADY_USED, or when the installation was
                // deleted server side), so read back the one FCM actually registered.
                Task<String> registeredFidTask = fetchInstallationId();
                Tasks.await(registeredFidTask, TASK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!registeredFidTask.isSuccessful()) {
                    Logger.internal(
                        "Fetching Firebase Installation ID after registration failed",
                        registeredFidTask.getException()
                    );
                    return null;
                }
                fid = registeredFidTask.getResult();
                registeredFid.set(fid);
            }
            return fid;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not register for FCM Push.", e);
        }
        return null;
    }

    @VisibleForTesting
    @NonNull
    Task<Void> registerWithFCM() {
        return FirebaseMessaging.getInstance().register();
    }

    @VisibleForTesting
    @NonNull
    Task<String> fetchInstallationId() {
        return FirebaseInstallations.getInstance().getId();
    }
}
