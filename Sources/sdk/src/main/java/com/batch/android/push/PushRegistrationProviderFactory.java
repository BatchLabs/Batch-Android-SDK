package com.batch.android.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchPushInstanceIDService;
import com.batch.android.BatchPushReceiver;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.core.DiscoveryServiceHelper;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.module.PushModule;
import com.batch.android.util.MetaDataUtils;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.List;

/**
 * Factory, giving you the adequate push registration provider according to the app setup and
 * SDK settings
 */

public class PushRegistrationProviderFactory {

    private static final String TAG = "PushRegistrationProviderFactory";
    private static final String COMPONENT_SENTINEL_VALUE = "com.batch.android.push.PushRegistrationRegistrar";
    private static final String COMPONENT_KEY_PREFIX = "com.batch.android.push:";

    private Context context;
    private boolean shouldUseGoogleInstanceID;
    private String gcmSenderID;

    public PushRegistrationProviderFactory(
        @NonNull Context context,
        boolean shouldUseGoogleInstanceID,
        @Nullable String gcmSenderID
    ) {
        this.context = context.getApplicationContext();
        this.shouldUseGoogleInstanceID = shouldUseGoogleInstanceID;
        this.gcmSenderID = gcmSenderID;
    }

    @Nullable
    public PushRegistrationProvider getRegistrationProvider() {
        /*
         * Due to legacy SDK implementations (all the way back to 1.3), we have to maintain a very
         * specific strategy to figure out what provider we pick.
         * Here is how it is decided: (User = The app developer using Batch SDK)
         *  - Look if BatchPushReceiver is in the manifest, meaning that Batch has previously been implemented in this app
         *   - Yes: Check if Batch.Push.setGCMSenderId has been called (gcmSenderID != null in this method)
         *     - Yes: Check if BatchPushInstanceIDService is in the manifest, is instanciable, InstanceID allowed in the configuration and runtime libs are here
         *       - Yes: Use GCM Instance ID
         *       - No: Fallback on Legacy GCM, and warn that this is deprecated
         *     - No: Fail
         *   - No: Fail
         *
         *  else
         *  - Look if Firebase is available at both compile time and runtime. FCMAbstractRegistrationProvider will enforce firebase-core & firebase-messaing's presences itself.
         *      - Yes: Check if FCM Token is available( firebase-messaging >= 20.3 ):
         *          - Yes: Check if sender id is overridden or FCM Instance ID is forced from the manifest:
         *              - Yes: Check if FCM Instance ID is available:
         *                  Yes: Use FCM Instance ID provider
         *                  No: Fail
         *              - No:  Use FCM Token
         *          - No: Check if FCM Instance ID is available:
         *                  Yes: Use FCM Instance ID provider
         *                  No: Fail
         *      - No: query for plugins.
         *
         * A provider's decision is final. If it then fails its availability check, another provider will NOT be picked.
         * Thus, looking at the manifest to take this decision isn't the Provider's responsibility, but this Factory's.
         * To avoid unnecessary work, providers assume that they have been instantiated through this factory,
         * and will not check the manifest for their receiver.
         *
         * Note that the advertising identifier will be provided by the same provider than push,
         * for consistency.
         */

        Logger.internal(TAG, "Determining which registration provider to use...");
        if (isLegacyPushReceiverInManifest()) {
            // No need to check for BatchPushService, as this will be done by GCMAbstractRegistrationProvider
            if (gcmSenderID != null) {
                boolean canUseGCMInstanceID = true;

                if (shouldUseGoogleInstanceID) {
                    if (isGCMInstanceIdAvailable()) {
                        Logger.internal(TAG, "GCM Instance ID class available");
                        if (!isBatchGCMIidServiceAvailable()) {
                            Logger.internal(
                                TAG,
                                "Batch's BatchPushInstanceIdService not registered in manifest, falling back"
                            );
                            canUseGCMInstanceID = false;
                        } else {
                            Logger.internal(
                                TAG,
                                "Batch's BatchPushInstanceIdService is registered in manifest, using GCM Instance ID"
                            );
                        }
                    } else {
                        Logger.internal(TAG, "GCM Instance ID class unavailable, falling back");
                        canUseGCMInstanceID = false;
                    }
                } else {
                    Logger.internal(PushModule.TAG, "GCM Instance ID disabled by configuration");
                    canUseGCMInstanceID = false;
                }

                if (canUseGCMInstanceID) {
                    Logger.internal(TAG, "Using GCM Instance ID provider");
                    Logger.warning(
                        PushModule.TAG,
                        "Registering for push notifications using GCM Instance ID is deprecated: please consider migrating to FCM. More info in our documentation."
                    );
                    return new GCMIidRegistrationProvider(context, gcmSenderID);
                } else {
                    Logger.internal(TAG, "Using GCM legacy provider");
                    Logger.warning(
                        PushModule.TAG,
                        "Registering for push notifications using GCM's legacy API is deprecated: please migrate to FCM. More info in our documentation."
                    );
                    return new GCMLegacyRegistrationProvider(context, gcmSenderID);
                }
            } else {
                Logger.internal(TAG, "No GCM Sender ID set: Push is disabled");
                Logger.warning(
                    PushModule.TAG,
                    "BatchPushReceiver is present in your manifest, but no Sender ID has been set: skipping push registration. Please migrate to FCM to fix this error."
                );
                return null;
            }
        }

        if (gcmSenderID != null) {
            Logger.internal(
                TAG,
                "Manifest doesn't have BatchPushReceiver but Batch.Push.setGCMSenderId has been called. Skipping Firebase."
            );
            Logger.error(
                PushModule.TAG,
                "BatchPushReceiver is not declared in Manifest, but Batch.Push.setGCMSenderId() has been called. Push registration will be disabled. In order to enable the use of FCM, please remove the Batch.Push.setGCMSenderId() call."
            );
            return null;
        }

        Integer playServiceAvailableError = GooglePlayServicesHelper.isFCMAvailable(context);
        if (playServiceAvailableError == null || playServiceAvailableError != 0) {
            // Google play services are missing or doesn't support FCM (too old)

            PushRegistrationProvider provider = getExternalPushRegistrationProvider();
            if (provider != null) {
                Logger.info(PushModule.TAG, "Registration ID/Push Token: Using " + provider.getClass().getSimpleName());
                return provider;
            }
        }

        boolean shouldUseFCMInstanceId = false;

        // Case: firebase-messaging >= 20.3
        if (isFCMTokenApiAvailable()) {
            if (isSenderIdOverridden()) {
                Logger.warning(
                    "Overriding sender id is deprecated with the FCM's Token APIs, please migrate away from it. See our help center for more info."
                );
                shouldUseFCMInstanceId = true;
            } else if (shouldForceFirebaseIIDProvider()) {
                shouldUseFCMInstanceId = true;
                Logger.internal(TAG, "FCM InstanceId provider is forced from the manifest.");
            }
        } else {
            shouldUseFCMInstanceId = true;
        }

        if (shouldUseFCMInstanceId) {
            if (isFCMFirebaseInstanceIdAvailable()) {
                Logger.internal(TAG, "Using FCM InstanceId provider");
                return new FCMInstanceIdRegistrationProvider(context);
            } else if (FCMAbstractRegistrationProvider.isFirebaseMessagingPresent()) {
                // This log is only if we have a recent FCM (firebase-messaging >= 22) without IID support.
                // Do not log anything if we don't have FCM at all, the provider will take care
                // of it when attempting to register.
                Logger.error(
                    PushModule.TAG,
                    "Trying to use FCM InstanceID but it looks like the library is not present. Please migrate to FCM's Token APIs or add the firebase-iid dependency."
                );
                return null;
            }
        }

        Logger.internal(TAG, "Using FCM-Token provider");
        return new FCMTokenRegistrationProvider(context);
    }

