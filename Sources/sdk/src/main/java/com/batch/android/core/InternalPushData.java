package com.batch.android.core;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.batch.android.BatchNotificationAction;
import com.batch.android.BatchNotificationSource;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONHelper;
import com.batch.android.json.JSONObject;
import com.batch.android.module.PushModule;
import com.google.firebase.messaging.RemoteMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class to easily access the push data
 *
 */
public class InternalPushData {

    /**
     * Key to retrieve a custom scheme to launch (optional)
     */
    private static final String SCHEME_KEY = "l";
    /**
     * Key to retrieve the ID key (= id of the push)
     */
    private static final String ID_KEY = "i";
    /**
     * Key to retreive the local session
     */
    private static final String INSTALL_ID_KEY = "di";
    /**
     * Key to retrieve the silent boolean (if true, Batch shouldn't handle the notification)
     */
    private static final String IS_SILENT_KEY = "s";
    /**
     * Key to retrieve the local campaigns refresh key
     * (if true, it's silent instruction, we will refresh local campaigns)
     */
    private static final String IS_LOCAL_CAMPAIGNS_REFRESH_KEY = "lcr";
    /**
     * Key to retrieve the landing payload
     */
    private static final String LANDING_KEY = "ld";
    /**
     * Key to retrieve the big icon key
     */
    private static final String CUSTOM_BIG_ICON_KEY = "bi";
    /**
     * Key to retrieve the big image key
     */
    private static final String CUSTOM_BIG_IMAGE_KEY = "bp";
    /**
     * Key to retrieve the actions
     */
    private static final String ACTION_KEY = "a";
    /**
     * Key to retrieve the priority
     */
    private static final String PRIORITY_KEY = "pr";

    /**
     * Key to retrieve the group name
     */
    private static final String GROUP_NAME_KEY = "gr";

    /**
     * Key to retrieve if the notification is a group summary
     */
    private static final String IS_GROUP_SUMMARY_KEY = "grs";

    /**
     * Key to retrieve the additional open data
     */
    private static final String OPEN_DATA_KEY = "od";

    /**
     * Key to retrieve the push type
     */
    private static final String TYPE_KEY = "t";

    /**
     * Key to retrieve the experiment id
     */
    private static final String EXPERIMENT_KEY = "ex";

    /**
     * Key to retrieve the variant id
     */
    private static final String VARIANT_KEY = "va";

    /**
     * Key to retrieve the wanted channel
     */
    private static final String CHANNEL_KEY = "ch";

    /**
     * Key to retrieve the wanted channel
     */
    private static final String VISIBILITY_KEY = "vis";

    /**
     * Key to retrieve the notification format
     * Formats are similar to Messaging formats
     */
    private static final String FORMAT_KEY = "fmt";

    /**
     * Key to retrieve the notification format arguments
     */
    private static final String FORMAT_ARGS_KEY = "fmt_args";

    /**
     * Key to retrieve the receipt parameters
     */
    private static final String RECEIPT_KEY = "r";

    /**
     * Key to ask for the old custom icon behaviour in BigPictureStyle
     */
    private static final String OLD_BIG_PICTURE_ICON_BEHAVIOUR = "old_bp_icnhandling";

    /**
     * Key this data is contained in when stored in a push bundle
     */
    public static final String BATCH_BUNDLE_KEY = "com.batch";

    private String jsonPayload;
    private JSONObject payload;

    public InternalPushData(String batchData) {
        if (batchData == null || batchData.isEmpty()) {
            throw new NullPointerException("Cannot init PushData without the associated JSON data");
        }

        try {
            jsonPayload = batchData;
            payload = new JSONObject(batchData);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Error while parsing JSON data", e);
        }
    }

    public InternalPushData(JSONObject batchData) {
        if (batchData == null || batchData.keySet().isEmpty()) {
            throw new NullPointerException("Cannot init PushData without the associated JSON data");
        }

        jsonPayload = batchData.toString();
        payload = batchData;
    }

    public static InternalPushData getPushDataForReceiverIntent(@NonNull Intent intent) {
        //noinspection ConstantConditions
        if (intent == null) {
            throw new IllegalArgumentException("intent cannot be null");
        }

        return getPushDataForReceiverBundle(intent.getExtras());
    }

    public static InternalPushData getPushDataForReceiverBundle(@Nullable Bundle extras) {
        if (extras != null && !extras.isEmpty()) {
            String batchData = extras.getString(BATCH_BUNDLE_KEY);
            if (batchData != null) {
                return new InternalPushData(batchData);
            }
        }

        return null;
    }

