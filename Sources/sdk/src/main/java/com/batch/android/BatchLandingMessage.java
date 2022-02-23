package com.batch.android;

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Set;

/**
 * A subclass of BatchMessage that represents a push landing message
 */
@PublicSDK
public class BatchLandingMessage extends BatchMessage implements PushUserActionSource {

    public static final String KIND = "landing";

    private static final String GOOGLE_PREFIX = "google.";

    private Bundle payload;
    private JSONObject landing;

    protected BatchLandingMessage(@NonNull Bundle rawPayload, @NonNull JSONObject parsedLanding) {
        payload = cleanBundle(rawPayload);
        landing = parsedLanding;
    }

    /**
     * Remove reserved firebase's keys from a bundle
     *
     * @param bundle bundle to clean
     * @return A cleaned bundle without google's keys
     */
    private Bundle cleanBundle(Bundle bundle) {
        Bundle copy = new Bundle(bundle);
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            if (key != null && key.startsWith(GOOGLE_PREFIX)) {
                copy.remove(key);
            }
        }
        return copy;
    }

    @Override
    protected JSONObject getJSON() {
        return landing;
    }

    /**
     * @hide
     */
    @Override
    protected JSONObject getCustomPayloadInternal() {
        JSONObject json = new JSONObject();
        Set<String> keys = payload.keySet();
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String) {
                try {
                    json.put(key, value.toString());
                } catch (JSONException e) {
                    //ignore
                }
            }
        }
        return json;
    }

    @Override
    protected String getKind() {
        return KIND;
    }

    @Override
    protected Bundle getBundleRepresentation() {
        // This is way less costly than creating a BatchPushPayload to get its serialization method
        // But if BatchPushPayload's changes, we'll need to change it here
        Bundle b = new Bundle();
        b.putBundle(Batch.Push.PAYLOAD_KEY, payload);
        return b;
    }

    public Bundle getPushBundle() {
        return new Bundle(payload);
    }
}
