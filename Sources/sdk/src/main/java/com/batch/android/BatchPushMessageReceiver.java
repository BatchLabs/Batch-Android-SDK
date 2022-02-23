package com.batch.android;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.compat.WakefulBroadcastReceiver;
import com.batch.android.core.JobHelper;
import com.batch.android.core.Logger;

/**
 * Batch's implementation of GCM's Push BroadcastReceiver
 *
 */
@PublicSDK
public class BatchPushMessageReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "BatchPushMessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Logger.internal(TAG, "null intent");
            return;
        }

        String msgType = intent.getStringExtra("message_type");
        if (msgType == null || "gcm".equalsIgnoreCase(msgType)) {
            // Android O forbids us to directly start a service in the background, so use JobScheduler
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    if (scheduler == null) {
                        Logger.internal(TAG, "Could not get Job Scheduler system service");
                        return;
                    }

                    final Bundle intentExtras = intent.getExtras();
                    if (intentExtras == null || intentExtras.isEmpty()) {
                        Logger.internal(
                            TAG,
                            "Intent extras were empty, not scheduling push notification presenter job"
                        );
                        return;
                    }

                    final Bundle jobExtras = new Bundle();
                    jobExtras.putBundle(BatchPushJobService.JOB_EXTRA_PUSH_DATA_KEY, intentExtras);

                    JobInfo job = new JobInfo.Builder(
                        JobHelper.generateUniqueJobId(scheduler),
                        new ComponentName(context, BatchPushJobService.class)
                    )
                        .setOverrideDeadline(3600000) // one hour
                        .setTransientExtras(jobExtras)
                        .build();

                    if (scheduler.schedule(job) == JobScheduler.RESULT_FAILURE) {
                        Logger.internal(TAG, "Failed to schedule the push notification presenter job");
                    } else {
                        Logger.internal(TAG, "Successfully scheduled the push notification presenter job");
                    }
                } catch (JobHelper.GenerationException e) {
                    Logger.internal(TAG, "Could not find a suitable job ID", e);
                } catch (Exception e1) {
                    Logger.internal(TAG, "Could schedule Batch push presentation job", e1);
                }
            } else {
                // Explicitly specify that BatchPushService will handle the intent and start it.
                ComponentName comp = new ComponentName(context.getPackageName(), BatchPushService.class.getName());
                startWakefulService(context, intent.setComponent(comp));
            }
        } else {
            Logger.internal(TAG, "Intent was not a push message.");
        }
    }
}
