package com.batch.android.actions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import com.batch.android.runtime.RuntimeManager;

public class LocalCampaignsRefreshActionRunnable implements UserActionRunnable {

    private static final String TAG = "LocalCampaignsRefreshAction";
    public static String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "refresh_lc";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        RuntimeManager rm = RuntimeManagerProvider.get();
        if (rm != null) {
            WebserviceLauncher.launchLocalCampaignsWebservice(rm);
        } else {
            Logger.error(
                TAG,
                "Tried to perform a Local Campaigns Refresh action, but was unable to get a RuntimeManager instance."
            );
        }
    }
}
