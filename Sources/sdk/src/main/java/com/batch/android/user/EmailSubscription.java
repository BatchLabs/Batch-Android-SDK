package com.batch.android.user;

import android.content.Context;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchEmailSubscriptionState;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class EmailSubscription {

    /**
     * Enum defining the email kinds for subscription management
     */
    public enum Kind {
        MARKETING,
    }

    private static final String TAG = "EmailSubscription";
    private static final String EMAIL_KEY = "email";
    private static final String CUSTOM_ID_KEY = "custom_id";
    private static final String SUBSCRIPTION_KEY = "subscriptions";

    @Nullable
    private String email;

    private boolean deleteEmail = false;

    private final Map<Kind, BatchEmailSubscriptionState> subscriptions = new LinkedHashMap<>();

    public EmailSubscription() {}

    public EmailSubscription(@Nullable String email) {
        if (email == null) {
            deleteEmail = true;
        }
        this.email = email;
    }

    public void setEmail(@Nullable String email) {
        if (email == null) {
            deleteEmail = true;
        }
        this.email = email;
    }

    public void addSubscription(Kind kind, BatchEmailSubscriptionState state) {
        this.subscriptions.put(kind, state);
    }

    public void sendEmailSubscriptionEvent() {
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.error("Context cannot be null");
            return;
        }

        try {
            JSONObject params = new JSONObject();

            String customId = Batch.User.getIdentifier(context);
            if (customId == null) {
                Logger.internal(TAG, "Custom user id is null, not sending event.");
                return;
            }

            params.put(CUSTOM_ID_KEY, customId);
            if (email != null) {
                params.put(EMAIL_KEY, email);
            } else if (deleteEmail) {
                params.put(EMAIL_KEY, JSONObject.NULL);
            }

            if (!subscriptions.isEmpty()) {
                JSONObject subscriptionParam = new JSONObject();
                for (Map.Entry<Kind, BatchEmailSubscriptionState> sub : subscriptions.entrySet()) {
                    subscriptionParam.put(
                        sub.getKey().toString().toLowerCase(Locale.US),
                        sub.getValue().toString().toLowerCase(Locale.US)
                    );
                }
                params.put(SUBSCRIPTION_KEY, subscriptionParam);
            }
            TrackerModuleProvider.get().track(InternalEvents.EMAIL_CHANGED, params);
        } catch (JSONException e) {
            Logger.internal(TAG, "Failed building email subscription params.");
        }
    }
}
