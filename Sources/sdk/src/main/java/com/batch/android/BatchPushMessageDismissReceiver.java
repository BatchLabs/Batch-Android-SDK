package com.batch.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.compat.WakefulBroadcastReceiver;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.EventDispatcherModuleProvider;
import com.batch.android.eventdispatcher.PushEventPayload;

/**
 * Batch's implementation of dismiss intent of push notification
 *
 */
@PublicSDK
public class BatchPushMessageDismissReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "BatchPushMessageDismissReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Logger.internal(TAG, "Null intent");
            return;
        }

        final Bundle extras = intent.getExtras();
        if (extras == null || extras.isEmpty()) {
            Logger.internal(TAG, "Intent extras were empty, stop dispatching event");
            return;
        }

        try {
            BatchPushPayload pushPayload = BatchPushPayload.payloadFromBundle(extras);
            Batch.EventDispatcher.Payload eventPayload = new PushEventPayload(pushPayload);

            // We may come from background, try to reload dispatchers from manifest
            EventDispatcherModuleProvider.get().loadDispatcherFromContext(context);
            EventDispatcherModuleProvider
                .get()
                .dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISMISS, eventPayload);
        } catch (BatchPushPayload.ParsingException e) {
            Logger.internal(TAG, "Invalid payload, skip dispatchers");
        }
    }
}
