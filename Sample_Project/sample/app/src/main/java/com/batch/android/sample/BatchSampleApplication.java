/*
 * Copyright (c) 2015 Batch.com. All rights reserved.
 */

package com.batch.android.sample;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.batch.android.Batch;
import com.batch.android.Config;
import com.batch.android.PushNotificationType;

import java.util.EnumSet;

/**
 * Batch Sample application subclass
 */
public class BatchSampleApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener
{

    private static final String TAG = "BatchSampleApplication";

    private static final String PREF_NOTIF_ALERT = "notifications_alert";
    private static final String PREF_NOTIF_SOUND = "notifications_sound";
    private static final String PREF_NOTIF_VIBRATE = "notifications_vibrate";
    private static final String PREF_NOTIF_LIGHTS = "notifications_lights";

    @Override
    public void onCreate()
    {
        super.onCreate();

        PreferenceManager.setDefaultValues(this, R.xml.settings_notification, false);

        // Subscribe to notification changes so that changes in NotificationSettingsActivity
        // are automatically reflected in Batch immediately
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        // In order to enable push, uncomment this and set your GCM sender ID
        //Batch.Push.setGCMSenderId("YOUR-GCM-SENDER-ID");
        Batch.Push.setSmallIconResourceId(R.drawable.ic_notification_icon);

        Batch.setConfig(new Config("YOUR_API_KEY"));

        // This app takes advantage of Ads' manual load system
        Batch.Ads.setAutoLoad(false);

        updateNotificationSettings();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        updateNotificationSettings();
    }

    private void updateNotificationSettings()
    {
        Log.i(TAG, "Updating Batch notification settings");

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        EnumSet<PushNotificationType> pushNotificationTypes = EnumSet.allOf(PushNotificationType.class);

        if (!prefs.getBoolean(PREF_NOTIF_ALERT, true))
        {
            // Removing ALERT is enough, no sound or vibration will be emitted.
            pushNotificationTypes.remove(PushNotificationType.ALERT);
        }
        if (!prefs.getBoolean(PREF_NOTIF_SOUND, true))
        {
            pushNotificationTypes.remove(PushNotificationType.SOUND);
        }
        if (!prefs.getBoolean(PREF_NOTIF_VIBRATE, true))
        {
            pushNotificationTypes.remove(PushNotificationType.VIBRATE);
        }
        if (!prefs.getBoolean(PREF_NOTIF_LIGHTS, true))
        {
            pushNotificationTypes.remove(PushNotificationType.LIGHTS);
        }

        Batch.Push.setNotificationsType(pushNotificationTypes);
    }
}