    private boolean isLegacyPushReceiverInManifest() {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final Intent intent = new Intent(context, BatchPushReceiver.class);

            int matchFlag = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                matchFlag = PackageManager.MATCH_DISABLED_COMPONENTS;
            } else {
                matchFlag = PackageManager.GET_DISABLED_COMPONENTS;
            }

            @SuppressLint("QueryPermissionsNeeded")
            List<ResolveInfo> resolveInfo = packageManager.queryBroadcastReceivers(intent, matchFlag);

            return resolveInfo.size() > 0;
        } catch (Exception e) {
            Logger.error(PushModule.TAG, "Could not check if legacy push receiver is in the manifest", e);
            return false;
        }
    }

    /**
     * Verify if the FCM Token API is available by checking if getToken method exist at runtime
     * (firebase-messaging >= 20.3)
     *
     * @return true if if the FCM Token APIs are available
     */
    private boolean isFCMTokenApiAvailable() {
        if (!FCMAbstractRegistrationProvider.isFirebaseMessagingPresent()) {
            return false;
        }
        try {
            FirebaseMessaging.class.getMethod("getToken");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Verify if FCM Firebase InstanceId is available
     *
     * @return true if class exist
     */
    private boolean isFCMFirebaseInstanceIdAvailable() {
        try {
            Class.forName("com.google.firebase.iid.FirebaseInstanceId");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Verify if GCM InstanceId is available
     *
     * @return true if class exist
     */
    private boolean isGCMInstanceIdAvailable() {
        try {
            Class.forName("com.google.android.gms.iid.InstanceID");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private boolean isBatchGCMIidServiceAvailable() {
        try {
            final PackageManager packageManager = RuntimeManagerProvider.get().getContext().getPackageManager();
            final Intent intent = new Intent(
                RuntimeManagerProvider.get().getContext(),
                BatchPushInstanceIDService.class
            );
            @SuppressLint("QueryPermissionsNeeded")
            List<ResolveInfo> resolveInfo = packageManager.queryIntentServices(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            );

            return resolveInfo.size() > 0;
        } catch (Exception | NoClassDefFoundError e) {
            Logger.error(
                PushModule.TAG,
                "Could not check if Batch's GCM Instance ID token refresh service is in the manifest",
                e
            );
            return false;
        }
    }

    /**
     * Check if we should force using the FCM Instance ID provider from the android manifest
     *
     * @return true if we should force the provider
     */
    private boolean shouldForceFirebaseIIDProvider() {
        return MetaDataUtils.getBooleanMetaData(context, MetaDataUtils.MANIFEST_FORCE_FCM_IID_KEY);
    }

    /**
     * Check if the FCM sender identifier is define from the android manifest
     *
     * @return true if the SenderId is overridden
     */
    private boolean isSenderIdOverridden() {
        return MetaDataUtils.getIntMetaData(context, MetaDataUtils.MANIFEST_SENDER_ID_KEY) != -1;
    }

    public PushRegistrationProvider getExternalPushRegistrationProvider() {
        List<String> registrarNames = DiscoveryServiceHelper.getComponentNames(
            context,
            PushRegistrationDiscoveryService.class,
            COMPONENT_SENTINEL_VALUE,
            COMPONENT_KEY_PREFIX
        );
        for (String name : registrarNames) {
            try {
                Class<?> loadedClass = Class.forName(name);
                if (!PushRegistrationRegistrar.class.isAssignableFrom(loadedClass)) {
                    Logger.error(
                        TAG,
                        String.format("Class %s is not an instance of %s", name, COMPONENT_SENTINEL_VALUE)
                    );
                    continue;
                }

                PushRegistrationRegistrar registrar = (PushRegistrationRegistrar) loadedClass
                    .getDeclaredConstructor()
                    .newInstance();
                PushRegistrationProvider registrationProvider = registrar.getPushRegistrationProvider(context);
                if (registrationProvider != null) {
                    try {
                        final String shortname = registrationProvider.getShortname();
                        if (!isExternalProviderAllowed(shortname)) {
                            Logger.internal(
                                TAG,
                                "Found '" +
                                shortname +
                                "' (" +
                                registrationProvider.getClass().getSimpleName() +
                                ") which is not allowed, skipping..."
                            );
                            continue;
                        }
                        registrationProvider.checkServiceAvailability();
                        return registrationProvider;
                    } catch (PushRegistrationProviderAvailabilityException e) {
                        Logger.internal(
                            TAG,
                            "Tried to use " +
                            registrationProvider.getClass().getSimpleName() +
                            " but not available, skipping..."
                        );
                    }
                } else {
                    Logger.internal(
                        TAG,
                        "Registrar " +
                        registrar.getClass().getSimpleName() +
                        " did not return a PushRegistrationProvider, skipping..."
                    );
                }
            } catch (Throwable e) {
                Logger.error(String.format("Could not instantiate %s", name), e);
            }
        }
        return null;
    }

    public boolean isExternalProviderAllowed(@NonNull String shortname) {
        // For now, we only support "HMS"
        return "HMS".equals(shortname);
    }
}
