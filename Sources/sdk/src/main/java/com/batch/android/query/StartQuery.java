package com.batch.android.query;

import android.content.Context;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;

/**
 * A start query
 *
 */
public final class StartQuery extends Query {

    /**
     * Is this start query the consequence of user activity
     */
    private boolean userActivity;
    /**
     * Is this start query the consequence of a push opening
     */
    private boolean fromPush;
    /**
     * ID of the opening push (if {@link #fromPush} is true)
     */
    private String pushId;

    // ------------------------------------------->

    /**
     * @param context
     * @param fromPush
     */
    public StartQuery(Context context, boolean fromPush, String pushId, boolean userActivity) {
        super(context, QueryType.START);
        this.fromPush = fromPush;
        this.pushId = pushId;
        this.userActivity = userActivity;
    }

    // ------------------------------------------->

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = super.toJSON();

        obj.put("silent", !userActivity);

        obj.put("push", fromPush);
        if (fromPush && pushId != null && !pushId.isEmpty()) {
            obj.put("pushId", pushId);
        }

        return obj;
    }
}
