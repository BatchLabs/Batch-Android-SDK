package com.batch.android.core;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import java.util.List;

/**
 * Simple helper for Android 21+ Jobs
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class JobHelper {

  private static final int MAX_GENERATION_ATTEMPTS = 20;

  public static synchronized int generateUniqueJobId(
    @NonNull JobScheduler scheduler
  ) throws GenerationException {
    // Consider letting developers override this
    int generatedId;

    for (int attempts = 0; attempts <= MAX_GENERATION_ATTEMPTS; attempts++) {
      generatedId = (int) (Math.random() * Integer.MAX_VALUE);

      if (!jobListContainsJobId(scheduler.getAllPendingJobs(), generatedId)) {
        return generatedId;
      }
    }

    throw new GenerationException(
      "Could not generate an unique id: attempts exhausted"
    );
  }

  private static boolean jobListContainsJobId(
    List<JobInfo> jobList,
    int jobId
  ) {
    if (jobList == null || jobList.size() == 0) {
      return false;
    }

    for (JobInfo job : jobList) {
      if (job.getId() == jobId) {
        return true;
      }
    }

    return false;
  }

  public static class GenerationException extends Exception {

    public GenerationException(String message) {
      super(message);
    }
  }
}
