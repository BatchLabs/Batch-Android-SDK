package com.batch.android;

import com.batch.android.inbox.InboxNotificationContentInternal;

/**
 * Helper to extract a package local field from {@link BatchInboxNotificationContent}
 *
 * @hide
 */
public class PrivateNotificationContentHelper {

    private PrivateNotificationContentHelper() {}

    public static InboxNotificationContentInternal getInternalContent(BatchInboxNotificationContent publicContent) {
        return publicContent.internalContent;
    }

    public static BatchInboxNotificationContent getPublicContent(InboxNotificationContentInternal internalContent) {
        return new BatchInboxNotificationContent(internalContent);
    }
}
