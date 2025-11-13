package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.Webservice;
import com.batch.android.core.domain.DomainURLBuilder;
import com.batch.android.post.MetricPostDataProvider;
import com.batch.android.webservice.listener.MetricWebserviceListener;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

class MetricWebservice extends Webservice implements TaskRunnable {

    private static final String TAG = "MetricWebservice";

    private final MetricWebserviceListener listener;
    private final MetricPostDataProvider dataProvider;

    protected MetricWebservice(
        Context context,
        MetricWebserviceListener listener,
        MetricPostDataProvider dataProvider,
        String... parameters
    ) throws MalformedURLException {
        super(context, RequestType.POST, DomainURLBuilder.METRIC_WS_URL, parameters);
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }
        if (dataProvider == null || dataProvider.isEmpty()) {
            throw new NullPointerException("Provider is empty");
        }

        this.listener = listener;
        this.dataProvider = dataProvider;
    }

    @Override
    public void run() {
        Logger.internal(TAG, "Webservice started");
        try {
            executeRequest();
            listener.onSuccess();
        } catch (WebserviceError error) {
            listener.onFailure(error);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/metricsws";
    }

    @Override
    protected MetricPostDataProvider getPostDataProvider() {
        return dataProvider;
    }

    @Override
    protected Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-batch-sdk-version", Parameters.SDK_VERSION);
        return headers;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.METRIC_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.METRIC_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.METRIC_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.METRIC_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.METRIC_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.METRIC_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.METRIC_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.METRIC_WS_RETRYCOUNT_KEY;
    }
}
