package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.inbox.InboxNotificationContentInternal;
import com.batch.android.json.JSONObject;
import com.batch.android.module.MessagingModule;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * BatchInboxNotificationContent is a model representing the content of an inbox notification
 */
@PublicSDK
public class BatchInboxNotificationContent {

    private static final String TAG = "BatchInboxNotificationContent";

    @NonNull
    InboxNotificationContentInternal internalContent;

    @Nullable
    private BatchPushPayload batchPushPayloadCache = null;

    /**
     * @param internalContent
     * @hide
     */
    protected BatchInboxNotificationContent(InboxNotificationContentInternal internalContent) {
        this.internalContent = internalContent;
    }

    /**
     * Unique identifier for this notification.
     *
     * @return The unique notification identifier. Do not make assumptions about its format: it can change at any time.
     */
    @NonNull
    public String getNotificationIdentifier() {
        return internalContent.identifiers.identifier;
    }

    @Nullable
    public String getTitle() {
        return internalContent.title;
    }

    @Nullable
    public String getBody() {
        return internalContent.body;
    }

    @NonNull
    public BatchNotificationSource getSource() {
        return internalContent.source;
    }

    public boolean isUnread() {
        return internalContent.isUnread;
    }

    @NonNull
    public Date getDate() {
        return (Date) internalContent.date.clone();
    }

    /**
     * Returns whether Batch considers this a silent notification.
     *
     * A silent notification is a notification with no title and message, which won't be displayed by
     * Batch SDK.
     * Warning: Other services listening to push messages might display it.
     */
    public boolean isSilent() {
        try {
            return internalContent.body == null || getPushPayload().getInternalData().isSilent();
        } catch (BatchPushPayload.ParsingException ignored) {
            return true;
        }
    }

    /**
     * Get the payload in its raw JSON form. This might differ from what you're used to in other classes
     * handling push payloads. If you want to simulate the push behaviour, call {@link BatchPushPayload#getPushBundle()} on the instance given by {@link #getPushPayload()} .
     */
    @NonNull
    public Map<String, String> getRawPayload() {
        return new HashMap<>(internalContent.payload);
    }

    /**
     * Get {@link BatchPushPayload} instance, property initialized with the notification's original push payload
     */
    @NonNull
    public synchronized BatchPushPayload getPushPayload() throws BatchPushPayload.ParsingException {
        // This kinds of get into a lot of hoops to work, but reworking all of these classes would need
        // a lot of refactoring, and probably require to break the public API
        if (batchPushPayloadCache == null) {
            batchPushPayloadCache = new BatchPushPayload(internalContent.getReceiverLikePayload());
        }

        return batchPushPayloadCache;
    }

    /**
     * Whether the notification content has a landing message attached
     * @return true if a landing message is attached
     */
    public boolean hasLandingMessage() {
        try {
            BatchPushPayload payload = this.getPushPayload();
            return payload.hasLandingMessage();
        } catch (BatchPushPayload.ParsingException e) {
            return false;
        }
    }

    /**
     * Display the landing message attached to a BatchInboxNotificationContent.
     * Do nothing if no message is attached.
     * <p>
     * Note that this method will work even if Batch is in do not disturb mode.
     * <p>
     * The given context should be an Activity instance to enable support for the banner format, as it
     * has to be attached to an activity.
     * @param context Your activity's context, Can't be null.
     */
    public void displayLandingMessage(@NonNull Context context) {
        if (context == null) {
            Logger.internal(TAG, "Context cannot be null.");
            return;
        }

        if (OptOutModuleProvider.get().isOptedOutSync(context)) {
            Logger.info(TAG, "Ignoring as Batch has been Opted Out from");
            return;
        }

        MessagingModule messagingModule = MessagingModuleProvider.get();

        if (!messagingModule.doesAppHaveRequiredLibraries(true)) {
            return;
        }

        if (!RuntimeManagerProvider.get().isApplicationInForeground()) {
            Logger.internal(TAG, "Trying to present landing message while application is in background.");
        }

        try {
            BatchPushPayload payload = this.getPushPayload();

            if (!payload.hasLandingMessage()) {
                Logger.internal(TAG, "No landing message present.");
                return;
            }

            JSONObject messageJSON = payload.getInternalData().getLandingMessage();
            BatchLandingMessage message = new BatchLandingMessage(payload.getPushBundle(), messageJSON);
            message.setIsDisplayedFromInbox(true);
            messagingModule.displayMessage(context, message, true);
        } catch (BatchPushPayload.ParsingException e) {
            Logger.internal("Parsing push payload has failed, cannot display landing message.");
        }
    }
}
