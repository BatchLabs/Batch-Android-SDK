package com.batch.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.batch.android.core.DeeplinkHelper;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.core.NotificationAuthorizationStatus;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.PushImageCache;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.di.providers.BatchNotificationChannelsManagerProvider;
import com.batch.android.di.providers.DisplayReceiptModuleProvider;
import com.batch.android.di.providers.EventDispatcherModuleProvider;
import com.batch.android.di.providers.LocalCampaignsWebserviceListenerImplProvider;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.eventdispatcher.PushEventPayload;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.PayloadParser;
import com.batch.android.messaging.PayloadParsingException;
import com.batch.android.messaging.model.BannerMessage;
import com.batch.android.messaging.model.Message;
import com.batch.android.module.PushModule;
import com.batch.android.push.formats.APENFormat;
import com.batch.android.push.formats.NotificationFormat;
import com.batch.android.push.formats.SystemFormat;
import java.util.EnumSet;
import java.util.List;

/**
 * Class responsible to display a notification from a GCM/FCM push payload
 *
 * @hide
 */
public class BatchPushNotificationPresenter {

    private static final String TAG = "BatchPushNotificationPresenter";

    /**
     * Name of the meta-data to set a custom small icon to push notifications
     */
    private static final String CUSTOM_SMALL_ICON_METADATA_NAME = "com.batch.android.push.smallicon";
    /**
     * Name of Firebase's meta-data to set a custom small icon to push notifications
     */
    private static final String CUSTOM_SMALL_ICON_FIREBASE_METADATA_NAME =
        "com.google.firebase.messaging.default_notification_icon";
    /**
     * Name of the meta-data to set a color for push notifications
     */
    private static final String CUSTOM_COLOR_METADATA = "com.batch.android.push.color";
    /**
     * Default returned when no notifications should be shown
     */
    private static final int DEFAULT_NO_NOTIFICATION = -100;

    static void displayForPush(Context context, Bundle extras) throws NotificationInterceptorRuntimeException {
        if (extras != null && !extras.isEmpty()) {
            BatchPushPayload payload;

            try {
                payload = BatchPushPayload.payloadFromReceiverExtras(extras);
            } catch (IllegalArgumentException | BatchPushPayload.ParsingException ignored) {
                return; // Not a push for batch
            }

            if (payload == null) {
                return;
            }

            InternalPushData internalPushData = payload.getInternalData();
            if (internalPushData == null) {
                return;
            }

            PushModule pushModule = PushModuleProvider.get();

            if (OptOutModuleProvider.get().isOptedOutSync(context)) {
                Logger.info(TAG, "Ignoring push as Batch has been Opted Out from");
                return;
            }

            if (payload.getInternalData().getReceiptMode() == InternalPushData.ReceiptMode.FORCE) {
                DisplayReceiptModuleProvider.get().scheduleDisplayReceipt(context, payload.getInternalData());
            }

            // Do not display if manual display is activated
            if (pushModule.isManualDisplayModeActivated()) {
                Logger.internal(TAG, "Ignoring push cause manual display is activated");
                return;
            }

            // If the push is about geofencing, we have to refresh geofences
            //TODO: add this to shouldDisplayPush
            /*if (internalPushData.isLocalCampainsRefresh()) {
                _handleLocalCampaignsSilentPush(context);
            }*/

            presentNotification(context, extras, payload, pushModule.getNotificationInterceptor());
        }
    }

    /**
     * Internal method
     */
    private static void _handleLocalCampaignsSilentPush(Context context) {
        Context appContext = context.getApplicationContext();

        // TODO Retry if an Exception is caught or if we fall in onError
        // TODO Save campaigns
        try {
            TaskExecutorProvider
                .get(context)
                .submit(new LocalCampaignsWebservice(appContext, LocalCampaignsWebserviceListenerImplProvider.get()));
        } catch (Exception ex) {
            Logger.internal(TAG, "Can't refresh local campaigns. " + ex.toString());
        }
    }

