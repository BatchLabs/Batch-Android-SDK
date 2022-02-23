package com.batch.android;

import android.app.IntentService;
import android.content.Intent;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;

/**
 * Batch's service for handling the push messages and show a notification
 * <p>
 * This is a legacy implementation, and should not be used on versions higher than Android O
 *
 */
@PublicSDK
public class BatchPushService extends IntentService {

    private static final String TAG = "BatchPushService";

    public BatchPushService() {
        super("BatchPushService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (intent == null) {
                Logger.internal(TAG, "Error while handling notification: null intent");
                return;
            }
            BatchPushNotificationPresenter.displayForPush(this, intent.getExtras());
        } catch (NotificationInterceptorRuntimeException nie) {
            throw nie.getWrappedRuntimeException();
        } catch (Exception e) {
            Logger.internal(TAG, "Error while handing notification", e);
        } finally {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            BatchPushReceiver.completeWakefulIntent(intent);
        }
    }
}
