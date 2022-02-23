package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.json.JSONObject;
import com.batch.android.query.AttributesCheckQuery;
import com.batch.android.query.Query;
import com.batch.android.query.QueryType;
import com.batch.android.query.response.AttributesCheckResponse;
import com.batch.android.webservice.listener.AttributesCheckWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Webservice to check the user attributes validity
 * It allows the backend to instruct the SDK to do something to resolve the issue
 *
 * @hide
 */
class AttributesCheckWebservice extends BatchQueryWebservice implements TaskRunnable {

    private static final String TAG = "AttributesCheckWebservice";

    /**
     * Attributes version
     */
    private long version;

    /**
     * Saved transaciton ID for that version
     */
    private String transactionID;

    /**
     * Listener of this WS
     */
    private AttributesCheckWebserviceListener listener;

    // ------------------------------------------>

    protected AttributesCheckWebservice(
        Context context,
        long version,
        String transactionID,
        AttributesCheckWebserviceListener listener
    ) throws MalformedURLException {
        super(context, RequestType.POST, Parameters.ATTR_CHECK_WS_URL);
        if (version <= 0) {
            throw new IllegalArgumentException("version <= 0");
        }

        if (transactionID == null) {
            throw new NullPointerException("transactionid==null");
        }

        if (listener == null) {
            throw new NullPointerException("listener==null");
        }

        this.listener = listener;
        this.version = version;
        this.transactionID = transactionID;
    }

    // ------------------------------------------>

    @Override
    protected List<Query> getQueries() {
        List<Query> queries = new ArrayList<>(1);

        queries.add(new AttributesCheckQuery(applicationContext, version, transactionID));

        return queries;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "Attributes check webservice started");
            webserviceMetrics.onWebserviceStarted(this);

            /*
             * Read response
             */
            JSONObject response = null;
            try {
                response = getStandardResponseBodyIfValid();
                webserviceMetrics.onWebserviceFinished(this, true);
            } catch (WebserviceError error) {
                Logger.internal(TAG, error.getReason().toString(), error.getCause());
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
             * Read resposne
             */
            AttributesCheckResponse checkResponse = getResponseFor(
                AttributesCheckResponse.class,
                QueryType.ATTRIBUTES_CHECK
            );
            if (checkResponse == null) {
                throw new NullPointerException("Missing attributes check response");
            }

            Logger.internal(TAG, "Attributes check webservice ended");

            // Call the listener
            listener.onSuccess(checkResponse);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading response", e);
            listener.onError(FailReason.UNEXPECTED_ERROR);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/pushws";
    }

    // ------------------------------------------>

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_PROPERTY_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_CHECK_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.ATTR_CHECK_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.ATTR_CHECK_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.ATTR_CHECK_WS_RETRYCOUNT_KEY;
    }
}
