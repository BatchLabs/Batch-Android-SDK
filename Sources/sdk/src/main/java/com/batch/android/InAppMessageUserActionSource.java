package com.batch.android;

import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONObject;

/**
 * Represents an In-App Message user action source.
 */
@PublicSDK
public interface InAppMessageUserActionSource extends UserActionSource {
    @NonNull
    JSONObject getCustomPayload();
}