    /**
     * Internal method
     */
    @SuppressWarnings("deprecation")
    public static void presentNotification(
        Context context,
        Bundle extras,
        BatchPushPayload payload,
        BatchNotificationInterceptor interceptor
    ) throws NotificationInterceptorRuntimeException {
        String alert = extras.getString(Batch.Push.ALERT_KEY);
        String title = extras.getString(Batch.Push.TITLE_KEY);

        if (alert == null) {
            Logger.internal(TAG, "Not presenting a notification since it has no value for Batch.Push.ALERT_KEY");
            return;
        }

        InternalPushData batchData = payload.getInternalData();
        if (batchData == null) {
            Logger.internal(TAG, "Not presenting a notification since we could not read batch's internal data");
            return;
        }

        if (batchData.isSilent()) {
            Logger.internal(TAG, "Not presenting a notification since it is marked as silent");
            return;
        }

        ApplicationInfo appInfo = context.getApplicationInfo();

        if (!BatchPushHelper.canDisplayPush(context, batchData)) {
            return;
        }

        if (trySendLandingToForegroundApp(context, extras, batchData)) {
            return;
        }

        Bundle interceptorExtras = null;
        if (interceptor != null) {
            // Make a copy of the extras, because I don't trust the devs not to modify it
            interceptorExtras = new Bundle(extras);
        }

        String pushId = batchData.getPushId();

        /*
         * Get defaults
         */
        int defaults = getDefaults(context);
        if (defaults == DEFAULT_NO_NOTIFICATION) {
            Logger.internal(TAG, "Not showing notifications since notification type is NONE or does not contain ALERT");
            return;
        }

        /*
         * Get title
         */
        if (title == null || title.length() == 0) {
            try {
                // Default message title.
                title = context.getPackageManager().getApplicationLabel(appInfo).toString();
            } catch (Exception e) {
                Logger.error(
                    TAG,
                    "Unable to find label of the application. Did you correctly set your application label in the manifest?"
                );
                return;
            }
        }

        /*
         * Channel
         */
        BatchNotificationChannelsManager batchChannelsManager = BatchNotificationChannelsManagerProvider.get();
        String channelID = batchChannelsManager.getChannelId(payload);
        //TODO: Figure out a better place to register the channel (not on every notification display...)
        // Problem is that we have to do it here in case the app gets updated, and we never get the context when
        // set up in Application
        batchChannelsManager.registerBatchChannelIfNeeded(context);

        /*
         * Small icon
         */
        int smallIcon = appInfo.icon;
        Integer metaDataSmallIcon = getMetaDataSmallIconResId(context); // Check if the small icon meta data is set (for unity)
        if (metaDataSmallIcon != null) {
            smallIcon = metaDataSmallIcon;
        } else { // Else try to check if any custom one is defined
            int customSmallIcon = PushModuleProvider.get().getCustomSmallIconResourceId();
            if (customSmallIcon != 0) {
                smallIcon = customSmallIcon;
            }
        }

        if (smallIcon == 0) {
            Logger.error(
                TAG,
                "Unable to find icon of the application. Did you correctly set your application icon in the manifest?"
            );
            return;
        }

        /*
         * Large icon
         */
        Bitmap largeIcon = PushModuleProvider.get().getCustomLargeIcon();
        if (batchData.hasCustomBigIcon()) {
            try {
                String cacheID = PushImageCache.buildIdentifierForURL(batchData.getCustomBigIconURL());
                Bitmap customLargeIcon = PushImageCache.getImageFromCache(context, cacheID);
                if (customLargeIcon == null) { // if cache is null, download it
                    customLargeIcon =
                        new ImageDownloadWebservice(
                            context,
                            batchData.getCustomBigIconURL(),
                            batchData.getCustomBigIconAvailableDensity()
                        )
                            .run();
                    if (customLargeIcon != null) { // Store in cache
                        PushImageCache.storeImageInCache(context, cacheID, customLargeIcon);
                    }
                }

                if (customLargeIcon != null) {
                    largeIcon = resizeLargeIcon(context, customLargeIcon);
                } else {
                    Logger.error(TAG, "Unable to download large icon image sent via payload, fallback on default");
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Error while downloading custom big icon image", e);
                Logger.error(TAG, "Unable to download large icon image sent via payload, fallback on default");
            }
        }

        /*
         * Big Picture
         */
        Bitmap bigPicture = null;
        try {
            if (Build.VERSION.SDK_INT >= 16 && batchData.hasCustomBigImage()) {
                String cacheID = PushImageCache.buildIdentifierForURL(batchData.getCustomBigImageURL());
                Bitmap customBigPicture = PushImageCache.getImageFromCache(context, cacheID);
                if (customBigPicture == null) { // if cache is null, download it
                    customBigPicture =
                        new ImageDownloadWebservice(
                            context,
                            batchData.getCustomBigImageURL(),
                            batchData.getCustomBigImageAvailableDensity()
                        )
                            .run();
                    if (customBigPicture != null) { // Store in cache
                        PushImageCache.storeImageInCache(context, cacheID, customBigPicture);
                    }
                }

                if (customBigPicture != null) {
                    bigPicture = customBigPicture;
                } else {
                    Logger.error(
                        TAG,
                        "Unable to download large big picture image sent via payload, fallback on default"
                    );
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while downloading custom big picture image", e);
            Logger.error(TAG, "Unable to download big picture image sent via payload, fallback on default");
        }

        /*
         * Color
         */
        int color;

        Integer metaDataPushColor = getMetaDataPushColor(context);
        if (metaDataPushColor != null) {
            color = metaDataPushColor;
        } else {
            color = PushModuleProvider.get().getNotificationColor();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && color == PushModule.NO_COLOR) {
            color = getAppPrimaryColor(context);
        }

        /*
         * Sound
         */
        Uri customSound = PushModuleProvider.get().getSound();
        if (customSound != null) {
            // If we've got a soundOverride, we have to remove sound from the defaults, or Android will not accept the sound override
            // Only do that if sounds are enabled by the user though
            // TODO: This can be way cleaner
            if ((defaults & Notification.DEFAULT_SOUND) != 0) {
                defaults &= ~Notification.DEFAULT_SOUND; // Remove the flag
            } else {
                // User disabled sounds, so act as if the override isn't set
                customSound = null;
            }
        }

        // Check if we have a custom scheme to open
        Intent launchIntent = null;
        if (batchData.hasScheme()) {
            try {
                if (batchData.isSchemeEmpty()) {
                    throw new NullPointerException("Received scheme is empty");
                }

                launchIntent = new Intent(context, BatchActionActivity.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchIntent.putExtra(BatchActionActivity.EXTRA_DEEPLINK_KEY, batchData.getScheme());
                launchIntent.setAction("batch_" + Long.toString(System.currentTimeMillis()));
            } catch (Exception e) {
                Logger.error(TAG, "Error while parsing deeplink", e);
                launchIntent = null;
            }
        }

        if (launchIntent == null) {
            launchIntent = DeeplinkHelper.getFallbackIntent(context);
            if (launchIntent == null) {
                Logger.error(
                    TAG,
                    "Batch could not detect the launch intent for the current package. Not displaying notification."
                );
                return;
            }

            Integer userFlags = PushModuleProvider.get().getAdditionalIntentFlags();
            if (userFlags != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    userFlags = userFlags | PendingIntent.FLAG_IMMUTABLE;
                }
                //noinspection WrongConstant
                launchIntent.addFlags(userFlags);
            }
        }

        IntentParser.putPushExtrasToIntent(extras, batchData, launchIntent);

        int contentIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            contentIntentFlags = contentIntentFlags | PendingIntent.FLAG_IMMUTABLE;
        }

        // UnspecifiedImmutableFlag is suppressed as the linter can't recognize our conditional
        // Android M immutability flag.

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, contentIntentFlags);

        // Random notification id generation
        int notificationId = (int) (Math.random() * Integer.MAX_VALUE);
        if (interceptor != null) {
            notificationId = interceptor.getPushNotificationId(context, notificationId, interceptorExtras);
        }

        // Add a dismiss receiver to dispatch the DISMISS event
        Intent dismissIntent = new Intent(context, BatchPushMessageDismissReceiver.class);
        IntentParser.putPushExtrasToIntent(extras, batchData, dismissIntent);

        int deleteIntentFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deleteIntentFlags = deleteIntentFlags | PendingIntent.FLAG_IMMUTABLE;
        }

        // UnspecifiedImmutableFlag is suppressed as the linter can't recognize our conditional
        // Android M immutability flag.

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent deleteIntent = PendingIntent.getBroadcast(
            context.getApplicationContext(),
            notificationId,
            dismissIntent,
            deleteIntentFlags
        );

        // Build notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setDefaults(defaults);
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(alert);
        builder.setContentTitle(title);
        builder.setContentText(alert);
        builder.setSmallIcon(smallIcon);
        builder.setContentIntent(contentIntent);
        builder.setDeleteIntent(deleteIntent);
        builder.setOnlyAlertOnce(true);
        builder.setAutoCancel(true);
        builder.setVisibility(batchData.getVisibility());

        if (customSound != null) {
            builder.setSound(customSound);
        }
        builder.setShowWhen(true);
        builder.setChannelId(channelID);

        if (color != PushModule.NO_COLOR) {
            builder.setColor(color);
        }

        // Prepare and apply the notification format
        InternalPushData.Format notificationFormat = batchData.getNotificationFormat();
        NotificationFormat notificationFormatImpl = null;
        if (notificationFormat == InternalPushData.Format.APEN) {
            if (APENFormat.isSupported()) {
                notificationFormatImpl = new APENFormat(title, alert, largeIcon, bigPicture);
                Logger.internal(TAG, "Using APEN (ex-news) format");
                if (!ReflectionHelper.optOutOfSmartReply(builder)) {
                    Logger.internal(TAG, "Cannot opt out of Smart Reply");
                }
            } else {
                Logger.internal(TAG, "News format has been requested but is unsupported");
            }
        }

        if (notificationFormatImpl == null) {
            notificationFormatImpl =
                new SystemFormat(
                    title,
                    alert,
                    largeIcon,
                    bigPicture,
                    batchData.shouldUseLegacyBigPictureIconBehaviour()
                );
        }

        notificationFormatImpl.applyArguments(batchData.getNotificationFormatArguments());

        applyNotificationFormat(context, notificationFormatImpl, builder);

        final List<BatchNotificationAction> payloadActions = payload.getActions();
        if (payloadActions != null && payloadActions.size() > 0) {
            final List<NotificationCompat.Action> actions = BatchNotificationAction.getSupportActions(
                context,
                payloadActions,
                payload,
                notificationId
            );

            for (NotificationCompat.Action action : actions) {
                builder.addAction(action);
            }
        }

        InternalPushData.Priority priority = batchData.getPriority();
        if (priority != null && priority != InternalPushData.Priority.UNDEFINED) {
            builder.setPriority(priority.toSupportPriority());
        }

        String group = batchData.getGroup();
        if (group != null) {
            builder.setGroup(group);
            builder.setGroupSummary(batchData.isGroupSummary());
        }

        if (interceptor != null) {
            Logger.info(TAG, "Calling developer's Notification Interceptor implementation");
            try {
                builder =
                    interceptor.getPushNotificationCompatBuilder(context, builder, interceptorExtras, notificationId);
                if (builder == null) {
                    Logger.info(TAG, "Aborting notification display: The push interceptor returned a null builder");
                    return;
                }
            } catch (RuntimeException e) {
                Logger.error(
                    TAG,
                    "Interceptor has thrown a runtime exception. Aborting notification display by rethrowing",
                    e
                );
                throw new NotificationInterceptorRuntimeException(e);
            }
        }

        Notification notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(Batch.NOTIFICATION_TAG, notificationId, notification);

        boolean canDisplay;
        // We don't use the batchChannelsManager because the dev can override channel ID in the interceptor
        String finalChannelId = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            finalChannelId = notification.getChannelId();
        }
        canDisplay = NotificationAuthorizationStatus.canAppShowNotificationsForChannel(context, finalChannelId);

