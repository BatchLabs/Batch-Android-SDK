package com.batch.android.actions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

/**
 * Action that runs an array of actions
 */
public class GroupActionRunnable implements UserActionRunnable {

    public static final String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "group";

    private ActionModule actionModule;

    public GroupActionRunnable(@NonNull ActionModule actionModule) {
        this.actionModule = actionModule;
    }

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        /*
         * Arguments look like:
         * {
         *   actions: [
         *     ["batch.deeplink", {"l": "https://google.com"}],
         *     ["batch.user.tag", {"add": "..."}]
         *   ]
         * }
         */

        JSONArray rawActions = args.optJSONArray("actions");
        if (rawActions == null) {
            Logger.error(ActionModule.TAG, "Could not parse group action, 'actions' is not an array");
            return;
        }

        int executedActions = 0;
        for (int i = 0; i < rawActions.length(); i++) {
            try {
                JSONArray rawAction = rawActions.getJSONArray(i);
                if (rawAction.length() == 0) {
                    Logger.error(ActionModule.TAG, "Could not parse group action item: invalid argument length");
                    continue;
                }
                String actionIdentifier = rawAction.getString(0);

                if (actionIdentifier.equalsIgnoreCase(IDENTIFIER)) {
                    Logger.error(ActionModule.TAG, "Can't trigger 'batch.group' from this action.");
                    continue;
                }

                JSONObject actionArgs = rawAction.optJSONObject(1);
                if (actionArgs == null) {
                    actionArgs = new JSONObject();
                }

                actionModule.performAction(context, actionIdentifier, actionArgs, source);
                executedActions++;
                if (executedActions >= 10) {
                    Logger.warning(ActionModule.TAG, "The group action does not support running more than 10 actions");
                    break;
                }
            } catch (JSONException e) {
                Logger.internal(ActionModule.TAG, "Could not parse group action item, JSON error");
                Logger.internal(ActionModule.TAG, "Caused by: " + e);
            }
        }
    }
}
