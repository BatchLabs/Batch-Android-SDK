package com.batch.android.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.user.InstallDataEditor;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import com.batch.android.user.UserDataDiff;
import com.batch.android.user.UserDatabaseException;
import com.batch.android.user.UserOperation;
import com.batch.android.user.UserOperationQueue;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Batch User module
 *
 */
@Module
@Singleton
public final class UserModule extends BatchModule {

    public static final String TAG = "User";
    public static final String PARAMETER_KEY_LABEL = "label";
    public static final String PARAMETER_KEY_ATTRIBUTES = "attributes";
    private static final Pattern EVENT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,30}$");
    private static final long LOCATION_UPDATE_MINIMUM_TIME_MS = 30000;
    private static final long CIPHER_FALLBACK_RESET_TIME_MS = 172800000L;

    private static ScheduledExecutorService applyQueue = makeApplyQueue();

    private static ScheduledExecutorService makeApplyQueue() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory());
    }

    private final List<UserOperationQueue> operationQueues = new LinkedList<>();
    private BroadcastReceiver localBroadcastReceiver;
    private final AtomicBoolean checkScheduled = new AtomicBoolean(false);
    private long lastLocationTrackTimestamp = 0;
    private final TrackerModule trackerModule;

    private UserModule(TrackerModule trackerModule) {
        this.trackerModule = trackerModule;
    }

    @Provide
    public static UserModule provide() {
        return new UserModule(TrackerModuleProvider.get());
    }

    // region BatchModule

    @Override
    public String getId() {
        return "user";
    }

    @Override
    public int getState() {
        return 1;
    }

    @Override
    public void batchDidStart() {
        super.batchDidStart();

        startCheckWS(1000);

        final Context context = RuntimeManagerProvider.get().getContext();
        if (context != null) {
            // We check the cipher v1 fallback
            // and remove it if necessary
            String lastFailureStr = ParametersProvider.get(context).get(ParameterKeys.WS_CIPHERV2_LAST_FAILURE_KEY);
            if (lastFailureStr != null) {
                try {
                    long lastFailure = Long.parseLong(lastFailureStr);
                    if (lastFailure <= System.currentTimeMillis() - CIPHER_FALLBACK_RESET_TIME_MS) { // 2 days
                        ParametersProvider.get(context).remove(ParameterKeys.WS_CIPHERV2_LAST_FAILURE_KEY);
                    }
                } catch (NumberFormatException e) {
                    ParametersProvider.get(context).remove(ParameterKeys.WS_CIPHERV2_LAST_FAILURE_KEY);
                }
            }

            if (localBroadcastReceiver == null) {
                localBroadcastReceiver =
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (OptOutModule.INTENT_OPTED_IN.equalsIgnoreCase(intent.getAction())) {
                                userOptedIn();
                            }
                        }
                    };
                LocalBroadcastManagerProvider
                    .get(context)
                    .registerReceiver(localBroadcastReceiver, new IntentFilter(OptOutModule.INTENT_OPTED_IN));
            }
        }
        submitOperationQueues(0);
    }

    // endregion

    // region Getter & Setter
    /**
     * Set the new language
     *
     * @param context Android's context
     * @param language The User's language, null to remove
     */
    public void setLanguage(@NonNull Context context, @Nullable String language) {
        if (language != null) {
            ParametersProvider.get(context).set(ParameterKeys.USER_PROFILE_LANGUAGE_KEY, language, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.USER_PROFILE_LANGUAGE_KEY);
        }
    }

    /**
     * Get the User's language
     *
     * @param context Android's context
     * @return the User's language if any, null otherwise
     */
    @Nullable
    public String getLanguage(@NonNull Context context) {
        return ParametersProvider.get(context).get(ParameterKeys.USER_PROFILE_LANGUAGE_KEY);
    }

    /**
     * Set the User's region
     *
     * @param context Android's context
     * @param region the User's region, null to remove
     */
    public void setRegion(@NonNull Context context, @Nullable String region) {
        if (region != null) {
            ParametersProvider.get(context).set(ParameterKeys.USER_PROFILE_REGION_KEY, region, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.USER_PROFILE_REGION_KEY);
        }
    }

    /**
     * Get the User's region
     * @param context Android's context
     * @return the User's region if any, null otherwise
     */
    @Nullable
    public String getRegion(@NonNull Context context) {
        return ParametersProvider.get(context).get(ParameterKeys.USER_PROFILE_REGION_KEY);
    }

    /**
     * Set the User's custom identifier
     * @param context Android's context
     * @param customID the User's custom identifier , null to remove
     */
    public void setCustomID(@NonNull Context context, @Nullable String customID) {
        if (customID != null) {
            ParametersProvider.get(context).set(ParameterKeys.CUSTOM_ID, customID, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.CUSTOM_ID);
        }
    }

    /**
     * Get the custom ID if any, null otherwise
     * @param context Android's context
     * @return the custom ID if any, null otherwise
     */
    public String getCustomID(@NonNull Context context) {
        return ParametersProvider.get(context).get(ParameterKeys.CUSTOM_ID);
    }

    /**
     * Get the data version
     * @param context Android's context
     * @return the data version
     */
    public long getVersion(@NonNull Context context) {
        String version = ParametersProvider.get(context).get(ParameterKeys.USER_DATA_VERSION);
        if (version == null) {
            return 1;
        }

        try {
            return Long.parseLong(version);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Get the data version and increment it
     *
     * @param context Android's context
     */
    public synchronized void incrementVersion(@NonNull Context context) {
        long newVersion = getVersion(context) + 1;
        ParametersProvider.get(context).set(ParameterKeys.USER_DATA_VERSION, Long.toString(newVersion), true);
    }

    /**
     * Clear all installation data (attributes + tags)
     */
    public void clearInstallationData() {
        addOperationQueueAndSubmit(
            0,
            new UserOperationQueue(
                Collections.singletonList(datasource -> {
                    datasource.clearAttributes();
                    datasource.clearTags();
                })
            )
        );
    }

    //endregion

    // region Webservices

    public void startSendWS(long msDelay) {
        submitOnApplyQueue(
            msDelay,
            () -> {
                Context context = RuntimeManagerProvider.get().getContext();
                if (context == null) {
                    Logger.internal(TAG, "Internal error while sending attributes send WS: null context");
                    return;
                }

                Parameters parameters;
                long changeset;

                try {
                    parameters = ParametersProvider.get(context);
                } catch (Exception e) {
                    Logger.internal(TAG, "Internal error while sending attributes send WS", e);
                    return;
                }

                try {
                    changeset = Long.parseLong(parameters.get(ParameterKeys.USER_DATA_CHANGESET, "0"));
                } catch (NumberFormatException e) {
                    changeset = 0L;
                }

                if (changeset <= 0L) {
                    changeset = 1L;
                    parameters.set(ParameterKeys.USER_DATA_CHANGESET, Long.toString(changeset), true);
                }

                final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);

                WebserviceLauncher.launchAttributesSendWebservice(
                    RuntimeManagerProvider.get(),
                    changeset,
                    UserAttribute.getServerMapRepresentation(datasource.getAttributes(), true),
                    datasource.getTagCollections()
                );
            }
        );
    }

    public void startCheckWS(long msDelay) {
        checkScheduled.set(true);

        submitOnApplyQueue(
            msDelay,
            () -> {
                // Stop if a check wasn't scheduled (meaning it was already done by another scheduled check)
                // No need to spam with checks!
                if (!checkScheduled.compareAndSet(true, false)) {
                    return;
                }

                Context context = RuntimeManagerProvider.get().getContext();
                if (context == null) {
                    Logger.internal(TAG, "Internal error while sending attributes check WS: null context");
                    return;
                }

                Parameters parameters;
                long changeset;

                try {
                    parameters = ParametersProvider.get(context);
                } catch (Exception e) {
                    Logger.internal(TAG, "Internal error while sending attributes check WS", e);
                    return;
                }

                try {
                    changeset = Long.parseLong(parameters.get(ParameterKeys.USER_DATA_CHANGESET, "0"));
                } catch (NumberFormatException e) {
                    changeset = 0L;
                }

                String transactionID = parameters.get(ParameterKeys.USER_DATA_TRANSACTION_ID, "");

                if (TextUtils.isEmpty(transactionID)) {
                    // If we don't have a transaction ID but have a changeset, start another send
                    if (changeset > 0) {
                        startSendWS(0);
                    }

                    return;
                }

                if (changeset <= 0) {
                    changeset = 1L;
                    parameters.set(ParameterKeys.USER_DATA_CHANGESET, Long.toString(changeset), true);
                }

                WebserviceLauncher.launchAttributesCheckWebservice(
                    RuntimeManagerProvider.get(),
                    changeset,
                    transactionID
                );
            }
        );
    }

    public void storeTransactionID(final String transactionID, final long version) {
        if (version <= 0L || TextUtils.isEmpty(transactionID)) {
            return;
        }

        submitOnApplyQueue(
            0,
            () -> {
                long changeset;
                Parameters parameters;

                try {
                    parameters = ParametersProvider.get(RuntimeManagerProvider.get().getContext());
                } catch (Exception e) {
                    // We all love Android's context
                    Logger.internal(TAG, "Internal error while storing transaction ID", e);
                    return;
                }

                try {
                    changeset = Long.parseLong(parameters.get(ParameterKeys.USER_DATA_CHANGESET, "0"));
                } catch (NumberFormatException e) {
                    changeset = 0L;
                }

                if (changeset == version) {
                    parameters.set(ParameterKeys.USER_DATA_TRANSACTION_ID, transactionID, true);

                    // Recheck in 15s
                    startCheckWS(15000);
                }
            }
        );
    }

    public void bumpVersion(final long serverVersion) {
        submitOnApplyQueue(
            0,
            () -> {
                long changeset;
                Parameters parameters;

                try {
                    parameters = ParametersProvider.get(RuntimeManagerProvider.get().getContext());
                } catch (Exception e) {
                    // We all love Android's context
                    Logger.internal(TAG, "Internal error while bumping user data version", e);
                    return;
                }

                try {
                    changeset = Long.parseLong(parameters.get(ParameterKeys.USER_DATA_CHANGESET, "0"));
                } catch (NumberFormatException e) {
                    changeset = 0L;
                }

                long targetVersion = serverVersion + 1;

                // If we are already higher than the server, things will correct themselves
                // so don't touch anything!
                if (changeset < targetVersion) {
                    parameters.set(ParameterKeys.USER_DATA_CHANGESET, Long.toString(targetVersion), true);
                    parameters.remove(ParameterKeys.USER_DATA_TRANSACTION_ID);

                    startSendWS(0);
                }
            }
        );
    }

    // endregion

    public void trackLocation(Location location) {
        if (location == null) {
            return;
        }

        boolean shouldTrackLocation = false;

        if (lastLocationTrackTimestamp <= 0) {
            Logger.internal(TAG, "Tracking location because no location has been tracked yet");
            shouldTrackLocation = true;
        } else if ((SystemClock.elapsedRealtime() - lastLocationTrackTimestamp) >= LOCATION_UPDATE_MINIMUM_TIME_MS) {
            Logger.internal(
                TAG,
                "Tracking location event since the elapsed time is greater than the minimum threshold"
            );
            shouldTrackLocation = true;
        }

        if (!shouldTrackLocation) {
            Logger.internal(TAG, "Not tracking location event");
            return;
        }

        double accuracy = Math.max(0.0, location.getAccuracy());

        try {
            final JSONObject params = new JSONObject();
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
            params.put("acc", accuracy);

            long time = location.getTime();
            if (time > 0) {
                params.put("date", (double) time);
            }

            trackerModule.trackCollapsible(InternalEvents.LOCATION_CHANGED, new Date().getTime(), params);
            lastLocationTrackTimestamp = SystemClock.elapsedRealtime();
        } catch (JSONException e) {
            Logger.error(TAG, "Failed to track location", e);
        }
    }

    //endregion

    // region User operations
    public static void submitOnApplyQueue(long msDelay, Runnable r) {
        if (!applyQueue.isShutdown()) {
            applyQueue.schedule(r, msDelay, TimeUnit.MILLISECONDS);
        } else {
            Logger.error(
                InstallDataEditor.TAG,
                "Could not perform User Data operation. Is this installation currently opted out from Batch?"
            );
        }
    }

    public void addOperationQueueAndSubmit(long msDelay, UserOperationQueue queue) {
        synchronized (operationQueues) {
            operationQueues.add(queue);
        }
        if (!RuntimeManagerProvider.get().isReady()) {
            Logger.internal(InstallDataEditor.TAG, "Batch is not started, enqueuing user operations");
            return;
        }
        // Delay is used to prevent bad use of BatchUserEditor.save() method and trying to batch as much as possible user data transactions.
        submitOperationQueues(msDelay);
    }

    public void submitOperationQueues(long msDelay) {
        synchronized (operationQueues) {
            if (operationQueues.isEmpty()) {
                return;
            }
            Runnable runnable = () -> {
                List<UserOperation> operations = new LinkedList<>();
                if (operationQueues.size() >= 3) {
                    Logger.warning(
                        InstallDataEditor.TAG,
                        "It looks like you are using many instances of BatchUserDataEditor. Please check our documentation to ensure you are using this api correctly: https://doc.batch.com/android/custom-data/custom-attributes#methods"
                    );
                }
                for (UserOperationQueue userOperationQueue : operationQueues) {
                    operations.addAll(userOperationQueue.popOperations());
                }
                operationQueues.clear();
                if (operations.isEmpty()) {
                    return;
                }
                try {
                    applyUserOperationsSync(operations);
                } catch (UserModule.SaveException e) {
                    Logger.error(TAG, e.getMessage());
                }
            };
            submitOnApplyQueue(msDelay, runnable);
        }
    }

    public static void applyUserOperationsSync(List<UserOperation> pendingOperationQueue) throws SaveException {
        final Context context = RuntimeManagerProvider.get().getContext();

        if (context == null) {
            throw new SaveException(
                "Error while applying. Make sure Batch is started beforehand, and not globally opted out from.",
                "'context' was null while saving."
            );
        }

        final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);
        long changeset = Long.parseLong(ParametersProvider.get(context).get(ParameterKeys.USER_DATA_CHANGESET, "0"));
        changeset++;

        try {
            datasource.acquireTransactionLock(changeset);
        } catch (UserDatabaseException e) {
            throw new SaveException(
                "An internal error occurred while applying the changes (code 40)",
                "Could not acquire transaction lock",
                e
            );
        }

        Map<String, UserAttribute> previousAttributes = datasource.getAttributes();
        Map<String, Set<String>> previousTagCollections = datasource.getTagCollections();

        for (UserOperation operation : pendingOperationQueue) {
            try {
                operation.execute(datasource);
            } catch (Exception e) {
                try {
                    datasource.rollbackTransaction();
                } catch (UserDatabaseException e1) {
                    Logger.internal(InstallDataEditor.TAG, "Save - Error while rolling back transaction.", e1);
                }

                throw new SaveException(
                    "An internal error occurred while applying the changes (code 41)",
                    "Save - Callable exception",
                    e
                );
            }
        }

        try {
            datasource.commitTransaction();

            Map<String, UserAttribute> newAttributes = datasource.getAttributes();
            Map<String, Set<String>> newTagCollections = datasource.getTagCollections();

            UserDataDiff.Result diff = new UserDataDiff(
                newAttributes,
                previousAttributes,
                newTagCollections,
                previousTagCollections
            )
                .result;

            if (diff.hasChanges()) {
                final Parameters parameters = ParametersProvider.get(context);
                parameters.set(ParameterKeys.USER_DATA_CHANGESET, Long.toString(changeset), true);
                parameters.remove(ParameterKeys.USER_DATA_TRANSACTION_ID);

                UserModuleProvider.get().startSendWS(0);
                try {
                    TrackerModuleProvider
                        .get()
                        .track(InternalEvents.INSTALL_DATA_CHANGED, diff.toEventParameters(changeset));
                } catch (JSONException e2) {
                    Logger.internal(InstallDataEditor.TAG, "Could not serialize install data diff");
                    TrackerModuleProvider.get().track(InternalEvents.INSTALL_DATA_CHANGED_TRACK_FAILURE);
                }

                Logger.internal(InstallDataEditor.TAG, "Changeset bumped");
            } else {
                Logger.internal(InstallDataEditor.TAG, "Changeset not bumped");
            }
        } catch (UserDatabaseException e) {
            try {
                datasource.rollbackTransaction();
            } catch (UserDatabaseException e1) {
                Logger.internal(InstallDataEditor.TAG, "Error while rolling back transaction.", e1);
            }

            throw new SaveException(
                "An internal error occurred while applying the changes (code 42)",
                "Save - Commit exception",
                e
            );
        }
    }

    public static class SaveException extends Exception {

        public String internalErrorMessage;

        public SaveException(String message, String internalErrorMessage) {
            super(message);
            this.internalErrorMessage = internalErrorMessage;
        }

        public SaveException(String message, String internalErrorMessage, Throwable cause) {
            super(message, cause);
            this.internalErrorMessage = internalErrorMessage;
        }

        public void log() {
            Logger.error(InstallDataEditor.TAG, getMessage());
            Logger.internal(InstallDataEditor.TAG, internalErrorMessage, getCause());
        }
    }

    // endregion

    // region Opt Out

    public static void wipeData(Context context) {
        final Context c = context.getApplicationContext();

        submitOnApplyQueue(
            0,
            () -> {
                SQLUserDatasourceProvider.get(c).clear();

                Parameters parameters = ParametersProvider.get(c);
                parameters.remove(ParameterKeys.USER_PROFILE_REGION_KEY);
                parameters.remove(ParameterKeys.USER_PROFILE_LANGUAGE_KEY);
                parameters.remove(ParameterKeys.USER_DATA_VERSION);
                parameters.remove(ParameterKeys.USER_DATA_CHANGESET);
                parameters.remove(ParameterKeys.USER_DATA_TRANSACTION_ID);

                applyQueue.shutdownNow();
            }
        );
    }

    public static void userOptedIn() {
        if (applyQueue.isShutdown()) {
            applyQueue = makeApplyQueue();
        }
    }
    // endregion
}