        if (canDisplay) {
            if (batchData.getReceiptMode() == InternalPushData.ReceiptMode.DISPLAY) {
                DisplayReceiptModuleProvider.get().scheduleDisplayReceipt(context, batchData);
            }

            // We may come from background, try to reload dispatchers from manifest
            EventDispatcherModuleProvider.get().loadDispatcherFromContext(context);
            Batch.EventDispatcher.Payload eventPayload = new PushEventPayload(payload);
            EventDispatcherModuleProvider
                .get()
                .dispatchEvent(Batch.EventDispatcher.Type.NOTIFICATION_DISPLAY, eventPayload);
        } else {
            Logger.info("Batch.Push: notification can't be displayed, skipping event dispatcher...");
        }
    }

    /**
     * Try to send the landing to the app if:
     * - There's a landing in the payload
     * - The push payload allows us to
     * - The app is in the foreground
     * - Automatic mode is enabled
     *
     * @return Whether the push has been sent to the foreground or not. If this method returns true, you should usually stop showing the push
     */
    private static boolean trySendLandingToForegroundApp(Context context, Bundle extras, InternalPushData batchData) {
        if (!MessagingModuleProvider.get().shouldShowForegroundLandings()) {
            return false;
        }

        if (!MessagingModuleProvider.get().doesAppHaveRequiredLibraries(true)) {
            return false;
        }

        if (!MessagingModuleProvider.get().isInAutomaticMode()) {
            return false;
        }

        if (!RuntimeManagerProvider.get().isApplicationInForeground()) {
            Logger.internal(TAG, "Application is in background, not sending landing");
            return false;
        }

        final JSONObject messageJSON = batchData.getLandingMessage();

        if (messageJSON == null) {
            return false;
        }

        // Try to parse the message (not entirely) so that we're sure we can skip the display and don't miss the notification if we can't show it
        try {
            final Message message = PayloadParser.parseBasePayload(messageJSON);
            if (message != null) {
                // Workaround for banners: The service context will be unable to display it.
                if (message instanceof BannerMessage) {
                    Activity currentActivity = RuntimeManagerProvider.get().getActivity();
                    if (currentActivity != null) {
                        context = currentActivity;
                    } else {
                        return false;
                    }
                }

                MessagingModuleProvider
                    .get()
                    .displayMessage(context, new BatchLandingMessage(extras, messageJSON), false);
                return true;
            }
        } catch (PayloadParsingException | JSONException e) {
            Logger.internal(TAG, "Error while parsing the messaging payload. Not forwarding to foreground.", e);
        }

        return false;
    }

    private static Bitmap resizeLargeIcon(Context context, Bitmap customLargeIcon) {
        int height = 0;
        int width = 0;

        Resources res = context.getResources();
        height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

        return Bitmap.createScaledBitmap(customLargeIcon, width, height, false);
    }

    /**
     * Get the Notifications defaults
     *
     * @param context
     * @return {@link #DEFAULT_NO_NOTIFICATION} if no notifications should be shown, NotificationDefault otherwise
     */
    private static int getDefaults(Context context) {
        int defaults = 0;

        String param = ParametersProvider.get(context).get(ParameterKeys.PUSH_NOTIF_TYPE);
        if (param != null) {
            try {
                EnumSet<PushNotificationType> types = PushNotificationType.fromValue(Integer.parseInt(param));

                // Abort if only NONE or not contains ALERT
                if (
                    (types.size() == 1 && types.contains(PushNotificationType.NONE)) ||
                    !types.contains(PushNotificationType.ALERT)
                ) {
                    return DEFAULT_NO_NOTIFICATION;
                }

                if (types.contains(PushNotificationType.VIBRATE)) {
                    defaults |= Notification.DEFAULT_VIBRATE;
                }

                if (types.contains(PushNotificationType.SOUND)) {
                    defaults |= Notification.DEFAULT_SOUND;
                }

                if (types.contains(PushNotificationType.LIGHTS)) {
                    defaults |= Notification.DEFAULT_LIGHTS;
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Error while reading notification types. Fallback on ALL", e);
                defaults = Notification.DEFAULT_ALL;
            }
        } else {
            defaults = Notification.DEFAULT_ALL;
        }

        return defaults;
    }

    /**
     * Get the small icon res id from the metadata if any.
     * It first checks for Batch's resource, and then checks for Firebase's
     *
     * @param context
     * @return int if any, null otherwise
     */
    private static Integer getMetaDataSmallIconResId(Context context) {
        try {
            ApplicationInfo appInfo = context
                .getPackageManager()
                .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                int value = appInfo.metaData.getInt(CUSTOM_SMALL_ICON_METADATA_NAME);
                if (value != 0) {
                    return value;
                }
                // Try Firebase next
                value = appInfo.metaData.getInt(CUSTOM_SMALL_ICON_FIREBASE_METADATA_NAME);
                return value != 0 ? value : null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // if we can’t find it in the manifest, just return null
        } catch (Exception e) {
            Logger.error(TAG, "Error while parsing small icon meta data", e);
        }

        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static int getAppPrimaryColor(Context context) {
        TypedValue typedValue = new TypedValue();
        ContextThemeWrapper cw = new ContextThemeWrapper(context, context.getApplicationInfo().theme);
        Resources.Theme theme = cw.getTheme();
        if (theme != null && theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            return typedValue.data;
        }
        return PushModule.NO_COLOR;
    }

    /**
     * Get the push color from the metadata if any (for Unity)
     *
     * @param context
     * @return int if any, null otherwise
     */
    private static Integer getMetaDataPushColor(Context context) {
        try {
            ApplicationInfo appInfo = context
                .getPackageManager()
                .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                int value = appInfo.metaData.getInt(CUSTOM_COLOR_METADATA);
                return value != 0 ? value : null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // if we can’t find it in the manifest, just return null
        } catch (Exception e) {
            Logger.error(TAG, "Error while parsing small icon meta data", e);
        }

        return null;
    }

    private static void applyNotificationFormat(
        @NonNull Context context,
        @NonNull NotificationFormat format,
        @NonNull NotificationCompat.Builder builder
    ) {
        final String packageName = context.getPackageName();
        try {
            RemoteViews remoteView = format.generateCollapsedView(packageName);
            if (remoteView != null) {
                //TODO: check if we should override the heads up
                builder.setCustomContentView(remoteView);
            }
            remoteView = format.generateExpandedView(packageName);
            if (remoteView != null) {
                builder.setCustomBigContentView(remoteView);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Tried to instantiate remote views for format, but failed", e);
        }

        NotificationCompat.Style style = format.getSupportNotificationStyle();
        if (style != null) {
            builder.setStyle(style);
        }

        format.applyExtraBuilderConfiguration(builder);
    }
}
