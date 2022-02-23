package com.batch.android;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.batch.android.annotation.PublicSDK;

/**
 * Abstract class describing a notification interceptor.
 * <p>
 * An interceptor's job is to override some aspects of a notification that Batch wants to display.
 * See the various methods to see what you can override.
 */
@PublicSDK
public abstract class BatchNotificationInterceptor {

    /**
     * Get the builder instance used to generate the notification.<br/>
     * If you're basing your builder on defaultBuilder, some methods might have undefined behaviour as Batch already calls many of them.<br/>
     * Likewise, returning another instance of the builder than defaultBuilder's might result in loss of functionality if not reimplemented.
     * <p>
     *
     * @param context          Context. Will usually be a service context.
     * @param defaultBuilder   NotificationCompat builder that has been created and fully configured by Batch.
     * @param pushIntentExtras Raw push intent extras. Read directly your custom payload values from the extras as string values, or use {@link BatchPushPayload#payloadFromReceiverExtras(Bundle)} to extract Batch data from the extras.
     * @param notificationId   Notification id that will be used to post the notification. Result of {@link #getPushNotificationId(Context, int, Bundle)}.
     * @return A valid {@link NotificationCompat.Builder} instance, or null if you want to stop the display of that notification.
     */
    @Nullable
    public NotificationCompat.Builder getPushNotificationCompatBuilder(
        @NonNull Context context,
        @NonNull NotificationCompat.Builder defaultBuilder,
        @NonNull Bundle pushIntentExtras,
        int notificationId
    ) {
        return defaultBuilder;
    }

    /**
     * Notification Id to use when posting the notification to the system.
     * This id should be unique per notification, unless you want to update it with other pushes (such as a sports game score update, for example)
     * If you always return the same value, the notification can behave unexpectedly.
     *
     * @param context          Context. Will usually be a service context.
     * @param defaultId        Notification Id that was generated and about to be used by Batch
     * @param pushIntentExtras Raw push intent extras. Read directly your custom payload values from the extras as string values, or use {@link BatchPushPayload#payloadFromReceiverExtras(Bundle)} to extract Batch data from the intent.
     * @return A valid notification id.
     */
    public int getPushNotificationId(@NonNull Context context, int defaultId, @NonNull Bundle pushIntentExtras) {
        return defaultId;
    }
}
