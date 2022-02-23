package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.event.Event;
import com.batch.android.json.JSONObject;
import com.batch.android.query.Query;
import com.batch.android.query.TrackingQuery;
import com.batch.android.webservice.listener.TrackerWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
final class TrackerWebservice extends BatchQueryWebservice implements TaskRunnable {

    private static final String TAG = "TrackerWebservice";
    private TrackerWebserviceListener listener;
    private List<Event> events;
    private boolean canBypassOptOut;

    // ----------------------------------------->

    protected TrackerWebservice(
        Context context,
        TrackerWebserviceListener listener,
        List<Event> events,
        boolean canBypassOptOut
    ) throws MalformedURLException {
        super(context, RequestType.POST, Parameters.TRACKER_WS_URL);
        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        if (events == null || events.isEmpty()) {
            throw new NullPointerException("events is empty");
        }

        this.listener = listener;
        this.events = new ArrayList<>(events);
        this.canBypassOptOut = canBypassOptOut;
    }

    // ----------------------------------------->

    @Override
    protected boolean canBypassOptOut() {
        return this.canBypassOptOut;
    }

    @Override
    protected List<Query> getQueries() {
        List<Query> queries = new ArrayList<>(1);

        queries.add(new TrackingQuery(applicationContext, events));

        return queries;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "tracker webservice started");
            webserviceMetrics.onWebserviceStarted(this);

            /*
             * Read response
             */
            JSONObject response = null;
            try {
                response = getStandardResponseBodyIfValid();
                webserviceMetrics.onWebserviceFinished(this, true);
            } catch (WebserviceError error) {
                Logger.internal(TAG, "Error on TrackerWebservice : " + error.getReason().toString(), error.getCause());
                webserviceMetrics.onWebserviceFinished(this, false);

                switch (error.getReason()) {
                    case NETWORK_ERROR:
                        listener.onFailure(FailReason.NETWORK_ERROR, events);
                        break;
                    case INVALID_API_KEY:
                        listener.onFailure(FailReason.INVALID_API_KEY, events);
                        break;
                    case DEACTIVATED_API_KEY:
                        listener.onFailure(FailReason.DEACTIVATED_API_KEY, events);
                        break;
                    default:
                        listener.onFailure(FailReason.UNEXPECTED_ERROR, events);
                        break;
                }

                return;
            }

            /*
             * Parse response to retrieve responses for queries, parameters and other stuffs
             */
            parseResponse(response);

            Logger.internal(TAG, "tracker webservice ended");

            // Call the listener
            listener.onSuccess(events);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading TrackerWebservice response", e);
            listener.onFailure(FailReason.UNEXPECTED_ERROR, events);
        } finally {
            listener.onFinish();
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/trackerws";
    }

    // ----------------------------------------->

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.TRACKER_WS_PROPERTY_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.TRACKER_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.TRACKER_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.TRACKER_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.TRACKER_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.TRACKER_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.TRACKER_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.TRACKER_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.TRACKER_WS_RETRYCOUNT_KEY;
    }
}
