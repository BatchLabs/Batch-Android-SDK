package com.batch.android.push;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.PushRegistrationProvider;
import com.batch.android.PushRegistrationProviderAvailabilityException;
import com.batch.android.core.DiscoveryServiceHelper;
import com.batch.android.core.GooglePlayServicesHelper;
import com.batch.android.core.Logger;
import com.batch.android.module.PushModule;
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
    private final Context context;

    public PushRegistrationProviderFactory(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Get the registration provider.
     * <p>
     * This method only support FCM Token provider or external push provider (like our hms plugin).
     * <p>
     * A provider's decision is final. If it then fails its availability check, another provider will NOT be picked.
     * Thus, looking at the manifest to take this decision isn't the Provider's responsibility, but this Factory's.
     * To avoid unnecessary work, providers assume that they have been instantiated through this factory,
     * and will not check the manifest for their receiver.
     * @return The registration provider.
     */
    @Nullable
    public PushRegistrationProvider getRegistrationProvider() {
        Logger.internal(TAG, "Determining which registration provider to use...");
        Integer playServiceAvailableError = GooglePlayServicesHelper.isFCMAvailable(context);
        if (playServiceAvailableError == null || playServiceAvailableError != 0) {
            // Google play services are missing or doesn't support FCM (too old)
            // Checking for an external push provider (eg: hms)
            PushRegistrationProvider provider = getExternalPushRegistrationProvider();
            if (provider != null) {
                Logger.info(PushModule.TAG, "Registration ID/Push Token: Using " + provider.getClass().getSimpleName());
                return provider;
            }
        }
        if (isFCMTokenApiAvailable()) {
            Logger.internal(TAG, "Using FCM-Token provider");
            return new FCMTokenRegistrationProvider();
        } else {
            Logger.warning(
                TAG,
                "Could not register for FCM Push: Ensure you are using firebase-messaging 22.0.0 or higher in your gradle dependencies."
            );
        }
        return null;
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
     * Try to get an external push provider
     * @return An external push provider or null otherwise.
     */
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

    /**
     * Check if the given push provider short name is allowed
     *
     * @param shortname Push provider shortname
     * @return Whether the push provider is allowed
     */
    public boolean isExternalProviderAllowed(@NonNull String shortname) {
        // For now, we only support "HMS"
        return "HMS".equals(shortname);
    }
}
