package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.metrics.MetricRegistry;
import com.batch.android.query.LocalCampaignsQuery;
import com.batch.android.query.Query;
import com.batch.android.query.QueryType;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.webservice.listener.LocalCampaignsWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Webservice to ask the server for all type of local campaigns (be in-app or notification)
 *
 * @hide
 */
public class LocalCampaignsWebservice extends BatchQueryWebservice implements TaskRunnable {

    private static final String TAG = "LocalCampaignsWebservice";

    /**
     * Listener of this WS
     */
    private LocalCampaignsWebserviceListener listener;

    // ------------------------------------------>

    public LocalCampaignsWebservice(Context context, LocalCampaignsWebserviceListener listener)
        throws MalformedURLException {
        super(context, RequestType.POST, Parameters.LOCAL_CAMPAIGNS_WS_URL);
        this.listener = listener;
    }

    // ------------------------------------------>

    @Override
    protected List<Query> getQueries() {
        List<Query> queries = new ArrayList<>(1);

        queries.add(new LocalCampaignsQuery(CampaignManagerProvider.get(), applicationContext));

        return queries;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "local campaigns webservice started");
            webserviceMetrics.onWebserviceStarted(this);
            MetricRegistry.localCampaignsSyncResponseTime.startTimer();
            /*
             * Read response
             */
            JSONObject response = null;
            try {
                response = getStandardResponseBodyIfValid();
                MetricRegistry.localCampaignsSyncResponseTime.observeDuration();
                webserviceMetrics.onWebserviceFinished(this, true);
            } catch (WebserviceError error) {
                Logger.internal(
                    TAG,
                    "Error while getting local campaigns list : " + error.getReason().toString(),
                    error.getCause()
                );
                MetricRegistry.localCampaignsSyncResponseTime.observeDuration();
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
             * Read responses
             * As opposed to other webservices, this one does not fail if one query is missing, as we want the others to work
             */
            List<LocalCampaignsResponse> responses = new ArrayList<>();

            LocalCampaignsResponse localCampaignsResponse = getResponseFor(
                LocalCampaignsResponse.class,
                QueryType.LOCAL_CAMPAIGNS
            );
            if (localCampaignsResponse != null) {
                // If there's an error, we delete the local campaigns on the disk
                if (localCampaignsResponse.hasError()) {
                    Logger.internal(
                        TAG,
                        "Local campaigns response contains an error : ".concat(
                                localCampaignsResponse.getError().toString()
                            )
                    );
                    CampaignManagerProvider.get().deleteSavedCampaignsAsync(applicationContext);
                } else {
                    // else we save them
                    CampaignManagerProvider.get().saveCampaignsAsync(applicationContext, localCampaignsResponse);
                    responses.add(localCampaignsResponse);
                }
            } else {
                Logger.internal(TAG, "Missing In-App Campaigns response");
            }
            Logger.internal(TAG, "local campaigns webservice ended");
            listener.onSuccess(responses);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading LocalCampaigns response", e);
            listener.onError(FailReason.UNEXPECTED_ERROR);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/localcampaignsws";
    }

    // ------------------------------------------>

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_PROPERTY_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_RETRYCOUNT_KEY;
    }
}
