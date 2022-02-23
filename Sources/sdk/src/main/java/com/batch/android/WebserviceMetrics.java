package com.batch.android;

import com.batch.android.core.Logger;
import com.batch.android.core.Webservice;
import com.batch.android.inbox.InboxFetchWebserviceClient;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Static class that tracks webservices metrics for report
 *
 * @hide
 */
@Module
@Singleton
public class WebserviceMetrics {

    private static final String TAG = "WebserviceMetrics";

    /**
     * Metrics saved for future report
     */
    private final Map<String, Metric> metrics = new HashMap<>();
    /**
     * Map that saves start time for webservices
     */
    private final Map<String, Long> webservicesStartTime = new HashMap<>();

    // ---------------------------------------------->

    /**
     * Method that a webservice should call to report that its starting
     *
     * @param webservice
     */
    void onWebserviceStarted(Webservice webservice) {
        if (webservice == null) {
            throw new NullPointerException("webservice==null");
        }

        String shortName = shortNames.get(webservice.getClass());
        if (shortName == null) {
            Logger.internal(TAG, "Unknown webservice reported for metrics (" + webservice.getClass() + "), aborting");
            return;
        }

        synchronized (webservicesStartTime) {
            webservicesStartTime.put(shortName, System.currentTimeMillis());
        }
    }

    /**
     * Method that a webservice should call to report its success or failure
     *
     * @param webservice
     * @param success
     */
    void onWebserviceFinished(Webservice webservice, boolean success) {
        if (webservice == null) {
            throw new NullPointerException("webservice==null");
        }

        String shortName = shortNames.get(webservice.getClass());
        if (shortName == null) {
            Logger.internal(TAG, "Unknown webservice reported for metrics (" + webservice.getClass() + "), aborting");
            return;
        }

        Long startTime = webservicesStartTime.get(shortName);
        if (startTime == null) {
            Logger.internal(TAG, "Webservice finished without start recorded (" + shortName + "), aborting");
            return;
        }

        Metric metric = new Metric(success, System.currentTimeMillis() - startTime);

        synchronized (webservicesStartTime) {
            webservicesStartTime.remove(shortName);
        }

        synchronized (metrics) {
            metrics.put(shortName, metric);
        }
    }

    /**
     * Get the current saved metrics for report. Calling this method will clear saved metrics
     *
     * @return
     */
    Map<String, Metric> getMetrics() {
        synchronized (metrics) {
            Map<String, Metric> currentMetrics = new HashMap<>(metrics);
            metrics.clear();
            return currentMetrics;
        }
    }

    // ---------------------------------------------->

    /**
     * Static map of shortnames for webservice reporting
     */
    private static Map<Class<? extends Webservice>, String> shortNames = new HashMap<>();

    static {
        shortNames.put(StartWebservice.class, "s");
        shortNames.put(TrackerWebservice.class, "tr");
        shortNames.put(PushWebservice.class, "t");
        shortNames.put(AttributesSendWebservice.class, "ats");
        shortNames.put(AttributesCheckWebservice.class, "atc");
        shortNames.put(LocalCampaignsWebservice.class, "lc");
        shortNames.put(InboxFetchWebserviceClient.class, "inbox");
    }

    // ----------------------------------------------->

    /**
     * Static object that represent a webservice metric
     *
     */
    static final class Metric {

        /**
         * Was this webservice call a success
         */
        protected boolean success;
        /**
         * Duration of the webservice call
         */
        protected long time;

        // ------------------------------------------>

        /**
         * @param success
         * @param time
         */
        private Metric(boolean success, long time) {
            this.success = success;
            this.time = time;
        }
    }
}
