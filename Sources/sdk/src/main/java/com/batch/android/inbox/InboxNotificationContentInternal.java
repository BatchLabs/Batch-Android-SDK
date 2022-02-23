package com.batch.android.inbox;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchNotificationSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Internal representation of an inbox notification's content
 */
public class InboxNotificationContentInternal {

    @Nullable
    public String title;

    @Nullable
    public String body;

    @NonNull
    public BatchNotificationSource source;

    public boolean isUnread;

    public boolean isDeleted;

    @NonNull
    public Date date;

    @NonNull
    public Map<String, String> payload;

    @NonNull
    public NotificationIdentifiers identifiers;

    @Nullable
    public List<NotificationIdentifiers> duplicateIdentifiers;

    public InboxNotificationContentInternal(
        @NonNull BatchNotificationSource source,
        @NonNull Date date,
        @NonNull Map<String, String> payload,
        @NonNull NotificationIdentifiers identifiers
    ) {
        this.source = source;
        this.date = date;
        this.payload = payload;
        this.identifiers = identifiers;
    }

    @NonNull
    public Bundle getReceiverLikePayload() {
        final Bundle b = new Bundle();
        for (Map.Entry<String, String> payloadItem : payload.entrySet()) {
            b.putString(payloadItem.getKey(), payloadItem.getValue());
        }
        return b;
    }

    public void addDuplicateIdentifiers(NotificationIdentifiers identifiers) {
        if (duplicateIdentifiers == null) {
            duplicateIdentifiers = new ArrayList<>();
        }
        duplicateIdentifiers.add(identifiers);
    }

    public boolean isValid() {
        return (
            source != null &&
            date != null &&
            payload != null &&
            payload.size() > 0 &&
            identifiers != null &&
            identifiers.isValid()
        );
    }
}
