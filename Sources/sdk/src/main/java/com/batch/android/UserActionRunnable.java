package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONObject;

/**
 * Represents an {@link UserAction}'s runnable. Similar to {@link Runnable}, but with specific contextual arguments.
 */
@PublicSDK
public interface UserActionRunnable {
    /**
     * Perform the requested action defined by the given parameters.
     * <br/>
     * Note: This can be run from any thread. Do not make assumptions about the thread you're currently on.
     *
     * @param context    The current context, if applicable. Be careful, as this may be any context, and not just an activity one. It can also be null.
     * @param identifier The action identifier.
     * @param args       Action arguments. Can be empty.
     * @param source     The action source. Used to get more info about the what triggered the action (for example, the full payload of the push that triggered this action). In some cases, this can be null.
     */
    void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    );
}
