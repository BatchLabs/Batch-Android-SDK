package com.batch.android.inbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for the inbox fetch webservice's response
 */
public class InboxWebserviceResponse {

    public boolean hasMore;

    public boolean didTimeout;

    @Nullable
    public String cursor;

    @NonNull
    public List<InboxNotificationContentInternal> notifications = new ArrayList<>();
}
