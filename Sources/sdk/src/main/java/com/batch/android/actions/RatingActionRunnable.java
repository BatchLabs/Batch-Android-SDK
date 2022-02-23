package com.batch.android.actions;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.UserActionRunnable;
import com.batch.android.UserActionSource;
import com.batch.android.core.Logger;
import com.batch.android.core.Promise;
import com.batch.android.core.stores.StoreApplication;
import com.batch.android.core.stores.StoreApplicationFactory;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.module.ActionModule;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public class RatingActionRunnable implements UserActionRunnable {

    private static final String TAG = "RatingAction";
    public static final String IDENTIFIER = ActionModule.RESERVED_ACTION_IDENTIFIER_PREFIX + "rating";

    public RatingActionRunnable() {}

    @Override
    public void performAction(
        @Nullable Context context,
        @NonNull String identifier,
        @NonNull JSONObject args,
        @Nullable UserActionSource source
    ) {
        if (context == null) {
            Logger.error(TAG, "Tried to perform a Rating action, but no context was available");
            return;
        }
        tryOpenPlayStoreInAppRating(context)
            .catchException(e -> {
                String message = (e instanceof RatingActionRunnableException) ? e.getMessage() : null;
                if (message == null) {
                    message = "This device might not have Google's services, or the Play Store version is too old.";
                }
                Logger.error(
                    ActionModule.TAG,
                    "Could not use in-app rating: " + message,
                    e != null ? e.getCause() : null
                );

                openStore(context);
            });
    }

    private Promise<Void> tryOpenPlayStoreInAppRating(@NonNull Context context) {
        return new Promise<>(promise -> {
            try {
                // Try to find an activity context
                Activity supportActivity = null;
                if (context instanceof Activity) {
                    supportActivity = (Activity) context;
                } else {
                    supportActivity = RuntimeManagerProvider.get().getActivity();
                }
                if (supportActivity == null) {
                    throw new RatingActionRunnableException("Could not find an Activity to use for display.", null);
                }
                Activity finalSupportActivity = supportActivity;

                ReviewManager reviewManager = ReviewManagerFactory.create(context);
                reviewManager
                    .requestReviewFlow()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ReviewInfo reviewInfo = task.getResult();
                            if (reviewInfo == null) {
                                promise.reject(
                                    new RatingActionRunnableException("Invalid reply from Play Store (-10).", null)
                                );
                            }
                            reviewManager.launchReviewFlow(finalSupportActivity, reviewInfo);
                        } else {
                            promise.reject(new RatingActionRunnableException(null, null));
                        }
                    });
            } catch (NoClassDefFoundError ignored) {
                throw new RatingActionRunnableException("Is 'com.google.android.play:core' available?", null);
            }
        });
    }

    private void openStore(@NonNull Context context) {
        Logger.info(ActionModule.TAG, "In-app rating unavailable: trying to open store application.");
        StoreApplication app = StoreApplicationFactory.getMainStore(context);
        if (app != null) {
            app.open(context);
        }
    }

    private static class RatingActionRunnableException extends RuntimeException {

        public RatingActionRunnableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
