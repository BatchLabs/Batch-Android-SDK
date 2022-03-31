package com.batch.android;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.batch.android.core.Logger;
import com.batch.android.module.DisplayReceiptModule;
import java.lang.ref.WeakReference;

/**
 * JobService implementation of Batch Display Receipt
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BatchDisplayReceiptJobService extends JobService {

    private static final String TAG = "BatchDisplayReceiptJobService";

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Logger.internal(TAG, "starting display receipt job service");
        SendReceiptTask sendReceiptTask = new SendReceiptTask(this, jobParameters);
        sendReceiptTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private static class SendReceiptTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<JobService> originService;
        private JobParameters originJobParameters;

        SendReceiptTask(@NonNull JobService originService, @NonNull JobParameters originJobParameters) {
            this.originService = new WeakReference<>(originService);
            this.originJobParameters = originJobParameters;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            final JobService unwrappedService = originService != null ? originService.get() : null;

            if (unwrappedService == null) {
                Logger.internal(TAG, "JobService vanished before a receipt could be sent with it.");
                return null;
            }

            DisplayReceiptModule.sendReceipt(unwrappedService, false);
            unwrappedService.jobFinished(originJobParameters, false);
            Logger.internal(TAG, "Display receipt job finished successfully");
            return null;
        }
    }
}
