package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.core.MessagePackWebservice;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.post.DisplayReceiptPostDataProvider;
import com.batch.android.webservice.listener.DisplayReceiptWebserviceListener;
import java.net.MalformedURLException;

class DisplayReceiptWebservice extends MessagePackWebservice implements TaskRunnable {

    private static final String TAG = "DisplayReceiptWebservice";

    private final DisplayReceiptWebserviceListener listener;

    /**
     * @param context      Android context
     * @param listener
     * @param dataProvider
     * @param parameters
     * @throws MalformedURLException
     */
    protected DisplayReceiptWebservice(
        Context context,
        DisplayReceiptWebserviceListener listener,
        DisplayReceiptPostDataProvider dataProvider,
        String... parameters
    ) throws MalformedURLException {
        super(context, dataProvider, Parameters.DISPLAY_RECEIPT_WS_URL, addSchemaVersion(parameters));
        if (listener == null) {
            throw new NullPointerException("Listener is null");
        }
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "Webservice started");
            executeRequest();
            listener.onSuccess();
        } catch (WebserviceError error) {
            listener.onFailure(error);
        }
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/receiptws";
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.DISPLAY_RECEIPT_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.DISPLAY_RECEIPT_WS_RETRYCOUNT_KEY;
    }
}
