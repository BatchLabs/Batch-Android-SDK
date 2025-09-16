package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.domain.DomainURLBuilder;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.metrics.MetricRegistry;
import com.batch.android.post.LocalCampaignsJITPostDataProvider;
import com.batch.android.post.PostDataProvider;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.webservice.listener.LocalCampaignsJITWebserviceListener;
import java.net.MalformedURLException;
import java.util.List;

class LocalCampaignsJITWebservice extends BatchWebservice implements TaskRunnable {

    private static final String TAG = "LocalCampaignsJITWebservice";

    /**
     * Web service callback
     */
    @NonNull
    private final LocalCampaignsJITWebserviceListener listener;

    @NonNull
    private final LocalCampaignsJITPostDataProvider dataProvider;

    @SuppressWarnings("ConstantConditions")
    protected LocalCampaignsJITWebservice(
        Context context,
        @NonNull LocalCampaignsJITWebserviceListener listener,
        @NonNull LocalCampaignsJITPostDataProvider dataProvider,
        @NonNull LocalCampaignsResponse.Version campaignsVersion,
        String... parameters
    ) throws MalformedURLException {
        super(
            context,
            RequestType.POST,
            campaignsVersion == LocalCampaignsResponse.Version.MEP
                ? DomainURLBuilder.LOCAL_CAMPAIGNS_JIT_MEP_WS_URL
                : DomainURLBuilder.LOCAL_CAMPAIGNS_JIT_CEP_WS_URL,
            addBatchApiKey(parameters)
        );
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }
        if (dataProvider == null) {
            throw new NullPointerException("DataProvider is null");
        }
        this.listener = listener;
        this.dataProvider = dataProvider;
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/localcampaignsjitws";
    }

    @Override
    protected PostDataProvider<JSONObject> getPostDataProvider() {
        return dataProvider;
    }

    @Override
    public void run() {
        Logger.internal(TAG, "Webservice started");
        MetricRegistry.localCampaignsJITResponseTime.startTimer();
        try {
            JSONObject response = getBasicJsonResponseBody();
            MetricRegistry.localCampaignsJITResponseTime.observeDuration();
            MetricRegistry.localCampaignsJITCount.labels("OK").inc();
            List<String> eligibleCampaigns = dataProvider.deserializeResponse(response);
            this.listener.onSuccess(eligibleCampaigns);
        } catch (WebserviceError error) {
            MetricRegistry.localCampaignsJITResponseTime.observeDuration();
            MetricRegistry.localCampaignsJITCount.labels("KO").inc();
            Logger.internal(TAG, error.getReason().toString(), error.getCause());
            this.listener.onFailure(error);
        } catch (JSONException e) {
            Logger.internal(TAG, "Error while parsing response", e);
            this.listener.onFailure(null);
        }
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_RETRYCOUNT_KEY;
    }

    @Override
    protected String getPropertyParameterKey() {
        return ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_PROPERTY_KEY;
    }
}
