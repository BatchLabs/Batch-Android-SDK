package com.batch.android;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import java.lang.ref.WeakReference;

/**
 * JobService implementation of Batch Push
 */

@RequiresApi(api = Build.VERSION_CODES.O)
@PublicSDK
public class BatchPushJobService extends JobService {

    private static final String TAG = "BatchPushJobService";

    public static final String JOB_EXTRA_PUSH_DATA_KEY = "com.batch.push_data";

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        if (jobParameters == null) {
            Logger.internal(TAG, "JobParameters were null");
            return false;
        }

        Bundle pushData = jobParameters.getTransientExtras().getBundle(JOB_EXTRA_PUSH_DATA_KEY);
        if (pushData == null) {
            Logger.internal(TAG, "No push data was found in the job's parameters");
            return false;
        }

        PresentPushTask presentPushTask = new PresentPushTask(pushData, this, jobParameters);
        presentPushTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Logger.internal(TAG, "onStopJob");
        return false;
    }

    private static class PresentPushTask extends AsyncTask<Void, Void, Void> {

        private Bundle pushData;

        private WeakReference<JobService> originService;

        private JobParameters originJobParameters;

        PresentPushTask(
            @NonNull Bundle pushData,
            @NonNull JobService originService,
            @NonNull JobParameters originJobParameters
        ) {
            this.pushData = pushData;
            this.originService = new WeakReference<>(originService);
            this.originJobParameters = originJobParameters;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final JobService unwrappedService = originService != null ? originService.get() : null;

            if (unwrappedService == null) {
                Logger.internal(TAG, "JobService vanished before a push notification could be presented with it.");
                return null;
            }

            if (originJobParameters == null) {
                Logger.internal(TAG, "JobParameters vanished before a push notification could be presented with them.");
                return null;
            }

            if (pushData == null) {
                Logger.internal(TAG, "Unexpected error: missing push data");
                return null;
            }

            try {
                BatchPushNotificationPresenter.displayForPush(unwrappedService, pushData);
            } catch (NotificationInterceptorRuntimeException nie) {
                throw nie.getWrappedRuntimeException();
            } catch (Exception e) {
                Logger.internal(TAG, "Error while handing notification", e);
            } finally {
                if (originJobParameters != null) {
                    unwrappedService.jobFinished(originJobParameters, false);
                    Logger.internal(TAG, "Push notification display job finished successfully");
                } else {
                    Logger.internal(TAG, "Unexpected error: job parameters vanished");
                }
            }

            return null;
        }
    }
}
