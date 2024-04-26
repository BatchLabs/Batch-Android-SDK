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

        // Detecting whether project has changed
        Context context = RuntimeManagerProvider.get().getContext();
        String projectKey = response.getProjectKey();
        if (projectKey != null && context != null) {
            Parameters parameters = ParametersProvider.get(context);
            String currentProjectKey = parameters.get(ParameterKeys.PROJECT_KEY);
            if (!projectKey.equals(currentProjectKey)) {
                // If we are here this mean we are running on a fresh V2 install and user has
                // just wrote some profile data.
                // So we save the project key to not trigger the profile data migration from the
                // next ATC response otherwise we would erase the data we just sent.
                parameters.set(ParameterKeys.PROJECT_KEY, projectKey, true);
            }
        }
    }

    @Override
    public void onError(FailReason reason) {
        UserModuleProvider.get().startSendWS(5000);
    }
}
