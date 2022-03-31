package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.MessagePackWebservice;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.post.MetricPostDataProvider;
import com.batch.android.webservice.listener.MetricWebserviceListener;
import java.net.MalformedURLException;

class MetricWebservice extends MessagePackWebservice implements TaskRunnable {

    private static final String TAG = "MetricWebservice";

    private final MetricWebserviceListener listener;

    protected MetricWebservice(
        Context context,
        MetricWebserviceListener listener,
        MetricPostDataProvider dataProvider,
        String... parameters
    ) throws MalformedURLException {
        super(context, dataProvider, Parameters.METRIC_WS_URL, parameters);
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        Logger.internal(TAG, "Webservice started");
        try {
            executeRequest();
            this.listener.onSuccess();
        } catch (WebserviceError error) {
            this.listener.onFailure(error);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/metricsws";
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.METRIC_WS_RETRYCOUNT_KEY;
    }
}
