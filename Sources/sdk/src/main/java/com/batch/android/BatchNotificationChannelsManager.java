package com.batch.android;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.module.PushModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;

/**
 * BatchNotificationChannelsManager manages how Batch interacts with Android 8.0 (API 26).
 * It allows you to control which default notification channel Batch will use, how it is named, or
 * to provide your own channel per notification
 */
@PublicSDK
@Module
@Singleton
public final class BatchNotificationChannelsManager {

    //region: SDK Internal

    private BatchNotificationChannelsManager(PushModule pushModule) {
        this.pushModule = pushModule;
    }

    @Provide
    public static BatchNotificationChannelsManager provide() {
        return new BatchNotificationChannelsManager(PushModuleProvider.get());
    }

    @Nullable
    private String channelOverride = null;

    @Nullable
    private ChannelNameProvider channelNameProvider = null;

    @Nullable
    private NotificationChannelIdInterceptor channelIdInterceptor = null;

    private PushModule pushModule;

    @NonNull
    String getChannelId(@Nullable BatchPushPayload payload) {
        String channelId = DEFAULT_CHANNEL_ID;

        if (channelOverride != null) {
            channelId = channelOverride;
        }

        if (payload != null && channelIdInterceptor != null) {
            try {
                String interceptedChannelId = channelIdInterceptor.getChannelId(payload, channelId);
                if (interceptedChannelId != null) {
                    channelId = interceptedChannelId;
                }
            } catch (Exception e) {
                Logger.error(
                    PushModule.TAG,
                    "An exception occurred while calling the specified channel id interceptor. Falling back on '" +
                    channelId +
                    "'",
                    e
                );
            }
        }

        return channelId;
    }

    private boolean isChannelIdOverridden() {
        return channelOverride != null;
    }

