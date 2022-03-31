package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.LocalCampaignsWebserviceListenerImplProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.event.Event;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.post.DisplayReceiptPostDataProvider;
import com.batch.android.post.LocalCampaignsJITPostDataProvider;
import com.batch.android.post.MetricPostDataProvider;
import com.batch.android.push.Registration;
import com.batch.android.runtime.RuntimeManager;
import com.batch.android.webservice.listener.DisplayReceiptWebserviceListener;
import com.batch.android.webservice.listener.LocalCampaignsJITWebserviceListener;
import com.batch.android.webservice.listener.MetricWebserviceListener;
import com.batch.android.webservice.listener.TrackerWebserviceListener;
import com.batch.android.webservice.listener.impl.AttributesCheckWebserviceListenerImpl;
import com.batch.android.webservice.listener.impl.AttributesSendWebserviceListenerImpl;
import com.batch.android.webservice.listener.impl.PushWebserviceListenerImpl;
import com.batch.android.webservice.listener.impl.StartWebserviceListenerImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple proxy to allow webservice call from outside this package
 *
 * @hide
 */
public final class WebserviceLauncher {

    private static final String TAG = "WebserviceLauncher";

    /**
     * Launch the start webservice
     */
    public static boolean launchStartWebservice(
        RuntimeManager runtimeManager,
        boolean fromPush,
        String pushId,
        boolean userActivity
    ) {
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(
                    new StartWebservice(
                        runtimeManager.getContext(),
                        fromPush,
                        pushId,
                        userActivity,
                        new StartWebserviceListenerImpl()
                    )
                );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing SW", e);
            return false;
        }
    }

    /**
     * Create an instance of the Tracker webservice and return the runnable
     *
     * @param runtimeManager
     * @param events
     * @param listener
     * @return instance of tracker webservice ready to be runned
     */
    public static TaskRunnable initTrackerWebservice(
        RuntimeManager runtimeManager,
        List<Event> events,
        TrackerWebserviceListener listener
    ) {
        try {
            return new TrackerWebservice(runtimeManager.getContext(), listener, events, false);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing TW", e);
            return null;
        }
    }

    /**
     * Create an instance of the display receipt webservice and return the runnable
     *
     * @param context
     * @param dataProvider
     * @param listener
     * @return instance of the webservice ready to be run
     */
    public static TaskRunnable initDisplayReceiptWebservice(
        Context context,
        DisplayReceiptPostDataProvider dataProvider,
        DisplayReceiptWebserviceListener listener
    ) {
        try {
            return new DisplayReceiptWebservice(context, listener, dataProvider);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing DRW", e);
            return null;
        }
    }

    /**
     * Create an instance of the Opt-Out Tracker webservice and return the runnable
     *
     * @param events
     * @param listener
     * @return instance of tracker webservice ready to be runned
     */
    public static TaskRunnable initOptOutTrackerWebservice(
        Context context,
        List<Event> events,
        TrackerWebserviceListener listener
    ) {
        try {
            return new TrackerWebservice(context, listener, events, true);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing TW", e);
            return null;
        }
    }

    /**
     * Create an instance of the metrics webservice and return the runnable
     *
     * @param context      android context
     * @param dataProvider provider
     * @param listener     listener
     * @return instance of the webservice ready to be run
     */
    public static TaskRunnable initMetricWebservice(
        Context context,
        MetricPostDataProvider dataProvider,
        MetricWebserviceListener listener
    ) {
        try {
            return new MetricWebservice(context, listener, dataProvider);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing metrics webservice", e);
            return null;
        }
    }

    /**
     * Launch the push webservice
     */
    public static boolean launchPushWebservice(RuntimeManager runtimeManager, @NonNull Registration registration) {
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(
                    new PushWebservice(runtimeManager.getContext(), registration, new PushWebserviceListenerImpl())
                );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing PW", e);
            return false;
        }
    }

    public static boolean launchAttributesSendWebservice(
        RuntimeManager runtimeManager,
        long version,
        Map<String, Object> attributes,
        Map<String, Set<String>> tags
    ) {
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(
                    new AttributesSendWebservice(
                        runtimeManager.getContext(),
                        version,
                        attributes,
                        tags,
                        new AttributesSendWebserviceListenerImpl()
                    )
                );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing ATS WS", e);
            return false;
        }
    }

    public static boolean launchAttributesCheckWebservice(
        RuntimeManager runtimeManager,
        long version,
        String transactionID
    ) {
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(
                    new AttributesCheckWebservice(
                        runtimeManager.getContext(),
                        version,
                        transactionID,
                        new AttributesCheckWebserviceListenerImpl()
                    )
                );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing ATC WS", e);
            return false;
        }
    }

    public static boolean launchLocalCampaignsWebservice(RuntimeManager runtimeManager) {
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(
                    new LocalCampaignsWebservice(
                        runtimeManager.getContext(),
                        LocalCampaignsWebserviceListenerImplProvider.get()
                    )
                );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing LC WS", e);
            return false;
        }
    }

    public static boolean launchLocalCampaignsJITWebservice(
        RuntimeManager runtimeManager,
        List<LocalCampaign> campaigns,
        LocalCampaignsJITWebserviceListener listener
    ) {
        LocalCampaignsJITPostDataProvider dataProvider = new LocalCampaignsJITPostDataProvider(campaigns);
        try {
            TaskExecutorProvider
                .get(runtimeManager.getContext())
                .submit(new LocalCampaignsJITWebservice(runtimeManager.getContext(), listener, dataProvider));
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while initializing Local Campaigns JIT WS", e);
            return false;
        }
    }
}
