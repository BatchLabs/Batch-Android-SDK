/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.batch.android.Feature;
import com.batch.android.Offer;
import com.batch.android.Resource;

import java.util.List;
import java.util.Map;

/**
 * Manager responsible for redeeming and storing unlocked features and items
 */
public class UnlockManager
{
    private static final String TAG = "UnlockManager";

    /**
     * Keys, Default Values, and Batch Item references for the supported unlockables
     */
    private static final String NO_ADS_KEY = "no_ads";
    private static final boolean NO_ADS_DEFAULT_VALUE = false;
    private static final String NO_ADS_REFERENCE = "NO_ADS";

    private static final String LIVES_KEY = "lives";
    private static final long LIVES_DEFAULT_VALUE = 10l;
    private static final String LIVES_REFERENCE = "LIVES";

    private static final String PRO_TRIAL_KEY = "pro_trial";
    private static final long PRO_TRIAL_DEFAULT_VALUE = 0l;
    private static final String PRO_TRIAL_REFERENCE = "PRO_TRIAL";

    /**
     * Intent action signifying that the unlocks may have changed
     */
    public static final String ACTION_UNLOCKS_CHANGED = "com.batch.android.sample.UNLOCKS_CHANGED";

    /**
     * Constant for unlimited Pro Trial duration
     */
    public static final long PRO_TRIAL_UNLIMITED = -1;

    private static UnlockManager instance;

    /**
     * Gets the static instance of the UnlockManager, created on demand.
     * @param context Context, can be your application's context.
     * @return The static shared UnlockManager instance
     */
    public static UnlockManager getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new UnlockManager(context);
        }

        return instance;
    }

    private Context applicationContext;

    private SharedPreferences defaultSharedPrefs;

    /**
     * Instantiates a new UnlockManager. Use UnlockManager.getInstance instead.
     * @param context Your application's context.
     */
    public UnlockManager(Context context)
    {
        applicationContext = context.getApplicationContext();
        defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    /**
     * Show a successful redeem alert for an offer, using the custom parameters
     * @param context Context used for building the AlertDialog
     * @param offer Offer for which the message should be displayed
     */
    public void showRedeemAlert(Context context, Offer offer)
    {
        final Map<String, String> additionalParameters = offer.getOfferAdditionalParameters();
        if (additionalParameters.containsKey("reward_message"))
        {
            Log.i(TAG, "Displaying 'reward_message' additional parameter");
            String rewardMessage = additionalParameters.get("reward_message");

            // Build the Dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(rewardMessage).setTitle(R.string.unlock_auto_popup_title);
            builder.setPositiveButton(R.string.thanks, null);

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else
        {
            Log.e(TAG, "Didn't find an additional parameter named 'reward_message' to display a reward confirmation message");
        }
    }

    /**
     * Unlocks all items from an offer
     * @param offer Offer to read unlocks from
     */
    public void unlockItems(Offer offer)
    {
        if (offer.hasFeatures())
        {
            unlockFeatures(offer.getFeatures());
        }

        for (Resource resource : offer.getResources())
        {
            if (LIVES_REFERENCE.equalsIgnoreCase(resource.getReference()))
            {
                Log.i(TAG, "Unlocking " + resource.getQuantity() + " " + LIVES_REFERENCE);
                setLivesCount(getLivesCount() + resource.getQuantity());
            }
        }

        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(new Intent(ACTION_UNLOCKS_CHANGED));
    }

    /**
     * Unlock the specified features to the user. Used for Batch Unlock's restore.
     * Note : The ACTION_UNLOCKS_CHANGED intent will be locally broadcast on completion
     * @param features Features to unlock
     */
    public void unlockFeatures(List<Feature> features)
    {
        unlockFeatures(features, true);
    }

    /**
     * Unlock the specified features to the user. Used for Batch Unlock's restore.
     * @param features Features to unlock
     * @param broadcastChanges Whether to broadcast an Intent telling that unlocks changed
     */
    private void unlockFeatures(List<Feature> features, boolean broadcastChanges)
    {
        for (Feature feature : features)
        {
            if (NO_ADS_REFERENCE.equalsIgnoreCase(feature.getReference()))
            {
                Log.i(TAG, "Unlocking " + NO_ADS_REFERENCE);
                writeBool(NO_ADS_KEY, true);
            }
            else if (PRO_TRIAL_REFERENCE.equalsIgnoreCase(feature.getReference()))
            {
                if (feature.isLifetime())
                {
                    Log.i(TAG, "Unlocking " + PRO_TRIAL_REFERENCE + "forever");
                    writeLong(PRO_TRIAL_KEY, PRO_TRIAL_UNLIMITED);
                }
                else
                {
                    Log.i(TAG, "Unlocking " + PRO_TRIAL_REFERENCE + "for " + feature.getTTL() + " seconds");
                    // Store the timestamp of expiration rather than the TTL
                    writeLong(PRO_TRIAL_KEY, (System.currentTimeMillis() / 1000) + feature.getTTL());
                }
            }
        }

        if (broadcastChanges)
        {
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(new Intent(ACTION_UNLOCKS_CHANGED));
        }
    }

    /**
     * @return Number of lives left
     */
    public long getLivesCount()
    {
        return readLong(LIVES_KEY, LIVES_DEFAULT_VALUE);
    }

    /**
     * Set the number of lives left
     * @param count Lives left
     */
    public void setLivesCount(long count)
    {
        writeLong(LIVES_KEY, count);
    }

    /**
     * @return Whether the user unlocked the "no ads" feature
     */
    public boolean hadNoAds()
    {
        return readBool(NO_ADS_KEY, NO_ADS_DEFAULT_VALUE);
    }

    /**
     * @return Whether the user currently has a pro trial active
     */
    public boolean hasProTrial()
    {
        final long timeLeft = timeLeftForProTrial();
        return timeLeft != 0 && timeLeft != PRO_TRIAL_UNLIMITED;
    }

    /**
     * @return Time left for the pro trial in seconds (or PRO_TRIAL_UNLIMITED if unlimited)
     */
    public long timeLeftForProTrial()
    {
        long expirationDate = readLong(PRO_TRIAL_KEY, PRO_TRIAL_DEFAULT_VALUE);
        if (expirationDate == PRO_TRIAL_UNLIMITED)
        {
            return PRO_TRIAL_UNLIMITED;
        }
        return Math.max(expirationDate - (System.currentTimeMillis() / 1000), 0);
    }

    /**
     * Storage helper methods
     */

    private long readLong(String key, long defaultValue)
    {
        return defaultSharedPrefs.getLong(key, defaultValue);
    }

    private void writeLong(String key, long value)
    {
        defaultSharedPrefs.edit().putLong(key, value).apply();
    }

    private boolean readBool(String key, boolean defaultValue)
    {
        return defaultSharedPrefs.getBoolean(key, defaultValue);
    }

    private void writeBool(String key, boolean value)
    {
        defaultSharedPrefs.edit().putBoolean(key, value).apply();
    }
}
