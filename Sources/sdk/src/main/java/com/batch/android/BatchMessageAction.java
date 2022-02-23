package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;

/**
 * Represents a BatchAction triggerable by a messaging component
 */
@PublicSDK
public class BatchMessageAction {

    private String action;

    private JSONObject args;

    /**
     * This is a private constructor
     *
     * @hide
     */
    public BatchMessageAction(@NonNull com.batch.android.messaging.model.Action from) {
        action = from.action;
        if (from.args != null) {
            try {
                args = new JSONObject(from.args);
            } catch (JSONException e) {
                args = new JSONObject();
            }
        }
    }

    @Nullable
    public String getAction() {
        return action;
    }

    @Nullable
    public JSONObject getArgs() {
        return args;
    }

    public boolean isDismissAction() {
        return action == null;
    }
}
