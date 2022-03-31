package com.batch.android.module;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import com.batch.android.BatchDisplayReceiptJobService;
import com.batch.android.WebserviceLauncher;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.core.TaskRunnable;
import com.batch.android.core.Webservice;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.displayreceipt.CacheHelper;
import com.batch.android.displayreceipt.DisplayReceipt;
import com.batch.android.post.DisplayReceiptPostDataProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.webservice.listener.DisplayReceiptWebserviceListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Module
@Singleton
public class DisplayReceiptModule extends BatchModule {

    private static final String TAG = "DisplayReceipt";

    private OptOutModule optOutModule;

    private DisplayReceiptModule(OptOutModule optOutModule) {
        this.optOutModule = optOutModule;
    }

    @Provide
    public static DisplayReceiptModule provide() {
        return new DisplayReceiptModule(OptOutModuleProvider.get());
    }

    @Override
    public String getId() {
        return "displayreceipt";
    }

    @Override
    public int getState() {
        return 1;
    }

    @Override
    public void batchDidStart() {
        super.batchDidStart();
        final Context context = RuntimeManagerProvider.get().getContext();

        if (Boolean.TRUE.equals(optOutModule.isOptedOutSync(context))) {
            Logger.internal(TAG, "Batch is opted out, refusing to send display receipt.");
            return;
        }

        if (context != null) {
            Logger.internal(TAG, "Trying to send cached display receipts...");
            sendReceipt(context, true);
        }
    }

    /**
     * Save the display receipt when receiving a push payload
     * Must be done before launching the JobService
     *
     * @param context
     * @param pushData
     */
    private File savePushReceipt(Context context, InternalPushData pushData) {
        Map<String, Object> od = pushData.getOpenData();
        Map<String, Object> ed = pushData.getReceiptEventData();

        long timestamp = System.currentTimeMillis() / 1000L;
        byte[] data = DisplayReceipt.pack(timestamp, false, 0, od, ed);
        if (data != null) {
            return CacheHelper.write(context, timestamp, data);
        }
        return null;
    }

    /**
     * Save the new receipt and schedule the send of display receipts using a JobService
     * If no delay or Android version is too low, we send without a job
     */
    public void scheduleDisplayReceipt(Context context, @NonNull InternalPushData pushData) {
        if (Boolean.TRUE.equals(optOutModule.isOptedOutSync(context))) {
            Logger.internal(TAG, "Batch is opted out, refusing to send display receipt.");
            return;
        }

        // FIRST we save the receipt in cache asap
        File newReceipt = savePushReceipt(context, pushData);
        if (newReceipt == null) {
            return;
        }

        // Get delay from push payload
        long dmi = pushData.getReceiptMinDelay();
        long dma = pushData.getReceiptMaxDelay();
        if (dma < 0 || dma < dmi) {
            dma = 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && dmi >= 0) {
            try {
                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (scheduler == null) {
                    Logger.internal(TAG, "Could not get Job Scheduler system service");
                    return;
                }
                int jobId = (int) (Math.random() * Integer.MAX_VALUE);
                JobInfo.Builder builder = new JobInfo.Builder(
                    jobId,
                    new ComponentName(context, BatchDisplayReceiptJobService.class)
                )
                    .setOverrideDeadline(dma * 1000L)
                    .setMinimumLatency(dmi * 1000L)
                    .setPersisted(true)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY); // We need network

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // We don't know the exact payload size, but it's likely less than 6Kb
                    // 1024 bytes per receipt (max 5 receipt per request) + 1024 bytes for HTTP overhead
                    builder.setEstimatedNetworkBytes(0L, 1024 * 6);
                }

                if (scheduler.schedule(builder.build()) == JobScheduler.RESULT_FAILURE) {
                    Logger.internal(TAG, "Failed to schedule the display receipt job");
                } else {
                    Logger.internal(TAG, "Successfully scheduled the display receipt job");
                }
            } catch (Exception e1) {
                Logger.internal(TAG, "Could not schedule Batch display receipt job", e1);
            }
        } else {
            sendReceipt(context, false);
        }
    }

    /**
     * Send up to {@link CacheHelper.MAX_READ_RECEIPT_FROM_CACHE} display receipt to the ws.
     * <p>
     * Update send attempts and replay in cached files.
     * Delete cached file if send is successful.
     *
     * @param context app context
     * @param replay  replay = true if request is at SDK start false otherwise
     */
    public static synchronized void sendReceipt(Context context, boolean replay) {
        List<File> files = CacheHelper.getCachedFiles(context, false);
        if (files != null && files.size() > 0) {
            Map<File, DisplayReceipt> payloads = new HashMap<>();
            for (File file : files) {
                byte[] data = CacheHelper.read(file);
                if (data != null) {
                    // We update and save each receipt before send
                    DisplayReceipt displayReceipt = DisplayReceipt.unpack(data);
                    if (displayReceipt != null) {
                        displayReceipt.setReplay(replay);
                        displayReceipt.incrementSendAttempt();

                        byte[] payload = displayReceipt.packAndWrite(file);
                        if (payload != null) {
                            payloads.put(file, displayReceipt);
                        }
                    }
                }
            }

            if (payloads.size() <= 0) {
                Logger.internal(TAG, "No receipt to send, aborting...");
                return;
            }

            DisplayReceiptPostDataProvider dataProvider = new DisplayReceiptPostDataProvider(payloads.values());
            TaskRunnable runnable = WebserviceLauncher.initDisplayReceiptWebservice(
                context,
                dataProvider,
                new DisplayReceiptWebserviceListener() {
                    @Override
                    public void onSuccess() {
                        // WS success - Delete all cached files
                        for (File file : payloads.keySet()) {
                            file.delete();
                        }
                    }

                    @Override
                    public void onFailure(Webservice.WebserviceError e) {
                        Logger.internal(TAG, "Error when sending receipt", e);
                    }
                }
            );

            if (runnable != null) {
                runnable.run();
            }
        }
    }

    /**
     * Delete all receipt still in cache
     *
     * @param context
     */
    public void wipeData(@NonNull Context context) {
        CacheHelper.deleteAll(context);
    }
}
