package com.batch.android.actions;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;

/**
 * Action that copy a text in the device's clipboard
 */
public class ClipboardActionRunnable implements UserActionRunnable {

    private static final String TAG = "ClipboardBuiltinAction";
    public static final String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "clipboard";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        try {
            String text = args.getString("t");
            if (text == null) {
                Logger.internal(TAG, "Could not perform clipboard action : text's null");
                return;
            }
            String description = args.optString("d", "text");

            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(description, text);
            clipboard.setPrimaryClip(clip);
        } catch (JSONException e) {
            Logger.internal(TAG, "Json object failure : " + e.getLocalizedMessage());
        }
    }
}
