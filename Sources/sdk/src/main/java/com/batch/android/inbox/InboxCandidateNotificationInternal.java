package com.batch.android.inbox;

import androidx.annotation.NonNull;

/**
 * Internal representation of an inbox candidate notification's content
 */
public class InboxCandidateNotificationInternal {

    public String identifier;

    public boolean isUnread;

    public InboxCandidateNotificationInternal(@NonNull String identifier, boolean isUnread) {
        this.identifier = identifier;
        this.isUnread = isUnread;
    }
}
