package com.batch.android.inbox;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;

/**
 * NotificationIdentifiers groups all identifiers of a notification, which should not be lost
 * when a notification is collapsed into another for deduplication
 */

public class NotificationIdentifiers {

    @NonNull
    public String identifier;

    @NonNull
    public String sendID;

    @Nullable
    public String installID;

    @Nullable
    public String customID;

    @Nullable
    public Map<String, Object> additionalData;

    public NotificationIdentifiers(@NonNull String identifier, @NonNull String sendID) {
        this.identifier = identifier;
        this.sendID = sendID;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(identifier) && !TextUtils.isEmpty(sendID);
    }
}
