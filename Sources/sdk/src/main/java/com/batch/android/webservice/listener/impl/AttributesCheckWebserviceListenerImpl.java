package com.batch.android.webservice.listener.impl;

import android.content.Context;
import com.batch.android.FailReason;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.ProfileModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.query.response.AttributesCheckResponse;
import com.batch.android.webservice.listener.AttributesCheckWebserviceListener;

/**
 * Default implementation for attributes send webservice
 *
 */
public class AttributesCheckWebserviceListenerImpl implements AttributesCheckWebserviceListener {

    private static final long DEFAULT_RECHECK_TIME = 15000L; // 15s

    @Override
    public void onSuccess(AttributesCheckResponse response) {
        boolean foundValidAction = false;

        switch (response.getAction()) {
            case OK:
                // All good
                foundValidAction = true;
                break;
            case RESEND:
                {
                    Long delay = response.time;
                    if (delay == null || delay < 0) {
                        delay = 0L;
                    }

                    UserModuleProvider.get().startSendWS(delay);
                    foundValidAction = true;
                    break;
                }
            case RECHECK:
                {
                    Long delay = response.time;
                    if (delay == null) {
                        delay = DEFAULT_RECHECK_TIME;
                    }

                    if (delay < 0) {
                        delay = 0L;
                    }

                    UserModuleProvider.get().startCheckWS(delay);
                    foundValidAction = true;
                    break;
                }
            case BUMP:
                if (response.version <= 0) {
                    // The server can't be at 0 and ask to bump.
                    break;
                }

                UserModuleProvider.get().bumpVersion(response.version);
                foundValidAction = true;
                break;
            case UNKNOWN:
                break;
        }

        if (!foundValidAction) {
            UserModuleProvider.get().startCheckWS(DEFAULT_RECHECK_TIME);
        }

        // Detecting whether project has changed
        Context context = RuntimeManagerProvider.get().getContext();
        String projectKey = response.getProjectKey();
        if (projectKey != null && context != null) {
            Parameters parameters = ParametersProvider.get(context);
            String currentProjectKey = parameters.get(ParameterKeys.PROJECT_KEY);
            if (!projectKey.equals(currentProjectKey)) {
                parameters.set(ParameterKeys.PROJECT_KEY, projectKey, true);
                ProfileModuleProvider.get().onProjectChanged(currentProjectKey, projectKey);
            }
        }
    }

    @Override
    public void onError(FailReason reason) {
        UserModuleProvider.get().startCheckWS(DEFAULT_RECHECK_TIME);
    }
}
