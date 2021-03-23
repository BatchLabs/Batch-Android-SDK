package com.batch.android;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.batch.android.core.FixedSizeArrayList;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.ObjectUserPreferencesStorageProvider;
import com.batch.android.module.PushModule;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Set of helper methods shared between {@link BatchPushNotificationPresenter} and methods exposed to
 * devs via the push module
 *
 * @hide
 */
public class BatchPushHelper
{
    private static final String ALREADY_SHOWN_IDS_STORAGE_KEY = "push_already_shown";
    private static final int ALREADY_SHOWN_IDS_SIZE = 20;

    // We also keep push id in RAM in case several pushs are received simultaneously
    private static final ArrayList<String> beingShownIds = new ArrayList<>(ALREADY_SHOWN_IDS_SIZE);

    /**
     * Internal method
     */
    public static synchronized boolean isPushValid(Context context, InternalPushData batchData)
    {
        /*
         * Check push ID
         */
        String pushId = batchData.getPushId();
        if (pushId != null && beingShownIds.contains(pushId)) {
            Logger.warning(PushModule.TAG, "Already shown notification[" + pushId + "], aborting");
            return false;
        } else if (pushId != null) {
            beingShownIds.add(pushId);
        }

        /*
         * Check install ID
         */
        String installId = batchData.getInstallId();
        if (installId != null && !installIDMatchesCurrent(context, installId)) {
            Logger.warning(PushModule.TAG,
                    "Received notification[" + pushId + "] for another install id[" + installId + "], aborting");
            return false;
        }

        return true;
    }

    /**
     * Internal method
     */
    public static synchronized void markPushAsShown(Context context, String pushId)
    {
        if (pushId == null) {
            return;
        }

        FixedSizeArrayList<String> alreadyShownIds = getShownPushIds(context);
        alreadyShownIds.add(pushId);
        if (!ObjectUserPreferencesStorageProvider.get(context).persist(
                ALREADY_SHOWN_IDS_STORAGE_KEY,
                alreadyShownIds))
        {
            Logger.internal(PushModule.TAG, "Error while saving already shown push ids");
        }

        // We remove all occurrences of the id in RAM
        BatchPushHelper.beingShownIds.removeAll(Collections.singletonList(pushId));
    }

    /**
     * Convert a Firebase RemoteMessage to a bundle, for compatibility with all the methods that used
     * to deal with a BroadcastReceiver started by GCM
     */
    @Nullable
    public static Bundle firebaseMessageToReceiverBundle(@Nullable RemoteMessage message)
    {
        if (message == null) {
            return null;
        }

        Map<String, String> data = message.getData();
        if (data == null || data.size() == 0) {
            return null;
        }

        Bundle retVal = new Bundle();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            retVal.putString(entry.getKey(), entry.getValue());
        }
        return retVal;
    }

    /**
     * Check if the push has been shown
     *
     * @param context
     * @param pushId
     * @return true if the push has already been shown
     */
    static boolean isPushAlreadyShown(Context context, String pushId)
    {
        return getShownPushIds(context).contains(pushId);
    }

    /**
     * Get shown push ids array
     *
     * @param context
     * @return
     */
    @SuppressWarnings("unchecked")
    private static FixedSizeArrayList<String> getShownPushIds(Context context)
    {
        FixedSizeArrayList<String> alreadyShownIds = null;
        try {
            Object storedIds = ObjectUserPreferencesStorageProvider.get(context).get(
                    ALREADY_SHOWN_IDS_STORAGE_KEY);
            if (storedIds != null) {
                alreadyShownIds = (FixedSizeArrayList<String>) storedIds;
            }
        } catch (Exception e) {
            Logger.internal(PushModule.TAG, "Error while reading stored ids", e);
        }

        // If nothing found, just create a new array
        if (alreadyShownIds == null) {
            alreadyShownIds = new FixedSizeArrayList<>(ALREADY_SHOWN_IDS_SIZE);
        }

        return alreadyShownIds;
    }

    /**
     * Is the given install id mine
     *
     * @param installId
     * @return
     */
    private static boolean installIDMatchesCurrent(Context context, String installId)
    {
        String currentInstallId = new Install(context).getInstallID();
        return installId.equals(currentInstallId);
    }
}
