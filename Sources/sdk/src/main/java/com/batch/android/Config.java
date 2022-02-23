package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * Class to build a configuration for Batch SDK
 *
 */
@PublicSDK
public final class Config {

    /**
     * The API key used for Batch
     */
    String apikey;
    /**
     * Should Batch use Advertising ID or not
     */
    boolean shouldUseAdvertisingID = true;
    /**
     * Should Batch use advanced device information or not
     */
    boolean shouldUseAdvancedDeviceInformation = true;
    /**
     * Should Batch use the Play Services Instance ID API (if available), or fallback to classic GCM.
     */
    boolean shouldUseGoogleInstanceID = true;

    /**
     * Should Batch send Logs to a 3rd party class
     */
    LoggerDelegate loggerDelegate = null;
    /**
     * Level of log Batch should use
     */
    LoggerLevel loggerLevel = LoggerLevel.INFO;

    // ----------------------------------------->

    /**
     * @param apikey
     */
    public Config(String apikey) {
        this.apikey = apikey;
    }

    // ----------------------------------------->

    /**
     * Method kept for compatibility: Batch will not use the Android ID in any situation
     *
     * @deprecated No replacement.
     */
    @Deprecated
    public Config setCanUseAndroidID(boolean canUse) {
        return this;
    }

    /**
     * Set if Batch can use AvertisingId (default = true)<br>
     * <br>
     * Setting this to false have a negative impact on offer delivery and restore<br>
     * You should only use it if you know what you are doing.
     *
     * @param canUse can Batch use AdvertisingID
     */
    public Config setCanUseAdvertisingID(boolean canUse) {
        shouldUseAdvertisingID = canUse;
        return this;
    }

    /**
     * Set if Batch can use advanced device identifiers (default = true)<br>
     * <br>
     * Advanced device identifiers include information about the device itself, but nothing that
     * directly identify the user, such as but not limited to:
     * - Device model
     * - Device brand
     * - Carrier name
     * <br>
     * Setting this to false have a negative impact on core Batch features</br>
     * You should only use it if you know what you are doing.
     * <p>
     * Note: Disabling this does not automatically disable Android ID/Advertising ID collection, use the
     * appropriate methods to control these.
     *
     * @param canUse Can Batch use advanced device information?
     */
    public Config setCanUseAdvancedDeviceInformation(boolean canUse) {
        shouldUseAdvancedDeviceInformation = canUse;
        return this;
    }

    /**
     * Set if Batch should send its logs to an object of yours (default = null)<br>
     * <br>
     * Be careful with your implementation: setting this can impact stability and performance<br>
     * You should only use it if you know what you are doing.
     *
     * @param delegate An object implementing {@link LoggerDelegate}
     */
    public Config setLoggerDelegate(LoggerDelegate delegate) {
        loggerDelegate = delegate;
        return this;
    }

    /**
     * Set the log level Batch should use
     *
     * @param level
     * @return
     */
    public Config setLoggerLevel(LoggerLevel level) {
        loggerLevel = level;
        return this;
    }

    /**
     * Set if Batch can use the Google Play Services Instance ID api (default = true)<br>
     * Setting this to false will make Batch fallback on the classic (and deprecated) GCM API.
     * <br>
     * You should only use it if you know what you are doing.
     *
     * @param canUse can Batch use the Instance ID
     * @deprecated Please switch to Firebase Cloud Messaging
     */
    @Deprecated
    public Config setCanUseInstanceID(boolean canUse) {
        shouldUseGoogleInstanceID = canUse;
        return this;
    }

    /**
     * Set if Batch should automatically register for pushes using FCM or GCM.
     * Note: This doesn't do anything anymore
     *
     * @param shouldAutomaticallyRegisterPush Doesn't do anything
     * @deprecated This feature isn't supported anymore
     */
    @Deprecated
    public Config setShouldAutomaticallyRegisterPush(boolean shouldAutomaticallyRegisterPush) {
        return this;
    }
}