    void registerBatchChannelIfNeeded(Context c) {
        if (isChannelIdOverridden()) {
            Logger.internal(PushModule.TAG, "Channel ID overridden, not registering Batch's channel.");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Logger.internal(PushModule.TAG, "Registering default Batch notification channel");
            //try {
            @SuppressLint("WrongConstant")
            NotificationChannel channel = new NotificationChannel(
                DEFAULT_CHANNEL_ID,
                getBatchChannelName(),
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.enableVibration(true);
            Uri customSound = pushModule.getSound();
            if (customSound != null) {
                channel.setSound(
                    customSound,
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                );
            }

            NotificationManager notificationManager = c.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            //TODO: Report this bug to google
            /*
            } catch (RemoteException e) {
				Logger.error(PushModule.TAG, "Could not register Batch's notification channel to Android. Notifications might not be displayed.", e);
			}*/
        }
    }

    String getBatchChannelName() {
        String name = null;
        if (channelNameProvider != null) {
            try {
                name = channelNameProvider.getDefaultChannelName();
            } catch (Exception e) {
                Logger.error(
                    PushModule.TAG,
                    "An exception occurred while calling the specified channel id interceptor. Falling back on the default name.",
                    e
                );
            }
        }

        if (TextUtils.isEmpty(name)) {
            name = "Notifications"; //TODO: translate
        }

        return name;
    }

    //endregion

    //region: Public methods and fields

    public static final String DEFAULT_CHANNEL_ID = "_BATCHSDK_DEFAULT";

    /**
     * Overrides the default notification channel that Batch will use for notifications.<br/>
     * By default, Batch will manage its own channel, identified by {@link #DEFAULT_CHANNEL_ID}.<br/>
     * After calling this method with a non-null channelId, Batch will stop trying to register its default channel
     * <p>
     * Note: Please make sure that you've registered this channel using {@link android.app.NotificationManager#createNotificationChannel(NotificationChannel)} or
     * {@link android.app.NotificationManager#createNotificationChannelGroup(NotificationChannelGroup)}<br/><br/>
     * <p>
     * Set to 'null' to revert to the default channel.
     */
    public void setChannelIdOverride(@Nullable String channelId) {
        channelOverride = channelId;
    }

    /**
     * Used to set a channel name provider, allowing you to change Batch's default channel name.<br/>
     * <p>
     * This is the string that the user will see in their device settings, so you should translate it.<br/>
     * Batch will call you back on your provider at least once, to get the channel name, and then will
     * call you on every locale change, to make sure that the Android OS has the right translation.<br/><br/>
     * <p>
     * If your provider throws an exception, Batch will use its default name.<br/><br/>
     * <p>
     * If you simply want to use a string resource, use {@link #setChannelName(Context, int)}.<br/>
     * <p>
     * Set to 'null' to remove the provider and use Batch's default one.<br/>
     */
    public void setChannelNameProvider(@Nullable ChannelNameProvider provider) {
        channelNameProvider = provider;
    }

    /**
     * Used to set a channel name provider, allowing you to change Batch's default channel name.<br/>
     * <p>
     * This is the string that the user will see in their device settings, so you should translate it properly.<br/>
     * Calling this is the equivalent of calling {@link #setChannelNameProvider(ChannelNameProvider)} with a {@link StringResChannelNameProvider} instance.<br/><br/>
     * <p>
     * Calling this method will remove any provider set with {@link #setChannelNameProvider(ChannelNameProvider)}
     */
    public void setChannelName(@NonNull Context context, @StringRes int channelNameResourceId) {
        setChannelNameProvider(new StringResChannelNameProvider(context, channelNameResourceId));
    }

    /**
     * Used to set a channel id interceptor, allowing you to override the Channel ID of a notification, per notification.<br/>
     * You will be called on this interceptor before displaying notifications, unless you're in manual mode and don't use Batch to display the notification.<br/><br/>
     * <p>
     * The provider might be called on ANY API Level<br/><br/>
     * <p>
     * If your provider throws an exception, Batch will use its default name.<br/>
     */
    public void setChannelIdInterceptor(@Nullable NotificationChannelIdInterceptor interceptor) {
        channelIdInterceptor = interceptor;
    }

    /**
     * Opens the notification channel settings system UI for Batch's default channel.
     * <p>
     * Will do nothing on API < 26
     *
     * @param context Your context
     * @return Whether the system settings have been opened or not. Always false on API < 26
     */
    public static boolean openSystemChannelSettings(@NonNull Context context) {
        return openSystemChannelSettings(context, DEFAULT_CHANNEL_ID);
    }

    /**
     * Opens the notification channel settings system UI for the specified channel
     * <p>
     * Will do nothing on API < 26
     *
     * @param context   Your context. Can't be null.
     * @param channelId The channel to open this. Can't be null.
     * @return Whether the system settings have been opened or not. Always false on API < 26
     */
    public static boolean openSystemChannelSettings(@NonNull Context context, @NonNull String channelId) {
        //noinspection ConstantConditions
        if (context == null) {
            throw new IllegalArgumentException("Context is mandatory");
        }
        //noinspection ConstantConditions
        if (channelId == null) {
            throw new IllegalArgumentException("ChannelId is mandatory");
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    //endregion

    /**
     * Interface describing a channel name provider.
     * <p>
     * Implementing that interface allows Batch to call you back on locale change, to update your
     * channel name translation.
     */
    @PublicSDK
    public interface ChannelNameProvider {
        /**
         * The user-facing channel name to return to the system.<br/>
         * If this method throws an exception, Batch will use its default name.
         */
        @NonNull
        String getDefaultChannelName();
    }

    /**
     * Implementation of {@link ChannelNameProvider} using a String resource to automatically fetch
     * the right translation.
     */
    @PublicSDK
    public static final class StringResChannelNameProvider implements ChannelNameProvider {

        private Context context;

        private int resId;

        public StringResChannelNameProvider(@NonNull Context context, @StringRes int channelNameResourceId) {
            this.context = context.getApplicationContext();
            resId = channelNameResourceId;
        }

        @NonNull
        public String getDefaultChannelName() {
            return context.getResources().getString(resId);
        }
    }

    /**
     * Interface describing a channel ID interceptor for notifications displayed by Batch
     */
    @PublicSDK
    public interface NotificationChannelIdInterceptor {
        /**
         * Method that should return the notification channel id that should be
         * Note that it might be called on any API level, even ones lower than {@link android.os.Build.VERSION_CODES#O}.
         * <p>
         * If this method throws an exception, Batch will use the value of deductedChannelId.
         *
         * @param payload           Full push payload. Read your custom payload as string values, using {@link Bundle#getString(String)} on {@link BatchPushPayload#getPushBundle()}
         * @param deductedChannelId Channel ID
         * @return The channel ID to use for this notification. If you return null, deductedChannelId will be used.
         */
        @Nullable
        String getChannelId(@NonNull BatchPushPayload payload, String deductedChannelId);
    }
}
