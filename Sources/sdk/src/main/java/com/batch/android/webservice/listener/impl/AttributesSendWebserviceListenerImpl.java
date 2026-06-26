package com.batch.android.webservice.listener.impl;

import android.content.Context;
import com.batch.android.FailReason;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.query.response.AttributesSendResponse;
import com.batch.android.webservice.listener.AttributesSendWebserviceListener;

/**
 * Default implementation for attributes send webservice
 *
 */
public class AttributesSendWebserviceListenerImpl implements AttributesSendWebserviceListener {

    @Override
    public void onSuccess(AttributesSendResponse response) {
        UserModuleProvider.get().storeTransactionID(response.getTransactionID(), response.getVersion());
    }

    @Override
    public void onError(FailReason reason) {
        UserModuleProvider.get().startSendWS(5000);
    }
}
