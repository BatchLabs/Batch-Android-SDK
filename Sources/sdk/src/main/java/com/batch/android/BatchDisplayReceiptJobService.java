package com.batch.android;

import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * A legacy no-op JobService for backward compatibility.
 *<p>
 * Batch SDK v2 scheduled this service. After an app update to a version using Batch SDK v3+,
 * the Android OS may still attempt to start this job from its cache.
 * <p>
 * @deprecated This dummy implementation is preventing a {@link java.lang.ClassNotFoundException} for users updating the app.
 */
@Deprecated
public class BatchDisplayReceiptJobService extends JobService {

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
