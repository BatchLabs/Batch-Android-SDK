package com.batch.android;

import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;

/**
 * Model that represents an user action, which can be triggered by Batch SDK in various contexts.
 */
@PublicSDK
public class UserAction {

    private String identifier;

    private UserActionRunnable runnable;

    /**
     * Construct an Action for the specified parameters
     *
     * @param identifier Action identifier. Must uniquely define an action in your app. Might be lowercased, so be sure to compare it with case unsensitive methods.
     * @param runnable   The {@link UserActionRunnable} that will be executed when Batch needs to perform your action.
     */
    public UserAction(@NonNull String identifier, @NonNull UserActionRunnable runnable) {
        //noinspection ConstantConditions
        if (identifier == null) {
            throw new IllegalArgumentException("identifier cannot be null");
        }

        if ("".equals(identifier.trim())) {
            throw new IllegalArgumentException("identifier cannot be empty");
        }

        //noinspection ConstantConditions
        if (runnable == null) {
            throw new IllegalArgumentException("runnable cannot be null");
        }

        this.identifier = identifier;
        this.runnable = runnable;
    }

    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    @NonNull
    public UserActionRunnable getRunnable() {
        return runnable;
    }
}
