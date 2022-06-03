package com.batch.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.compat.WakefulBroadcastReceiver;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.Logger;
import java.util.ArrayDeque;

/**
 * Batch's implementation of FCM's Push BroadcastReceiver
 */
@PublicSDK
public class BatchPushMessageReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "BatchPushMessageReceiver";

    @VisibleForTesting
    static final int MAX_HANDLED_MESSAGE_IDS_COUNT = 30;

    // Keep a record of the last FCM message IDs we've handled, as it looks like FCM can send
    // duplicate pushes.
    // This is single threaded, no need to be careful about concurrent accesses
    private static final ArrayDeque<String> handledMessageIDs = new ArrayDeque<>(MAX_HANDLED_MESSAGE_IDS_COUNT);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Logger.internal(TAG, "null intent. Ignoring.");
            return;
        }

        if (isFCMMessage(intent)) {
            final String messageID = getGoogleMessageID(intent);
            if (isDuplicateMessage(messageID)) {
                Logger.info(TAG, "Got a duplicate message_id from FCM, ignoring.");
                return;
            }
            if (presentNotification(context, intent)) {
                markMessageAsHandled(messageID);
            }
        } else {
            Logger.internal(TAG, "Intent was not a push message.");
        }
    }

    private boolean isFCMMessage(Intent intent) {
        // FCM has multiple push types, which we should not handle
        final String type = intent.getStringExtra("message_type");
        return type == null || "gcm".equalsIgnoreCase(type);
    }

    @VisibleForTesting
    protected boolean presentNotification(@NonNull Context context, @NonNull Intent intent) {
        // This method will try to avoid starting a job if possible to avoid potential delays
        // in notification display. Up to a certain frequency, high priority FCM messages should go
        // into that "fast path".

        // High priority FCM pushes have the RECEIVER_FOREGROUND flag. If it is missing, we can skip
        // trying to start a normal service on O and start a Job.
        @SuppressLint("InlinedApi")
        boolean isHighPriorityPush = (intent.getFlags() & Intent.FLAG_RECEIVER_FOREGROUND) != 0;

        // If we're on Android O and a normal priority push, start a Job, startService is guaranteed
        // to fail.
        if (!isHighPriorityPush && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Logger.internal(TAG, "Normal priority notification: scheduling a Job");
            return scheduleJob(context, intent);
        } else {
            // We're either on a high priority push or on an old Android version, start service directly.
            // This can happen for multiple reasons:
            //  - User has forced background restrictions
            //  - FCM's temporary restriction exclusion has already expired
            //  - Something else failed
            // Fallback on a Job if possible
            Logger.internal(TAG, "High priority notification/legacy Android: starting service");
            try {
                startPresentationService(context, intent);
                return true;
            } catch (IllegalStateException | SecurityException e) {
                // IllegalStateException can happen on Android O, fallback on scheduling a Job.
                // On earlier Android versions, it should not happen.
                // The SecurityException happens on some OEMs, like Wiko, for an unknown reason.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Logger.internal(TAG, "Failed to start service, scheduling Job");
                    return scheduleJob(context, intent);
                } else {
                    Logger.error(TAG, "Could not start notification presentation service:", e);
                    return false;
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private boolean scheduleJob(@NonNull Context context, @NonNull Intent intent) {
        try {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) {
                Logger.internal(TAG, "Could not get Job Scheduler system service");
                return false;
            }

            final Bundle intentExtras = intent.getExtras();
            if (intentExtras == null || intentExtras.isEmpty()) {
                Logger.internal(TAG, "Intent extras were empty, not scheduling push notification presenter job");
                return false;
            }

            final Bundle jobExtras = new Bundle();
            jobExtras.putBundle(BatchPushJobService.JOB_EXTRA_PUSH_DATA_KEY, intentExtras);
            int jobId = (int) (Math.random() * Integer.MAX_VALUE);
            JobInfo job = new JobInfo.Builder(jobId, new ComponentName(context, BatchPushJobService.class))
                .setOverrideDeadline(3600000) // one hour
                .setTransientExtras(jobExtras)
                .build();

            if (scheduler.schedule(job) == JobScheduler.RESULT_FAILURE) {
                Logger.internal(TAG, "Failed to schedule the push notification presenter job");
                return false;
            }

            Logger.internal(TAG, "Successfully scheduled the push notification presenter job");
            return true;
        } catch (Exception e1) {
            Logger.internal(TAG, "Could schedule Batch push presentation job", e1);
        }
        return false;
    }

    private void startPresentationService(@NonNull Context context, @NonNull Intent intent) {
        // Explicitly specify that BatchPushService will handle the intent and start it.
        ComponentName comp = new ComponentName(context.getPackageName(), BatchPushService.class.getName());
        if (GenericHelper.isWakeLockPermissionAvailable(context)) {
            startWakefulService(context, intent.setComponent(comp));
        } else {
            context.startService(intent.setComponent(comp));
        }
    }

    private boolean isDuplicateMessage(@Nullable String msgID) {
        // Can't deduplicate a message ID if we do not have one
        if (msgID == null) {
            return false;
        }

        return handledMessageIDs.contains(msgID);
    }

    private void markMessageAsHandled(@Nullable String msgID) {
        if (msgID == null) {
            return;
        }

        handledMessageIDs.add(msgID);
        if (handledMessageIDs.size() > MAX_HANDLED_MESSAGE_IDS_COUNT) {
            handledMessageIDs.pollFirst();
        }
    }

    // Extract the Firebase Message identifier from the payload
    @Nullable
    private String getGoogleMessageID(@NonNull Intent intent) {
        // GCM apparently can use both keys to store the message ID.
        // The google. key can't be controlled via the custom payload, not sure about
        // message ID.
        // FCM checks for both, so do the same.
        final String googleMessageID = intent.getStringExtra("google.message_id");
        if (!TextUtils.isEmpty(googleMessageID)) {
            return googleMessageID;
        }

        final String messageID = intent.getStringExtra("message_id");
        if (!TextUtils.isEmpty(messageID)) {
            return messageID;
        }

        return null;
    }

    @VisibleForTesting
    static int getHandledMessageIDsSize() {
        return handledMessageIDs.size();
    }

    @VisibleForTesting
    static void resetHandledMessageIDs() {
        handledMessageIDs.clear();
    }
}
