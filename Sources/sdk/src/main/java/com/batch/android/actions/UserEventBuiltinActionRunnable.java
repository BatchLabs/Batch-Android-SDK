package com.batch.android.actions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchEventData;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserEventBuiltinActionRunnable implements UserActionRunnable {

    private static final String TAG = "EventBuiltinActionRunnable";
    public static String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "user.event";

    private Date parseDate(String date) {
        DateFormat isoFormat = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
        } else {
            isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            // Java 6 doesn't parse dates with timezones well
            date = date.replaceAll("Z$", "+0000");
        }
        try {
            return isoFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        try {
            JSONObject json = new JSONObject(args);

            String event = json.getString("e");
            if (event == null) {
                Logger.internal(TAG, "Could not perform event action : event's null");
                return;
            }

            if (event.length() == 0) {
                Logger.internal(TAG, "Could not perform event action : event name is empty");
                return;
            }

            String label = json.reallyOptString("l", null);

            BatchEventData data = new BatchEventData();

            JSONArray tags = json.optJSONArray("t");
            if (tags != null && tags.length() > 0) {
                for (int i = 0; i < tags.length(); i++) {
                    String tag = tags.optString(i, null);
                    if (tag != null && tag.length() > 0) {
                        data.addTag(tag);
                    } else {
                        Logger.internal(TAG, "Could not add tag in event action : tag value is null or invalid");
                    }
                }
            }

            JSONObject argsData = json.optJSONObject("a");
            if (argsData != null && argsData.keySet().size() > 0) {
                for (String key : argsData.keySet()) {
                    Object toAdd = argsData.opt(key);
                    if (toAdd instanceof Integer) {
                        data.put(key, (Integer) toAdd);
                    } else if (toAdd instanceof Long) {
                        data.put(key, (Long) toAdd);
                    } else if (toAdd instanceof Float) {
                        data.put(key, (Float) toAdd);
                    } else if (toAdd instanceof Double) {
                        data.put(key, (Double) toAdd);
                    } else if (toAdd instanceof String) {
                        Date dateValue = parseDate((String) toAdd);
                        if (dateValue != null) {
                            data.put(key, dateValue);
                        } else {
                            data.put(key, (String) toAdd);
                        }
                    } else if (toAdd instanceof Boolean) {
                        data.put(key, (Boolean) toAdd);
                    } else {
                        Logger.internal(TAG, "Could not add data in event action : value type is invalid");
                    }
                }
            }

            Batch.User.trackEvent(event, label, data);
        } catch (JSONException e) {
            Logger.internal(TAG, "Json object failure : " + e.getLocalizedMessage());
        }
    }
}
