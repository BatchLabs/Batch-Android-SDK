package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.MessagePackWebservice;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.metrics.MetricRegistry;
import com.batch.android.post.LocalCampaignsJITPostDataProvider;
import com.batch.android.webservice.listener.LocalCampaignsJITWebserviceListener;
import java.net.MalformedURLException;
import java.util.List;

class LocalCampaignsJITWebservice extends MessagePackWebservice implements TaskRunnable {

    private static final String TAG = "LocalCampaignsJITWebservice";

    /**
     * Web service callback
     */
    private final LocalCampaignsJITWebserviceListener listener;

    protected LocalCampaignsJITWebservice(
        Context context,
        LocalCampaignsJITWebserviceListener listener,
        LocalCampaignsJITPostDataProvider dataProvider,
        String... parameters
    ) throws MalformedURLException {
        super(context, dataProvider, Parameters.LOCAL_CAMPAIGNS_JIT_WS_URL, addBatchApiKey(parameters));
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }
        this.listener = listener;
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/localcampaignsjitws";
    }

    @Override
    public void run() {
        Logger.internal(TAG, "Webservice started");
        MetricRegistry.localCampaignsJITResponseTime.startTimer();
        try {
            byte[] response = executeRequest();
            MetricRegistry.localCampaignsJITResponseTime.observeDuration();
            MetricRegistry.localCampaignsJITCount.labels("OK").inc();
            LocalCampaignsJITPostDataProvider dataProvider = (LocalCampaignsJITPostDataProvider) getPostDataProvider();
            List<String> eligibleCampaigns = dataProvider.unpack(response);
            this.listener.onSuccess(eligibleCampaigns);
        } catch (WebserviceError error) {
            MetricRegistry.localCampaignsJITResponseTime.observeDuration();
            MetricRegistry.localCampaignsJITCount.labels("KO").inc();
            Logger.internal(TAG, error.getReason().toString(), error.getCause());
            this.listener.onFailure(error);
        }
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
}
