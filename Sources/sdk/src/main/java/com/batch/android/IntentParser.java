package com.batch.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;

/**
 * Intent parser to retrieve promo code and other data
 *
 * @hide
 */
public final class IntentParser
{
    private static final String TAG = "IntentParser";

    /**
     * Key used to add an extra to an intent to prevent it to be used more than once to show a landing
     */
    private static final String ALREADY_SHOWN_LANDING_KEY = "com.batch.messaging.push.shown";

    /**
     * Key used to remember if an intent open has been tracked already
     */
    private static final String ALREADY_TRACKED_OPEN_KEY = "com.batch.open.tracked";

    /**
     * Key used to store if an intent comes from a push notification
     */
    private static final String FROM_PUSH_KEY = "com.batch.from_push";

    /**
     * Old key used to store if an intent comes from a push notification
     * Can't really ever be removed, as many devs rely on it
     */
    private static final String FROM_PUSH_LEGACY_KEY = "fromPush";

    /**
     * Key used to store the push id linked to this open
     * Can't really ever be removed, as many devs rely on it
     */
    private static final String PUSH_ID_KEY = "com.batch.push_id";

    /**
     * Old key used to store the push id linked to this open
     * Can't really ever be removed, as many devs rely on it
     */
    private static final String PUSH_ID_LEGACY_KEY = "pushId";

// ---------------------------------------->

    /**
     * Intent to parse
     */
    private Intent intent;

    /**
     * Parsed payload
     */
    private BatchPushPayload payload = null;

// ---------------------------------------->

    /**
     * Init a parser with the intent of the activity
     *
     * @param activity
     */
    public IntentParser(Activity activity)
    {
        this(activity.getIntent());
    }

