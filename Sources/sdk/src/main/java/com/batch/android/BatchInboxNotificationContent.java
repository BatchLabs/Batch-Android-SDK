package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.inbox.InboxNotificationContentInternal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * BatchInboxNotificationContent is a model representing the content of an inbox notification
 */
@PublicSDK
public class BatchInboxNotificationContent {

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

    /**
     * Flag indicating whether this notification is deleted or not.
     *
     * @return true if notification is deleted
     * @deprecated You should refresh your copy of the data with getFetchedNotifications() after using markAsDeleted.
     */
    @Deprecated
    public boolean isDeleted() {
        return internalContent.isDeleted;
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
}
