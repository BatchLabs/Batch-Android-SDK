package com.batch.android.push;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Registration {

    @NonNull
    public String provider;

    @NonNull
    public String registrationID;

    @Nullable
    public String senderID;

    @Nullable
    public String gcpProjectID;

    public Registration(
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
}
