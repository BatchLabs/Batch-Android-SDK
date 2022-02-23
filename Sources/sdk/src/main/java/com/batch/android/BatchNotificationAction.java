package com.batch.android;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.di.providers.ActionModuleProvider;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Notification Action button
 */
@PublicSDK
public class BatchNotificationAction {

    /**
     * CTA label text
     */
    public String label = "";

    /**
     * CTA Drawable name. Use this to find your drawable into your resources, or use an alias system.
     * <br/>
     * If using {@link #getSupportActions(Context, List, BatchPushPayload, Integer)}, this value can be one of the alias registered using {@link Batch.Actions#addDrawableAlias(String, int)}
     */
    public String drawableName;

    /**
     * Action identifier. Made to work with {@link com.batch.android.Batch.Actions}
     */
    public String actionIdentifier = "";

    /**
     * Action
     */
    public JSONObject actionArguments;

    /**
     * Does this action imply showing any UI or will it act in the background?
     * An action that has no UI should not close the notification drawer, but should still dismiss the notification itself (if asked to).
     */
    public boolean hasUserInterface = true;

    /**
     * Should a tap on this action dismiss its notification?
     * <p>
     * Note that for this to work, you will need to provide a valid notificationId to {@link #getSupportActions(Context, List, BatchPushPayload, Integer)}.
     */
    public boolean shouldDismissNotification = true;

    /**
     * Converts {@link BatchNotificationAction} instances to {@link NotificationCompat.Action}, allowing you to add actions to a Notification the same way
     * the SDK internally does it, taking care of all the boilerplate.
     * <p>
     *
     * @param context        Your application's context
     * @param batchActions   List of {@link BatchNotificationAction} instances to convert
     * @param pushPayload    The Batch push payload associated with these actions, if any.
     *                       If not set, the {@link UserActionSource} will not be set when your custom {@link UserActionRunnable} is called.
     *                       This parameter might be required for some internal actions.
     * @param notificationId The Id of the notification these actions will be attached to. Required for {@link #shouldDismissNotification} to work.
     * @return A list of {@link NotificationCompat.Action} instances matching the provided {@link BatchNotificationAction},
     * if they've been successfully converted
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    @NonNull
    public static List<NotificationCompat.Action> getSupportActions(
        @NonNull Context context,
        @NonNull List<BatchNotificationAction> batchActions,
        @Nullable BatchPushPayload pushPayload,
        @Nullable Integer notificationId
    ) {
        // UnspecifiedImmutableFlag is suppressed as the linter can't recognize our conditional
        // Android M immutability flag.

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        final List<NotificationCompat.Action> retVal = new ArrayList<>();
        if (batchActions == null || batchActions.size() == 0) {
            return retVal;
        }

        // Used for request code uniqueness
        int actionCounter = 0;

        for (BatchNotificationAction action : batchActions) {
            actionCounter++;
            final Intent actionIntent = new Intent(
                context,
                action.hasUserInterface ? BatchActionActivity.class : BatchActionService.class
            );
            actionIntent.setAction(BatchActionService.INTENT_ACTION);
            actionIntent.putExtra(BatchActionService.ACTION_EXTRA_IDENTIFIER, action.actionIdentifier);
            // This is a little wasteful since it is in the original payload, but reading it back from there would require exploring the actions again
            actionIntent.putExtra(BatchActionService.ACTION_EXTRA_ARGS, action.actionArguments.toString());
            if (action.shouldDismissNotification && notificationId != null) {
                actionIntent.putExtra(BatchActionService.ACTION_EXTRA_DISMISS_NOTIFICATION_ID, notificationId);
            }
            if (pushPayload != null) {
                pushPayload.writeToIntentExtras(actionIntent);
            }

            int actionIntentFlags = PendingIntent.FLAG_ONE_SHOT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Remove @SuppressLint("UnspecifiedImmutableFlag") if you ever delete this line,
                // so that the linter can warn appropriately
                actionIntentFlags = actionIntentFlags | PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingActionIntent;
            if (action.hasUserInterface) {
                actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pendingActionIntent =
                    PendingIntent.getActivity(
                        context,
                        actionCounter * (int) System.currentTimeMillis(),
                        actionIntent,
                        actionIntentFlags
                    );
            } else {
                pendingActionIntent =
                    PendingIntent.getService(
                        context,
                        actionCounter * (int) System.currentTimeMillis(),
                        actionIntent,
                        actionIntentFlags
                    );
            }

            retVal.add(
                new NotificationCompat.Action.Builder(
                    ActionModuleProvider.get().getDrawableIdForNameOrAlias(context, action.drawableName),
                    action.label,
                    pendingActionIntent
                )
                    .build()
            );
        }

        return retVal;
    }
}
