package com.batch.android;

import android.content.Context;
import android.text.TextUtils;
import com.batch.android.annotation.PublicSDK;

/**
 * User profile that contains user specific targeting data
 *
 * @deprecated Please use Batch.User methods instead
 */
@PublicSDK
@Deprecated
public final class BatchUserProfile {

    /**
     * Saved context
     */
    private Context context;

    // --------------------------------------------->

    /**
     * @param context
     */
    BatchUserProfile(Context context) {
        if (context == null) {
            throw new NullPointerException("context==null");
        }
        this.context = context;
    }

    // ---------------------------------------------->

    /**
     * Set the language of this user.<br>
     * Setting this will affect targeting of this user, use it only if you know what you're doing.
     *
     * @param language a string that represent a locale language (ex: fr, en, pt, ru etc...)
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public BatchUserProfile setLanguage(String language) {
        Batch.User.editor().setLanguage(language).save(false);
        return this;
    }

    /**
     * Get the current language of this user.<br>
     * If you set a custom one using {@link #setLanguage(String)} this method will return this one
     * otherwise it will return the device default.
     *
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public String getLanguage() {
        return Batch.User.getLanguage(context);
    }

    /**
     * Did the user have a custom language
     *
     * @return
     */
    boolean hasCustomLanguage() {
        return !TextUtils.isEmpty(Batch.User.getLanguage(context));
    }

    /**
     * Set the region (country) of this user.<br>
     * Setting this will affect targeting of this user, use it only if you know what you're doing.
     *
     * @param region a string that represent a locale region (country) (ex: FR, US, BR, RU etc...)
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public BatchUserProfile setRegion(String region) {
        Batch.User.editor().setRegion(region).save(false);
        return this;
    }

    /**
     * Get the current region of this user.<br>
     * If you set a custom one using {@link #setRegion(String)} this method will return this one
     * otherwise it will return the device default.
     *
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public String getRegion() {
        return Batch.User.getRegion(context);
    }

    /**
     * Did the user have a custom region
     *
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    boolean hasCustomRegion() {
        return !TextUtils.isEmpty(Batch.User.getRegion(context));
    }

    /**
     * Set the custom user identifier to Batch.<br>
     * You should use this method if you have your own login system.<br>
     * <br>
     * <b>Be carefull</b> : Do not use it if you don't know what you are doing,
     * giving a bad custom user ID can result in failure into offer delivery and restore<br>
     *
     * @param customID
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public BatchUserProfile setCustomID(String customID) {
        Batch.User.editor().setIdentifier(customID).save(false);
        return this;
    }

    /**
     * Return the custom ID of the user if you specified any using the {@link #setCustomID(String)}
     *
     * @return customID if any, null otherwise
     * @deprecated Please use Batch.User methods instead
     */
    @Deprecated
    public String getCustomID() {
        return Batch.User.getIdentifier(context);
    }

    /**
     * Get the data version
     *
     * @return
     * @deprecated Please use Batch.User methods instead
     */
    long getVersion() {
        return new com.batch.android.User(context).getVersion();
    }
}
