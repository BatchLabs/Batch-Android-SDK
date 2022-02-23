package com.batch.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.InternalPushData;
import com.batch.android.json.JSONObject;
import com.google.firebase.messaging.RemoteMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience object to retrieve standardized Batch data out of a Batch Push intent.<br/>
 * This class does not have a public constructor.
 * You must use {@link #payloadFromBundle(Bundle)} or {@link #payloadFromReceiverIntent(Intent)}
 *
 */
@SuppressWarnings("WeakerAccess")
@PublicSDK
public class BatchPushPayload implements PushUserActionSource {

    /**
     * Represents a payload parsing exception.
     * This usually means that the given bundle or receiver intent doesn't contain data that BatchPushPayload understands.
     */
    @PublicSDK
    public static class ParsingException extends Exception {

        @SuppressWarnings("unused")
        public ParsingException() {}

        @SuppressWarnings("unused")
        public ParsingException(String message) {
            super(message);
        }

        @SuppressWarnings("unused")
        public ParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private InternalPushData internalPushData;

    private Bundle rawData;

    BatchPushPayload(@NonNull Bundle extras) throws ParsingException {
        internalPushData = InternalPushData.getPushDataForReceiverBundle(extras);

        if (internalPushData == null) {
            throw new ParsingException("Payload does not contain required Batch push data");
        }

        rawData = new Bundle(extras);
    }

    BatchPushPayload(@NonNull RemoteMessage message) throws ParsingException {
        rawData = BatchPushHelper.firebaseMessageToReceiverBundle(message);
        internalPushData = InternalPushData.getPushDataForReceiverBundle(rawData);

        if (internalPushData == null) {
            throw new ParsingException("Payload does not contain required Batch push data");
        }
    }

    //region: Static instantiations

    /**
     * Attempt to extract the Batch Push data contained within the specified Bundle.<br/>
     * This is usually the method you want to use when reading this payload from an activity opened by Batch,
     * or when you wrote an existing BatchPushPayload to a Bundle/Intent extras using {@link #writeToBundle(Bundle)} / {@link #writeToIntentExtras(Intent)}.
     *
     * @param bundle Bundle containing Batch Push data. Parsed data is read from the key defined by {@link Batch.Push#PAYLOAD_KEY}.
     * @return A BatchPushPayload instance.
     * @throws ParsingException Thrown if the argument doesn't contain valid Batch Push data
     */
    public static BatchPushPayload payloadFromBundle(@Nullable Bundle bundle) throws ParsingException {
        //noinspection ConstantConditions
        if (bundle == null) {
            throw new IllegalArgumentException("Extras cannot be null");
        }

        final Bundle payload = bundle.getBundle(Batch.Push.PAYLOAD_KEY);

        if (payload == null) {
            throw new ParsingException("Given bundle does not contain push information in Batch.Push.PAYLOAD_KEY");
        }

        return new BatchPushPayload(payload);
    }

    /**
     * Attempt to extract the Batch Push data contained within the specified Intent.<br/>
     * This is usually the method you want to use when reading this payload from a push broadcast receiver/service, NOT from an activity intent.
     * If you wrote an existing BatchPushPayload to a Bundle/Intent extras using {@link #writeToBundle(Bundle)} / {@link #writeToIntentExtras(Intent)}, you should rather use {@link #payloadFromBundle(Bundle)}
     *
     * @param intent Broadcast receiver intent containing Batch Push data.
     * @return A BatchPushPayload instance.
     * @throws ParsingException Thrown if the argument doesn't contain valid Batch Push data
     */
    public static BatchPushPayload payloadFromReceiverIntent(@NonNull Intent intent) throws ParsingException {
        //noinspection ConstantConditions
        if (intent == null) {
            throw new IllegalArgumentException("Intent cannot be null");
        }

        final Bundle extras = intent.getExtras();

        if (extras == null) {
            throw new IllegalArgumentException("Invalid intent");
        }

        return new BatchPushPayload(extras);
    }

    /**
     * Attempt to extract the Batch Push data contained within the specified Intent extras.<br/>
     * This is usually the method you want to use when reading this payload from a push broadcast receiver/service, NOT from an activity intent.
     * If you wrote an existing BatchPushPayload to a Bundle/Intent extras using {@link #writeToBundle(Bundle)} / {@link #writeToIntentExtras(Intent)}, you should rather use {@link #payloadFromBundle(Bundle)}
     *
     * @param extras Broadcast receiver intent's extras containing Batch Push data.
     * @return A BatchPushPayload instance.
     * @throws ParsingException Thrown if the argument doesn't contain valid Batch Push data
     */
    public static BatchPushPayload payloadFromReceiverExtras(@NonNull Bundle extras) throws ParsingException {
        //noinspection ConstantConditions
        if (extras == null) {
            throw new IllegalArgumentException("Extras cannot be null");
        }

        return new BatchPushPayload(extras);
    }

    /**
     * Attempt to extract the Batch Push data contained within the specified Firebase RemoteMessage.<br/>
     * This is usually the method you want to use when reading this payload from a FirebaseMessagingService implementation, NOT from an activity intent.
     * If you wrote an existing BatchPushPayload to a Bundle/Intent extras using {@link #writeToBundle(Bundle)} / {@link #writeToIntentExtras(Intent)}, you should rather use {@link #payloadFromBundle(Bundle)}
     *
     * @param remoteMessage Firebase remote message containing Batch Push data.
     * @return A BatchPushPayload instance.
     * @throws ParsingException Thrown if the argument doesn't contain valid Batch Push data
     */
    public static BatchPushPayload payloadFromFirebaseMessage(@NonNull RemoteMessage remoteMessage)
        throws ParsingException {
        //noinspection ConstantConditions
        if (remoteMessage == null) {
            throw new IllegalArgumentException("RemoteMessage cannot be null");
        }

        return new BatchPushPayload(remoteMessage);
    }

    //endregion

    //region: Serialization

    /**
     * Serialize this instance into a {@link Bundle}.<br/>
     * Note that you'll need to use {@link #payloadFromBundle(Bundle)} with the intent's extras to read it back.
     *
     * @param bundle Bundle instance to serialize this in
     */
    public void writeToBundle(@NonNull Bundle bundle) {
        //noinspection ConstantConditions
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }

        bundle.putBundle(Batch.Push.PAYLOAD_KEY, rawData);
    }

    /**
     * Serialize this instance into an {@link Intent}. Note that you'll need to use {@link #payloadFromBundle(Bundle)} with the intent's extras to read it back.<br/>
     * This method WILL NOT fill the intent in a format that {@link #payloadFromReceiverIntent(Intent)} understands.
     *
     * @param intent Intent instance to serialize this in
     */
    public void writeToIntentExtras(@NonNull Intent intent) {
        //noinspection ConstantConditions
        if (intent == null) {
            throw new IllegalArgumentException("Intent cannot be null");
        }

        intent.putExtra(Batch.Push.PAYLOAD_KEY, rawData);
    }

    //endregion

    //region: Getters

    /**
     * Does this push contains a deeplink
     *
     * @return true if this push contains a deeplink, false otherwise
     */
    public boolean hasDeeplink() {
        return internalPushData.hasScheme();
    }

    /**
     * Get the deeplink url contained in this push.<br>
     * You should always check if the push contains a deeplink using {@link #hasDeeplink()}
     *
     * @return the deeplink if any, null otherwise
     */
    public String getDeeplink() {
        return internalPushData.getScheme();
    }

    /**
     * Does this push contains a custom large icon
     *
     * @return true if this push contains a custom large icon to download, false otherwise
     */
    public boolean hasCustomLargeIcon() {
        return internalPushData.hasCustomBigIcon();
    }

    /**
     * Get the custom large icon url contained in this push.<br>
     * You should always check if the push contains a custom large icon using {@link #hasCustomLargeIcon()}.<br>
     * <br>
     * The url returned by this method is already optimized for the device, you have to download the image and use it in the notification
     *
     * @return the custom large icon url if any, null otherwise
     */
    public String getCustomLargeIconURL(Context context) {
        String url = internalPushData.getCustomBigIconURL();
        if (url == null) {
            return null;
        }

        return ImageDownloadWebservice.buildImageURL(context, url, internalPushData.getCustomBigIconAvailableDensity());
    }

    /**
     * Does this push contains a big picture
     *
     * @return true if this push contains a big picture to download, false otherwise
     */
    public boolean hasBigPicture() {
        return internalPushData.hasCustomBigImage();
    }

    /**
     * Get the big picture url contained in this push.<br>
     * You should always check if the push contains a big picture using {@link #hasBigPicture()}.<br>
     * <br>
     * The url returned by this method is already optimized for the device, you have to download the image and use it in the notification
     *
     * @return the big picture url if any, null otherwise
     */
    public String getBigPictureURL(Context context) {
        String url = internalPushData.getCustomBigImageURL();
        if (url == null) {
            return null;
        }

        return ImageDownloadWebservice.buildImageURL(
            context,
            url,
            internalPushData.getCustomBigImageAvailableDensity()
        );
    }

    /**
     * Whether the payload contains a landing message or not
     */
    public boolean hasLandingMessage() {
        return internalPushData.hasLandingMessage();
    }

    /**
     * Returns the landing message that's in this payload if there is one.
     *
     * @return the landing message, if there is one
     */
    public BatchMessage getLandingMessage() {
        final JSONObject landingMessage = internalPushData.getLandingMessage();
        if (landingMessage == null) {
            return null;
        }
        return new BatchLandingMessage(rawData, landingMessage);
    }

    /**
     * Get the actions associated with this push, if any.
     */
    public List<BatchNotificationAction> getActions() {
        List<BatchNotificationAction> actions = internalPushData.getActions();
        return actions != null ? actions : new ArrayList<>();
    }

    /**
     * Get the notification priority
     * <p>
     * This integer value is the same one as priorities defined in {@link androidx.core.app.NotificationCompat}
     *
     * @deprecated Since Android 8.0, "priority" became "importance", and is now set on the channel itself. You should infer the priority from the channel, using {@link #getChannel()}
     */
    @Deprecated
    public int getPriority() {
        return internalPushData.getPriority().toSupportPriority();
    }

    /**
     * Get the notification group name. Meant to be used with {@link androidx.core.app.NotificationCompat.Builder#setGroup(String)}
     *
     * @return Group name string, null if none
     */
    public String getGroup() {
        return internalPushData.getGroup();
    }

    /**
     * Get the notification group name. Meant to be used with {@link androidx.core.app.NotificationCompat.Builder#setGroupSummary(boolean)}
     *
     * @return Whether this notification should be a group summary or not
     */
    public boolean isGroupSummary() {
        return internalPushData.isGroupSummary();
    }

    /**
     * Get the desired notification channel. Meant to be used with {@link androidx.core.app.NotificationCompat.Builder#setChannelId(String)}
     *
     * @return The notification channel this push should be displayed on, if applicable. Can be null.
     */
    @Nullable
    public String getChannel() {
        return internalPushData.getChannel();
    }

    //endregion

    //region: PushUserActionSource

    /**
     * Get the raw push bundle
     */
    @Override
    public Bundle getPushBundle() {
        return new Bundle(rawData);
    }

    //endregion

    InternalPushData getInternalData() {
        return internalPushData;
    }
}
