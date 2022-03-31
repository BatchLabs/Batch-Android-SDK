package com.batch.android.localcampaigns.output;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.ActionModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.module.LocalCampaignsModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.runtime.RuntimeManager;

@Module
public class ActionOutput extends LocalCampaign.Output {

    public ActionOutput(@NonNull JSONObject payload) {
        super(payload);
    }

    @Provide
    public static ActionOutput provide(@NonNull JSONObject payload) {
        return new ActionOutput(payload);
    }

    @Override
    protected boolean displayMessage(LocalCampaign campaign) {
        final RuntimeManager runtimeManager = RuntimeManagerProvider.get();
        Context targetContext = runtimeManager.getActivity();
        if (targetContext == null) {
            Logger.warning(
                LocalCampaignsModule.TAG,
                "Could not find an activity to run the action on, falling back on context."
            );

            targetContext = runtimeManager.getContext();
        }
        if (targetContext == null) {
            Logger.warning(
                LocalCampaignsModule.TAG,
                "Could not find any context to run the action on: action might fail."
            );
        }

        String actionIdentifier = payload.reallyOptString("action", null);
        if (TextUtils.isEmpty(actionIdentifier)) {
            Logger.error(LocalCampaignsModule.TAG, "Invalid action name, stopping.");
            return false;
        }

        JSONObject actionArgs = payload.optJSONObject("args");

        if (actionArgs == null) {
            actionArgs = new JSONObject();
        }

        // Maybe add a UserActionSource in the future if this becomes a real product
        // InAppMessageUserActionSource isn't right for the job here, as it's tightly coupled
        // to the landings.
        return ActionModuleProvider.get().performAction(targetContext, actionIdentifier, actionArgs, null);
    }
}
