package com.batch.android.actions;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchDeeplinkInterceptor;
import com.batch.android.BatchPushPayload;
import com.batch.android.DeeplinkInterceptorRuntimeException;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.DeeplinkHelper;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

public class DeeplinkActionRunnable implements UserActionRunnable {

    private static final String TAG = "DeeplinkAction";
    public static final String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "deeplink";

    public static final String ARGUMENT_DEEPLINK_URL = "l";
    public static final String ARGUMENT_SHOW_LINK_INAPP = "li";

    private ActionModule actionModule;

    public DeeplinkActionRunnable(@NonNull ActionModule actionModule) {
        this.actionModule = actionModule;
    }

    private void launchDeeplink(@NonNull Context context, String deeplink, boolean useCustomtab)
        throws DeeplinkInterceptorRuntimeException {
        BatchDeeplinkInterceptor interceptor = actionModule.getDeeplinkInterceptor();
        if (interceptor != null) {
            try {
                Intent i;
                try {
                    i = interceptor.getIntent(context, deeplink);
                } catch (RuntimeException re) {
                    Logger.error(
                        TAG,
                        "Interceptor has thrown a runtime exception. Aborting deeplink opens by rethrowing",
                        re
                    );
                    throw new DeeplinkInterceptorRuntimeException(re);
                }

                if (i != null) {
                    context.startActivity(i);
                    return;
                }
            } catch (DeeplinkInterceptorRuntimeException die) {
                throw die;
            } catch (Exception e) {
                Logger.error(TAG, "Error when trying to open deeplink from interceptor. Using fallback intent.", e);

                Intent fallback;
                try {
                    fallback = interceptor.getFallbackIntent(context);
                } catch (RuntimeException re) {
                    Logger.error(
                        TAG,
                        "Interceptor has thrown a runtime exception. Aborting deeplink opens by rethrowing",
                        re
                    );
                    throw new DeeplinkInterceptorRuntimeException(re);
                }

                if (fallback != null) {
                    context.startActivity(fallback);
                }
                return;
            }
        }

        // Interceptor not set or return null, using default behaviour
        Intent i = DeeplinkHelper.getIntent(deeplink, useCustomtab, true);
        context.startActivity(i);
    }

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        if (context == null) {
            Logger.error(TAG, "Tried to perform a Deeplink action, but no context was available");
            return;
        }

        final String target = args.reallyOptString(ARGUMENT_DEEPLINK_URL, null);
        try {
            if (!TextUtils.isEmpty(target)) {
                // Only use a custom tab if enabled by the payload and not launched through a push action
                boolean useCustomTab =
                    args.reallyOptBoolean(ARGUMENT_SHOW_LINK_INAPP, false) && !(source instanceof BatchPushPayload);
                launchDeeplink(context, target, useCustomTab);
            } else {
                Logger.error(
                    TAG,
                    "Tried to perform a Deeplink action, but no deeplink was found in the args. (args: " +
                    args.toString() +
                    ")"
                );
            }
        } catch (DeeplinkInterceptorRuntimeException die) {
            throw die.getWrappedRuntimeException();
        } catch (ActivityNotFoundException e) {
            Logger.error(
                TAG,
                "Could not open deeplink: no activity found to handle Intent. Is it valid and your manifest well-formed? URL: " +
                target
            );
        } catch (Exception e) {
            Logger.internal(TAG, "Could not perform deeplink action", e);
            Logger.error(TAG, "Could not open deeplink: Unknown error.");
        }
    }
}