    /**
     * Init a parser with the given intent
     *
     * @param intent
     */
    public IntentParser(Intent intent)
    {
        this.intent = intent;
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                final Bundle payload = extras.getBundle(Batch.Push.PAYLOAD_KEY);

                if (payload != null) {
                    this.payload = new BatchPushPayload(payload);
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG,
                    "Unexpected error while parsing BatchPushPayload from intent",
                    e);
        }
    }

    public boolean hasPushPayload()
    {
        return this.payload != null;
    }

    /**
     * Does this intent contains a Batch notification token that tells the user did open from push
     * <p>
     * Calling this consumes the open: subsequent calls for the same intent will return false
     */
    public boolean shouldHandleOpen()
    {
        try {
            if (intent == null) {
                Logger.internal(TAG, "shouldHandleOpen : No intent found");
                return false;
            }

            /*
             * Check if already used
             */
            if (intent.getBooleanExtra(ALREADY_TRACKED_OPEN_KEY, false)) {
                Logger.internal(TAG, "shouldHandleOpen : Already tracked open");
                return false;
            }


            boolean fromPush = intent.getBooleanExtra(FROM_PUSH_KEY, false);
            if (fromPush) {
                // Set already used
                intent.putExtra(ALREADY_TRACKED_OPEN_KEY, true);
            }

            return fromPush;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while checking if open is from push", e);
            return false;
        }
    }

    /**
     * Does this intent contains a Batch Messaging landing message
     *
     * @param context
     * @return
     */
    public BatchMessage getLanding(Context context)
    {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        try {
            if (intent == null) {
                Logger.internal(TAG, "getLanding : No intent found");
                return null;
            }

            if (payload == null) {
                Logger.internal(TAG, "getLanding : No valid payload in intent");
                return null;
            }

            /*
             * Check if already used
             */
            if (intent.getBooleanExtra(ALREADY_SHOWN_LANDING_KEY, false)) {
                Logger.internal(TAG, "getLanding : Already used intent");
                return null;
            }


            BatchMessage message = payload.getLandingMessage();
            if (message != null) {
                // Set already used
                intent.putExtra(ALREADY_SHOWN_LANDING_KEY, true);
            }

            return message;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while getting the embedded landing", e);
            return null;
        }
    }

    /**
     * Retrieve the push id from the intent.<br>
     * You should use {@link #shouldHandleOpen()} before calling this method.
     *
     * @param context
     * @return
     */
    public String getPushId(Context context)
    {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        try {
            if (intent == null) {
                Logger.internal(TAG, "getPushId : No intent found");
                return null;
            }

            return intent.getStringExtra(PUSH_ID_KEY);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while retrieving push id", e);
            return null;
        }
    }

    /**
     * Retrieve the push data from the intent.<br>
     *
     * @return
     */
    public InternalPushData getPushData()
    {
        try {
            if (intent == null) {
                Logger.internal(TAG, "getPushData : No intent found");
                return null;
            }

            return payload.getInternalData();
        } catch (Exception e) {
            Logger.internal(TAG, "Error while retrieving push data", e);
            return null;
        }
    }

    public Bundle getPushBundle()
    {
        if (hasPushPayload()) {
            return this.payload.getPushBundle();
        }
        return null;
    }

    /**
     * Add push extras to an intent, to be picked up and tracked by Batch later
     */
    public static void putPushExtrasToIntent(Bundle fullPayload,
                                             InternalPushData batchData,
                                             Intent launchIntent)
    {
        launchIntent.putExtra(FROM_PUSH_KEY, true);
        launchIntent.putExtra(FROM_PUSH_LEGACY_KEY, true);
        if (fullPayload != null && !fullPayload.isEmpty()) {
            launchIntent.putExtra(Batch.Push.PAYLOAD_KEY, fullPayload);
        }

        final String pushId = batchData.getPushId();
        if (pushId != null) {
            launchIntent.putExtra(PUSH_ID_KEY, pushId);
            launchIntent.putExtra(PUSH_ID_LEGACY_KEY, pushId);
        }
    }

    /**
     * Copy all of Batch's possible internal extras from an intent to another
     */
    public static void copyExtras(@Nullable Intent from, @Nullable Intent to)
    {
        if (from == null || to == null) {
            return;
        }

        final Bundle tmpBundle = new Bundle();
        copyExtras(from.getExtras(), tmpBundle);
        to.putExtras(tmpBundle);
    }

    /**
     * Copy all of Batch's possible internal extras from a bundle
     */
    public static void copyExtras(@Nullable Bundle from, @Nullable Bundle to)
    {
        if (from == null || to == null) {
            return;
        }

        if (from.containsKey(FROM_PUSH_KEY)) {
            boolean fromPush = from.getBoolean(FROM_PUSH_KEY, false);
            to.putBoolean(FROM_PUSH_KEY, fromPush);
            to.putBoolean(FROM_PUSH_LEGACY_KEY, fromPush);
        }

        if (from.containsKey(PUSH_ID_KEY)) {
            String pushId = from.getString(PUSH_ID_KEY, null);
            if (pushId != null) {
                to.putString(PUSH_ID_KEY, pushId);
                to.putString(PUSH_ID_LEGACY_KEY, pushId);
            }
        }

        if (from.containsKey(Batch.Push.PAYLOAD_KEY)) {
            Bundle payload = from.getBundle(Batch.Push.PAYLOAD_KEY);
            if (payload != null) {
                to.putBundle(Batch.Push.PAYLOAD_KEY, payload);
            }
        }

        if (from.containsKey(ALREADY_TRACKED_OPEN_KEY)) {
            to.putBoolean(ALREADY_TRACKED_OPEN_KEY,
                    from.getBoolean(ALREADY_TRACKED_OPEN_KEY, false));
        }

        if (from.containsKey(ALREADY_SHOWN_LANDING_KEY)) {
            to.putBoolean(ALREADY_SHOWN_LANDING_KEY,
                    from.getBoolean(ALREADY_SHOWN_LANDING_KEY, false));
        }
    }
}
