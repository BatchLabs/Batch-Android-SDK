package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.push.Registration;
import com.batch.android.query.PushQuery;
import com.batch.android.query.Query;
import com.batch.android.query.QueryType;
import com.batch.android.query.StartQuery;
import com.batch.android.query.response.StartResponse;
import com.batch.android.webservice.listener.StartWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Start webservice
 *
 * @hide
 */
class StartWebservice extends BatchQueryWebservice implements TaskRunnable {

    private static final String TAG = "StartWebservice";

    /**
     * Given listener to send result to
     */
    private StartWebserviceListener listener;
    /**
     * Is the start coming from a push notification
     */
    private boolean fromPush;
    /**
     * Push id of the opening push (if {@link #fromPush} is true)
     */
    private String pushId;
    /**
     * Is the start for user activity
     */
    private boolean userActivity;

    // ------------------------------------------->

    /**
     * @param context
     * @param fromPush
     * @param pushId
     * @param userActivity
     * @param listener
     * @throws MalformedURLException
     */
    protected StartWebservice(
        Context context,
        boolean fromPush,
        String pushId,
        boolean userActivity,
        StartWebserviceListener listener
    ) throws MalformedURLException {
        super(context, RequestType.POST, Parameters.START_WS_URL);
        if (listener == null) {
            throw new NullPointerException("Null listener");
        }

        this.listener = listener;
        this.fromPush = fromPush;
        this.pushId = pushId;
        this.userActivity = userActivity;
    }

    // ------------------------------------------->

    @Override
    protected List<Query> getQueries() {
        List<Query> queries = new ArrayList<>();

        queries.add(new StartQuery(applicationContext, fromPush, pushId, userActivity));

        // Add push query if we have a push token
        Registration registration = PushModuleProvider.get().getRegistration(applicationContext);
        if (registration != null) {
            queries.add(new PushQuery(applicationContext, registration));
        }

        return queries;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "start webservice started");
            webserviceMetrics.onWebserviceStarted(this);

            /*
             * Read response
             */
            JSONObject response = null;
            try {
                response = getStandardResponseBodyIfValid();
                webserviceMetrics.onWebserviceFinished(this, true);
            } catch (WebserviceError error) {
                Logger.internal(TAG, "Error on StartWebservice : " + error.getReason().toString(), error.getCause());
                webserviceMetrics.onWebserviceFinished(this, false);

                switch (error.getReason()) {
                    case NETWORK_ERROR:
                        listener.onError(FailReason.NETWORK_ERROR);
                        break;
                    case INVALID_API_KEY:
                        listener.onError(FailReason.INVALID_API_KEY);
                        break;
                    case DEACTIVATED_API_KEY:
                        listener.onError(FailReason.DEACTIVATED_API_KEY);
                        break;
                    default:
                        listener.onError(FailReason.UNEXPECTED_ERROR);
                        break;
                }

                return;
            }

            /*
             * Parse response to retrieve responses for queries, parameters and other stuffs
             */
            parseResponse(response);

            /*
             * Read start query response
             */
            StartResponse startResponse = getResponseFor(StartResponse.class, QueryType.START);
            if (startResponse == null) {
                throw new NullPointerException("Missing start response");
            }

            Logger.internal(TAG, "start webservice ended");

            // Call the listener
            listener.onSuccess();
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading StartWebservice response", e);
            listener.onError(FailReason.UNEXPECTED_ERROR);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/startws";
    }

    // ----------------------------------------->

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.START_WS_PROPERTY_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.START_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.START_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.START_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.START_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.START_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.START_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.START_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.START_WS_RETRYCOUNT_KEY;
    }
}