    public static InternalPushData getPushDataForFirebaseMessage(@Nullable RemoteMessage message) {
        if (message == null) {
            return null;
        }

        final Map<String, String> data = message.getData();
        if (data != null && data.size() > 0) {
            String batchData = data.get(BATCH_BUNDLE_KEY);
            if (batchData != null) {
                return new InternalPushData(batchData);
            }
        }

        return null;
    }

    public String getJsonPayload() {
        return jsonPayload;
    }

    // --------------------------------------->

    public boolean isSilent() {
        try {
            return payload.has(IS_SILENT_KEY) && payload.getBoolean(IS_SILENT_KEY);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean isLocalCampainsRefresh() {
        try {
            return payload.has(IS_LOCAL_CAMPAIGNS_REFRESH_KEY) && payload.getBoolean(IS_LOCAL_CAMPAIGNS_REFRESH_KEY);
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean hasScheme() {
        return payload.has(SCHEME_KEY) && !payload.isNull(SCHEME_KEY);
    }

    public boolean isSchemeEmpty() {
        String scheme = nullSafeGetString(SCHEME_KEY);
        return scheme == null || scheme.trim().isEmpty();
    }

    public String getScheme() {
        return nullSafeGetString(SCHEME_KEY);
    }

    public String getPushId() {
        return nullSafeGetString(ID_KEY);
    }

    public String getInstallId() {
        return nullSafeGetString(INSTALL_ID_KEY);
    }

    public boolean hasLandingMessage() {
        return nullSafeGetJSONObject(LANDING_KEY) != null;
    }

    public JSONObject getLandingMessage() {
        return nullSafeGetJSONObject(LANDING_KEY);
    }

    public boolean hasCustomBigIcon() {
        JSONObject bigIconObject = nullSafeGetJSONObject(CUSTOM_BIG_ICON_KEY);
        if (bigIconObject == null) {
            return false;
        }

        String url = nullSafeGetString(bigIconObject, "u");

        return url != null && !url.trim().isEmpty();
    }

    public String getCustomBigIconURL() {
        JSONObject bigIconObject = nullSafeGetJSONObject(CUSTOM_BIG_ICON_KEY);
        if (bigIconObject == null) {
            return null;
        }

        return nullSafeGetString(bigIconObject, "u");
    }

    public List<Double> getCustomBigIconAvailableDensity() {
        JSONObject bigIconObject = nullSafeGetJSONObject(CUSTOM_BIG_ICON_KEY);
        if (bigIconObject == null) {
            return null;
        }

        if (!bigIconObject.has("d") || bigIconObject.isNull("d")) {
            return null;
        }

        try {
            List<Double> result = new ArrayList<>();

            JSONArray densities = bigIconObject.getJSONArray("d");
            for (int i = 0; i < densities.length(); i++) {
                result.add(densities.getDouble(i));
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasCustomBigImage() {
        JSONObject bigIconImage = nullSafeGetJSONObject(CUSTOM_BIG_IMAGE_KEY);
        if (bigIconImage == null) {
            return false;
        }

        String url = nullSafeGetString(bigIconImage, "u");

        return url != null && !url.trim().isEmpty();
    }

    public String getCustomBigImageURL() {
        JSONObject bigIconImage = nullSafeGetJSONObject(CUSTOM_BIG_IMAGE_KEY);
        if (bigIconImage == null) {
            return null;
        }

        return nullSafeGetString(bigIconImage, "u");
    }

    public List<Double> getCustomBigImageAvailableDensity() {
        JSONObject bigIconImage = nullSafeGetJSONObject(CUSTOM_BIG_IMAGE_KEY);
        if (bigIconImage == null) {
            return null;
        }

        if (!bigIconImage.has("d") || bigIconImage.isNull("d")) {
            return null;
        }

        try {
            List<Double> result = new ArrayList<>();

            JSONArray densities = bigIconImage.getJSONArray("d");
            for (int i = 0; i < densities.length(); i++) {
                result.add(densities.getDouble(i));
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public List<BatchNotificationAction> getActions() {
        final List<BatchNotificationAction> retVal = new ArrayList<>();
        JSONArray actions = nullSafeGetJSONArray(ACTION_KEY);

        if (actions != null) {
            for (int i = 0; i < actions.length(); i++) {
                final JSONObject jsonAction = actions.optJSONObject(i);
                if (jsonAction == null) {
                    Logger.internal(
                        PushModule.TAG,
                        "InternalPushData - getActions: Invalid action json array object. Skipping."
                    );
                    continue;
                }
                final BatchNotificationAction tmpAction = new BatchNotificationAction();

                tmpAction.label = jsonAction.reallyOptString("l", null);
                tmpAction.drawableName = jsonAction.reallyOptString("i", null);
                tmpAction.hasUserInterface = jsonAction.reallyOptBoolean("ui", true);
                tmpAction.actionIdentifier = jsonAction.reallyOptString("a", null);
                tmpAction.actionArguments = jsonAction.optJSONObject("args");
                tmpAction.shouldDismissNotification = jsonAction.reallyOptBoolean("d", true);
                if (tmpAction.actionArguments == null) {
                    tmpAction.actionArguments = new JSONObject();
                }

                if (TextUtils.isEmpty(tmpAction.label)) {
                    Logger.internal(PushModule.TAG, "InternalPushData - getActions: Empty or null label. Skipping.");
                    continue;
                }

                if (TextUtils.isEmpty(tmpAction.actionIdentifier)) {
                    Logger.internal(
                        PushModule.TAG,
                        "InternalPushData - getActions: Empty or null action identifier. Skipping."
                    );
                    continue;
                }

                retVal.add(tmpAction);
            }
        }

        return retVal;
    }

    public ReceiptMode getReceiptMode() {
        JSONObject receipt = nullSafeGetJSONObject(RECEIPT_KEY);
        if (receipt == null) {
            return ReceiptMode.DEFAULT;
        }

        switch (receipt.optInt("m", 1)) {
            case 1:
                return ReceiptMode.DISPLAY;
            case 2:
                return ReceiptMode.FORCE;
            case 0:
            default:
                return ReceiptMode.DEFAULT;
        }
    }

    public long getReceiptMinDelay() {
        JSONObject receipt = nullSafeGetJSONObject(RECEIPT_KEY);
        if (receipt == null) {
            return 0;
        }

        return receipt.optLong("dmi", 0);
    }

    public long getReceiptMaxDelay() {
        JSONObject receipt = nullSafeGetJSONObject(RECEIPT_KEY);
        if (receipt == null) {
            return 0;
        }

        return receipt.optLong("dma", 0);
    }

    public Priority getPriority() {
        String priority = nullSafeGetString(PRIORITY_KEY);
        if (!TextUtils.isEmpty(priority)) {
            priority = priority.toLowerCase(Locale.US);
            try {
                int i = Integer.parseInt(priority);
                switch (i) {
                    default:
                    case 0:
                        return Priority.DEFAULT;
                    case -2:
                        return Priority.MIN;
                    case -1:
                        return Priority.LOW;
                    case 1:
                        return Priority.HIGH;
                    case 2:
                        return Priority.MAX;
                }
            } catch (NumberFormatException e) {
                Logger.internal(PushModule.TAG, "Error while reading the priority number " + priority, e);
            }
        }

        return Priority.UNDEFINED;
    }

    /**
     * Returns the channel for Android 8.0
     */
    public String getChannel() {
        return nullSafeGetString(CHANNEL_KEY);
    }

    public BatchNotificationSource getSource() {
        String source = nullSafeGetString(TYPE_KEY);

        if ("c".equalsIgnoreCase(source)) {
            return BatchNotificationSource.CAMPAIGN;
        } else if ("t".equalsIgnoreCase(source)) {
            return BatchNotificationSource.TRANSACTIONAL;
        } else if ("tc".equalsIgnoreCase(source)) {
            return BatchNotificationSource.TRIGGER;
        }
        return BatchNotificationSource.UNKNOWN;
    }

    /**
     * Get the notification group name. Meant to be used with {@link androidx.core.app.NotificationCompat.Builder#setGroup(String)}
     *
     * @return Group name string, null if none
     */
    public String getGroup() {
        String group = nullSafeGetString(GROUP_NAME_KEY);
        if (TextUtils.isEmpty(group)) {
            return null;
        }
        return group;
    }

    /**
     * Get the notification group name. Meant to be used with {@link androidx.core.app.NotificationCompat.Builder#setGroupSummary(boolean)}
     *
     * @return Whether this notification should be a group summary or not
     */
    public boolean isGroupSummary() {
        return payload.optBoolean(IS_GROUP_SUMMARY_KEY, false);
    }

    /**
     * Get the notification's visibility.
     * <p>
     * See {@link NotificationCompat.Builder#setVisibility(int)}
     *
     * @return The wanted visibility. Default is {@link Notification#VISIBILITY_PUBLIC}
     */
    @SuppressLint("InlinedApi")
    public int getVisibility() {
        switch (payload.optInt(VISIBILITY_KEY, 1)) {
            case 0:
                return Notification.VISIBILITY_PRIVATE;
            case -1:
                return Notification.VISIBILITY_SECRET;
            case 1:
            default:
                return Notification.VISIBILITY_PUBLIC;
        }
    }

    /**
     * Get the notification format
     * A format is like a Messaging message format
     */
    public Format getNotificationFormat() {
        return Format.fromString(payload.reallyOptString(FORMAT_KEY, null));
    }

    /**
     * Get the notification format arguments
     */
    @Nullable
    public JSONObject getNotificationFormatArguments() {
        return payload.optJSONObject(FORMAT_ARGS_KEY);
    }

    public Map<String, Object> getExtraParameters() {
        Map<String, Object> parameters;
        try {
            final JSONObject trimmedPayload = new JSONObject(
                payload,
                new String[] { ID_KEY, OPEN_DATA_KEY, EXPERIMENT_KEY, VARIANT_KEY, TYPE_KEY }
            );
            parameters = JSONHelper.jsonObjectToMap(trimmedPayload);
            return parameters;
        } catch (JSONException e) {
            Logger.internal(PushModule.TAG, "Error while deserializing the PushData extra parameters.", e);
        }

        return null;
    }

    /**
     * Get the open data as map
     *
     * @return
     */
    public Map<String, Object> getOpenData() {
        try {
            final JSONObject trimmedPayload = new JSONObject(payload, new String[] { OPEN_DATA_KEY });
            return JSONHelper.jsonObjectToMap(trimmedPayload);
        } catch (JSONException e) {
            Logger.internal(PushModule.TAG, "Error while deserializing the PushData open data.", e);
        }

        return null;
    }

    /**
     * Get the event data for the display receipt
     *
     * @return
     */
    public Map<String, Object> getReceiptEventData() {
        try {
            final JSONObject trimmedPayload = new JSONObject(
                payload,
                new String[] { ID_KEY, EXPERIMENT_KEY, VARIANT_KEY }
            );
            return JSONHelper.jsonObjectToMap(trimmedPayload);
        } catch (JSONException e) {
            Logger.internal(PushModule.TAG, "Error while deserializing the receipt event data.", e);
        }

        return null;
    }

    /**
     * Returns whether the old custom icon fallback behaviour for BigPictureStyle
     * (which changed in 1.14.1) should be used or not.
     * <p>
     * Default false
     */
    public boolean shouldUseLegacyBigPictureIconBehaviour() {
        return payload.reallyOptBoolean(OLD_BIG_PICTURE_ICON_BEHAVIOUR, false);
    }

    private String nullSafeGetString(String key) {
        return nullSafeGetString(payload, key);
    }

    private String nullSafeGetString(JSONObject source, String key) {
        try {
            return source.isNull(key) ? null : source.getString(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject nullSafeGetJSONObject(String key) {
        try {
            return payload.isNull(key) ? null : payload.getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray nullSafeGetJSONArray(String key) {
        try {
            return payload.isNull(key) ? null : payload.getJSONArray(key);
        } catch (JSONException e) {
            return null;
        }
    }

    // --------------------------------------->

    // Android priority abstraction for future proofing
    public enum Priority {
        UNDEFINED,
        DEFAULT,
        MIN,
        LOW,
        HIGH,
        MAX;

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public int toAndroidPriority() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                return 0;
            }

            switch (this) {
                case UNDEFINED:
                case DEFAULT:
                default:
                    return Notification.PRIORITY_DEFAULT;
                case MIN:
                    return Notification.PRIORITY_MIN;
                case LOW:
                    return Notification.PRIORITY_LOW;
                case HIGH:
                    return Notification.PRIORITY_HIGH;
                case MAX:
                    return Notification.PRIORITY_MAX;
            }
        }

        public int toSupportPriority() {
            switch (this) {
                case UNDEFINED:
                case DEFAULT:
                default:
                    return NotificationCompat.PRIORITY_DEFAULT;
                case MIN:
                    return NotificationCompat.PRIORITY_MIN;
                case LOW:
                    return NotificationCompat.PRIORITY_LOW;
                case HIGH:
                    return NotificationCompat.PRIORITY_HIGH;
                case MAX:
                    return NotificationCompat.PRIORITY_MAX;
            }
        }
    }

    public enum Format {
        DEFAULT,
        // Android Push Expanded Notification
        APEN;

        public static Format fromString(@Nullable String fmtString) {
            if ("apen".equals(fmtString)) {
                return Format.APEN;
            }
            return Format.DEFAULT;
        }
    }

    public enum ReceiptMode {
        DEFAULT,
        DISPLAY,
        FORCE,
    }
}
