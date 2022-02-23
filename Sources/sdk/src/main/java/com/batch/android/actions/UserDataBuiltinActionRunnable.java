package com.batch.android.actions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchUserDataEditor;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import java.util.Locale;

public class UserDataBuiltinActionRunnable implements UserActionRunnable {

    private static final String TAG = "UserDataBuiltinAction";
    public static String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "user.tag";

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        try {
            JSONObject json = new JSONObject(args);

            String collection = json.getString("c");
            if (collection == null) {
                Logger.internal(TAG, "Could not perform tag edit action : collection's null");
                return;
            }

            if (collection.length() == 0) {
                Logger.internal(TAG, "Could not perform tag edit action : collection name is empty");
                return;
            }

            String tag = json.getString("t");
            if (tag == null) {
                Logger.internal(TAG, "Could not perform tag edit action : tag's null");
                return;
            }

            if (tag.length() == 0) {
                Logger.internal(TAG, "Could not perform tag edit action : tag name is empty");
                return;
            }

            String action = json.getString("a");
            if (action == null) {
                Logger.internal(TAG, "Could not perform tag edit action : action's null");
                return;
            }

            action = action.toLowerCase(Locale.US);

            if (action.equals("add")) {
                Logger.internal(TAG, "Adding tag " + tag + " to collection " + collection);
                BatchUserDataEditor editor = Batch.User.editor();
                editor.addTag(collection, tag);
                editor.save();
            } else if (action.equals("remove")) {
                Logger.internal(TAG, "Removing tag " + tag + " to collection " + collection);
                BatchUserDataEditor editor = Batch.User.editor();
                editor.removeTag(collection, tag);
                editor.save();
            } else {
                Logger.internal(TAG, "Could not perform tag edit action: Unknown action");
            }
        } catch (JSONException e) {
            Logger.internal(TAG, "Json object failure : " + e.getLocalizedMessage());
        }
    }
}
