package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.json.JSONObject;
import com.batch.android.query.AttributesSendQuery;
import com.batch.android.query.Query;
import com.batch.android.query.QueryType;
import com.batch.android.query.response.AttributesSendResponse;
import com.batch.android.webservice.listener.AttributesSendWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Webservice to send the user attributes
 *
 * @hide
 */
class AttributesSendWebservice extends BatchQueryWebservice implements TaskRunnable {

    private static final String TAG = "AttributesSendWebservice";

    /**
     * Attributes version
     */
    private long version;

    /**
     * Attributes
     */
    private Map<String, Object> attributes;

    /**
     * Tags
     */
    private Map<String, Set<String>> tags;

    /**
     * Listener of this WS
     */
    private AttributesSendWebserviceListener listener;

    // ------------------------------------------>

    protected AttributesSendWebservice(
        Context context,
        long version,
        Map<String, Object> attributes,
        Map<String, Set<String>> tags,
        AttributesSendWebserviceListener listener
    ) throws MalformedURLException {
        super(context, RequestType.POST, Parameters.ATTR_SEND_WS_URL);
        if (version <= 0) {
            throw new IllegalArgumentException("version <= 0");
        }

        if (attributes == null) {
            throw new IllegalArgumentException("attributes==null");
        }

        if (tags == null) {
            throw new IllegalArgumentException("tags==null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("listener==null");
        }

        this.listener = listener;
        this.version = version;
        this.attributes = attributes;
        this.tags = tags;
    }

    // ------------------------------------------>

    @Override
    protected List<Query> getQueries() {
        List<Query> queries = new ArrayList<>(1);

        queries.add(new AttributesSendQuery(applicationContext, version, attributes, tags));

        return queries;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "Attributes send webservice started");
            webserviceMetrics.onWebserviceStarted(this);

            /*
             * Read response
             */
            JSONObject response = null;
            try {
                response = getStandardResponseBodyIfValid();
                webserviceMetrics.onWebserviceFinished(this, true);
            } catch (WebserviceError error) {
                Logger.internal(error.getReason().toString(), error.getCause());
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
            AttributesSendResponse sendResponse = getResponseFor(AttributesSendResponse.class, QueryType.ATTRIBUTES);
            if (sendResponse == null) {
                throw new NullPointerException("Missing attributes send response");
            }

            Logger.internal(TAG, "Attributes send webservice ended");

            // Call the listener
            listener.onSuccess(sendResponse);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading response", e);
            listener.onError(FailReason.UNEXPECTED_ERROR);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/attrsendws";
    }

    // ------------------------------------------>

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_PROPERTY_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_SEND_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.ATTR_SEND_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.ATTR_SEND_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.ATTR_SEND_WS_RETRYCOUNT_KEY;
    }
}
