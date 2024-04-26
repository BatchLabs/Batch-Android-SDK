package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;

/**
 * Class holding the push registration information.
 */
@PublicSDK
public class BatchPushRegistration {

    /**
     * The Push registration provider. Might be FCM-Token or HMS.
     */
    @NonNull
    private final String provider;

    /**
     * The Push registration identifier (Push Token).
     */
    @NonNull
    private final String registrationID;

    /**
     * The Sender ID of the provider, or equivalent. For example: "8122930293"
     * Also known as the GCP Project Number
     */
    @Nullable
    private final String senderID;

    /**
     * The GCP Project ID. For example "batch_sample".
     * Not to be confused with Sender ID, which is the Project Number. (FCM only)
     */
    @Nullable
    private final String gcpProjectID;

    public BatchPushRegistration(
        @NonNull String provider,
        @NonNull String registrationID,
        @Nullable String senderID,
        @Nullable String gcpProjectID
    ) {
        this.provider = provider;
        this.registrationID = registrationID;
        this.senderID = senderID;
        this.gcpProjectID = gcpProjectID;
    }

    /**
     * Get the Push registration provider. Might be FCM-Token or HMS.
     *
     * @return The Push registration provider. Might be FCM-Token or HMS.
     */
    @NonNull
    public String getProvider() {
        return provider;
    }

    /**
     * Get the registration Push Token (also known as registration id).
     *
     * @return The Push Token.
     */
    @NonNull
    public String getToken() {
        return registrationID;
    }

    /**
     * The Sender ID of the provider, or equivalent. Also known as the GCP Project Number.
     * For example: "8122930293"
     *
     * @return The Sender ID of the provider or null.
     */
    @Nullable
    public String getSenderID() {
        return senderID;
    }

    /**
     * The GCP Project ID. For example "batch_sample".
     * Not to be confused with Sender ID, which is the Project Number. (FCM only)
     *
     * @return he GCP Project ID or null.
     */
    @Nullable
    public String getGcpProjectID() {
        return gcpProjectID;
    }
}
