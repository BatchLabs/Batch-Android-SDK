package com.batch.android.module;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import com.batch.android.BatchUserDataEditor;
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
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import com.batch.android.user.UserDataDiff;
import com.batch.android.user.UserDatabaseException;
import com.batch.android.user.UserOperation;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
    public static final String PARAMETER_KEY_DATA = "data";
    public static final String PARAMETER_KEY_AMOUNT = "amount";

    private static final Pattern EVENT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,30}$");

    private static final long LOCATION_UPDATE_MINIMUM_TIME_MS = 30000;
    private static final long CIPHER_FALLBACK_RESET_TIME_MS = 172800000L;

    private static ScheduledExecutorService applyQueue = makeApplyQueue();

    private static ScheduledExecutorService makeApplyQueue() {
        return Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory());
    }

    private static final List<UserOperation> pendingUserOperation = new LinkedList<>();

    private BroadcastReceiver localBroadcastReceiver;

    private AtomicBoolean checkScheduled = new AtomicBoolean(false);

    private long lastLocationTrackTimestamp = 0;

    private TrackerModule trackerModule;

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
                                userOptedIn(context);
                            }
                        }
                    };
                LocalBroadcastManagerProvider
                    .get(context)
                    .registerReceiver(localBroadcastReceiver, new IntentFilter(OptOutModule.INTENT_OPTED_IN));
            }
        }

        executeUserPendingOperations();
    }

    // endregion

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
                    UserAttribute.getServerMapRepresentation(datasource.getAttributes()),
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

    // region Event tracking

    /**
     * Track a public event
     *
     * @param event Event name
     * @param label Event label
     * @param data  Event data, expected to already be converted from BatchEventData.
     */
    public void trackPublicEvent(String event, String label, JSONObject data) {
        JSONObject params;

        try {
            if (data != null) {
                params = new JSONObject(data);
            } else {
                params = new JSONObject();
            }

            if (label != null) {
                if (label.length() > 200) {
                    Logger.internal(
                        TAG,
                        "Event label is longer than 200 characters and has been removed from the event"
                    );
                } else if (label.length() > 0) {
                    params.put(PARAMETER_KEY_LABEL, label);
                }
            }
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not add public event data", e);
            params = null;
        }

        _trackEvent(event, params);
    }

    @VisibleForTesting
    protected boolean _trackEvent(String name, JSONObject params) {
        boolean nameValidated = !TextUtils.isEmpty(name) && EVENT_NAME_PATTERN.matcher(name).matches();

        if (!nameValidated) {
            Logger.error(TAG, "Invalid event name ('" + name + "'). Not tracking.");
            return false;
        }

        trackerModule.track("E." + name.toUpperCase(Locale.US), params);
        return true;
    }

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

    public void trackTransaction(double amount, JSONObject data) {
        try {
            final JSONObject params = new JSONObject();

            params.put(PARAMETER_KEY_AMOUNT, amount);

            if (data != null) {
                params.put(PARAMETER_KEY_DATA, data.toString());
            }

            trackerModule.track("T", params);
        } catch (JSONException e) {
            Logger.error(TAG, "Failed to track transaction", e);
        }
    }

    //endregion

    // region Apply queue

    public static void submitOnApplyQueue(long msDelay, Runnable r) {
        if (!applyQueue.isShutdown()) {
            applyQueue.schedule(r, msDelay, TimeUnit.MILLISECONDS);
        } else {
            Logger.error(
                BatchUserDataEditor.TAG,
                "Could not perform User Data operation. Is this installation currently opted out from Batch?"
            );
        }
    }

    // endregion

    // region User operations

    /**
     * Add pending operations when {@link BatchUserDataEditor#save()} is called before the SDK is started
     *
     * @param operations
     */
    public static void addUserPendingOperations(List<UserOperation> operations) {
        synchronized (pendingUserOperation) {
            pendingUserOperation.addAll(operations);
        }
    }

    /**
     * Execute pending operations in the apply queue when the {@link UserModule} is started
     */
    private void executeUserPendingOperations() {
        synchronized (pendingUserOperation) {
            if (!pendingUserOperation.isEmpty()) {
                final List<UserOperation> tmpOperationQueue = new LinkedList<>(pendingUserOperation);
                pendingUserOperation.clear();

                Runnable runnable = () -> {
                    try {
                        UserModule.applyUserOperationsSync(tmpOperationQueue);
                    } catch (UserModule.SaveException e) {
                        e.log();
                    }
                };

                // Execute operations on apply queue
                UserModule.submitOnApplyQueue(0, runnable);
            }
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

        if (datasource == null) {
            throw new SaveException("Datasource error while applying.");
        }

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
                    Logger.internal(BatchUserDataEditor.TAG, "Save - Error while rolling back transaction.", e1);
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
                    Logger.internal(BatchUserDataEditor.TAG, "Could not serialize install data diff");
                    TrackerModuleProvider.get().track(InternalEvents.INSTALL_DATA_CHANGED_TRACK_FAILURE);
                }

                Logger.internal(BatchUserDataEditor.TAG, "Changeset bumped");
            } else {
                Logger.internal(BatchUserDataEditor.TAG, "Changeset not bumped");
            }
        } catch (UserDatabaseException e) {
            try {
                datasource.rollbackTransaction();
            } catch (UserDatabaseException e1) {
                Logger.internal(BatchUserDataEditor.TAG, "Error while rolling back transaction.", e1);
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

        public SaveException(String message) {
            super(message);
        }

        public SaveException(String message, String internalErrorMessage) {
            super(message);
            this.internalErrorMessage = internalErrorMessage;
        }

        public SaveException(String message, String internalErrorMessage, Throwable cause) {
            super(message, cause);
            this.internalErrorMessage = internalErrorMessage;
        }

        public void log() {
            Logger.error(BatchUserDataEditor.TAG, getMessage());
            Logger.internal(BatchUserDataEditor.TAG, internalErrorMessage, getCause());
        }
    }

    // endregion

    // region Debug

    public static void printDebugInfo() {
        submitOnApplyQueue(
            0,
            () -> {
                final Context context = RuntimeManagerProvider.get().getContext();
                if (context != null) {
                    SQLUserDatasourceProvider.get(context).printDebugDump();
                } else {
                    Logger.error(
                        BatchUserDataEditor.TAG,
                        "Error while printing User Data Debug information: Batch must be started."
                    );
                }
            }
        );
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

    public static void userOptedIn(Context context) {
        if (applyQueue.isShutdown()) {
            applyQueue = makeApplyQueue();
        }
    }
    // endregion

}
