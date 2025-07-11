package com.batch.android.module;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.batch.android.BatchOptOutResultListener;
import com.batch.android.core.Logger;
import com.batch.android.core.Parameters;
import com.batch.android.core.Promise;
import com.batch.android.di.providers.InboxDatasourceProvider;
import com.batch.android.di.providers.LocalBroadcastManagerProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import com.batch.android.util.MetaDataUtils;

/**
 * Batch's Opt Out Module.
 *
 */
@Module
@Singleton
public class OptOutModule extends BatchModule {

    public static final String TAG = "OptOut";

    public static final String INTENT_OPTED_OUT = Parameters.LIBRARY_BUNDLE + ".optout.enabled";

    public static final String INTENT_OPTED_IN = Parameters.LIBRARY_BUNDLE + ".optout.disabled";

    public static final String INTENT_OPTED_OUT_WIPE_DATA_EXTRA = "wipe_data";

    private static final String OPT_OUT_PREFERENCES_NAME = "com.batch.optout";

    private static final String OPTED_OUT_FROM_BATCHSDK_KEY = "app.batch.opted_out";

    private static final String SHOULD_SEND_OPTIN_EVENT_KEY = "app.batch.send_optin_event";

    private Boolean isOptedOut = null;

    private SharedPreferences preferences;

    public OptOutModule() {}

    private synchronized SharedPreferences getPreferences(Context context) {
        if (preferences == null) {
            preferences =
                context.getApplicationContext().getSharedPreferences(OPT_OUT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
        return preferences;
    }

    public Boolean isOptedOut() {
        return isOptedOut;
    }

    public boolean isOptedOutSync(Context context) {
        if (isOptedOut == null) {
            SharedPreferences prefs = getPreferences(context);
            if (prefs.contains(OPTED_OUT_FROM_BATCHSDK_KEY)) {
                isOptedOut = prefs.getBoolean(OPTED_OUT_FROM_BATCHSDK_KEY, false);
            } else {
                isOptedOut = MetaDataUtils.getBooleanMetaData(context, MetaDataUtils.MANIFEST_OPT_OUT_BY_DEFAULT_KEY);
                prefs.edit().putBoolean(OPTED_OUT_FROM_BATCHSDK_KEY, isOptedOut).apply();
                if (isOptedOut) {
                    Logger.info(
                        TAG,
                        "Batch has been set to be Opted Out from by default in your app's manifest. You will need to call Batch.optIn() before performing anything else."
                    );
                }
            }
        }
        return isOptedOut;
    }

    public void trackOptinEventIfNeeded(@NonNull Context context) {
        SharedPreferences prefs = getPreferences(context);
        if (prefs.getBoolean(SHOULD_SEND_OPTIN_EVENT_KEY, false)) {
            try {
                TrackerModuleProvider.get().trackOptInEvent(context);
                prefs.edit().remove(SHOULD_SEND_OPTIN_EVENT_KEY).apply();
            } catch (JSONException e) {
                Logger.internal(TAG, "Could not track optin", e);
            }
        }
    }

    public void optIn(Context context) {
        Logger.internal(TAG, "Opt In");
        boolean oldOptOutValue = isOptedOutSync(context);
        if (oldOptOutValue) {
            isOptedOut = false;
            getPreferences(context)
                .edit()
                .putBoolean(OPTED_OUT_FROM_BATCHSDK_KEY, false)
                .putBoolean(SHOULD_SEND_OPTIN_EVENT_KEY, true)
                .apply();
            LocalBroadcastManagerProvider.get(context).sendBroadcast(new Intent(INTENT_OPTED_IN));
        }
    }

    public Promise<Void> optOut(
        final Context context,
        final boolean wipeData,
        final BatchOptOutResultListener listener
    ) {
        // This is different than the event one, as this one is resolved
        // when the Opt-Out or wipe has actually been executed, taking into account whether
        // we should wait for the event and the developer's callback failure decision
        final Promise<Void> optOutAppliedPromise = new Promise<>();

        Logger.internal(TAG, "Opt Out, wipe data: " + wipeData);
        boolean oldOptOutValue = isOptedOutSync(context);
        if (!oldOptOutValue) {
            Promise<Void> eventPromise = null;
            if (wipeData) {
                eventPromise =
                    TrackerModuleProvider.get().trackOptOutEvent(context, InternalEvents.OPT_OUT_AND_WIPE_DATA);
            } else {
                eventPromise = TrackerModuleProvider.get().trackOptOutEvent(context, InternalEvents.OPT_OUT);
            }

            if (listener == null) {
                // Don't wait on the event to maintain backwards-compatible behaviour
                eventPromise = Promise.resolved(null);
            }

            eventPromise.then(value ->
                new Handler(context.getMainLooper())
                    .post(() -> {
                        if (listener != null) {
                            listener.onSuccess();
                        }
                        doOptOut(context, wipeData);
                        optOutAppliedPromise.resolve(null);
                    })
            );

            eventPromise.catchException(e ->
                new Handler(context.getMainLooper())
                    .post(() -> {
                        if (listener != null) {
                            if (listener.onError() == BatchOptOutResultListener.ErrorPolicy.CANCEL) {
                                optOutAppliedPromise.reject(null);
                                return;
                            }
                        }
                        doOptOut(context, wipeData);
                        optOutAppliedPromise.resolve(null);
                    })
            );
        } else {
            optOutAppliedPromise.reject(null);
        }

        return optOutAppliedPromise;
    }

    private void doOptOut(Context context, boolean wipeData) {
        if (wipeData) {
            wipeData(context);
        }

        isOptedOut = true;
        getPreferences(context).edit().putBoolean(OPTED_OUT_FROM_BATCHSDK_KEY, true).apply();
        final Intent i = new Intent(INTENT_OPTED_OUT);
        i.putExtra(INTENT_OPTED_OUT_WIPE_DATA_EXTRA, wipeData);
        LocalBroadcastManagerProvider.get(context).sendBroadcast(i);
    }

    @VisibleForTesting
    public void wipeData(Context context) {
        Logger.internal(TAG, "Wiping data");
        UserModule.wipeData(context);
        TrackerModuleProvider.get().wipeData(context);
        LocalCampaignsModuleProvider.get().wipeData(context);
        InboxDatasourceProvider.get(context).wipeData();
        ParametersProvider.get(context).wipeData();
    }

    //region: BatchModule

    @Override
    public String getId() {
        return "optout";
    }

    @Override
    public int getState() {
        return 1;
    }
    //endregion
}
