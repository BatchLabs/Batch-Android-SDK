package com.batch.android;

import android.content.Context;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.UserModule;

/**
 * Object that encapsulate user data
 *
 * @hide
 */
final class User {

    /**
     * Saved context
     */
    private Context context;

    // -------------------------------------------->

    /**
     * @param context
     */
    protected User(Context context) {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        this.context = context.getApplicationContext();
    }

    // ------------------------------------------->

    /**
     * Set the new language
     *
     * @param language can be null
     */
    public void setLanguage(@Nullable String language) {
        if (language != null) {
            ParametersProvider.get(context).set(ParameterKeys.USER_PROFILE_LANGUAGE_KEY, language, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.USER_PROFILE_LANGUAGE_KEY);
        }
    }

    /**
     * Return the language if any, null otherwise
     *
     * @return
     */
    public String getLanguage() {
        return ParametersProvider.get(context).get(ParameterKeys.USER_PROFILE_LANGUAGE_KEY);
    }

    // ------------------------------------------->

    /**
     * Set the new region
     *
     * @param region can be null
     */
    public void setRegion(@Nullable String region) {
        if (region != null) {
            ParametersProvider.get(context).set(ParameterKeys.USER_PROFILE_REGION_KEY, region, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.USER_PROFILE_REGION_KEY);
        }
    }

    /**
     * Return the region if any, null otherwise
     *
     * @return
     */
    public String getRegion() {
        return ParametersProvider.get(context).get(ParameterKeys.USER_PROFILE_REGION_KEY);
    }

    // ------------------------------------------->

    /**
     * Set the new customID
     *
     * @param customID can be null
     */
    public void setCustomID(@Nullable String customID) {
        if (customID != null) {
            ParametersProvider.get(context).set(ParameterKeys.CUSTOM_ID, customID, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.CUSTOM_ID);
        }
    }

    /**
     * Return the custom ID if any, null otherwise
     *
     * @return
     */
    public String getCustomID() {
        return ParametersProvider.get(context).get(ParameterKeys.CUSTOM_ID);
    }

    /**
     * Set the new attribution id
     *
     * @param attributionID can be null
     */
    public void setAttributionID(@Nullable String attributionID) {
        if (attributionID != null) {
            ParametersProvider.get(context).set(ParameterKeys.ATTRIBUTION_ID, attributionID, true);
        } else {
            ParametersProvider.get(context).remove(ParameterKeys.ATTRIBUTION_ID);
        }
    }

    /**
     * Return the attribution ID if any, null otherwise
     *
     * @return
     */
    @Nullable
    public String getAttributionID() {
        return ParametersProvider.get(context).get(ParameterKeys.ATTRIBUTION_ID);
    }

    // ------------------------------------------->

    /**
     * Get the data version
     *
     * @return
     */
    public long getVersion() {
        String version = ParametersProvider.get(context).get(ParameterKeys.USER_DATA_VERSION);
        if (version == null) {
            return 1;
        }

        try {
            return Long.parseLong(version);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Get the data version and increment it
     *
     * @return
     */
    private synchronized long incrementVersion() {
        long newVersion = getVersion() + 1;
        ParametersProvider.get(context).set(ParameterKeys.USER_DATA_VERSION, Long.toString(newVersion), true);
        return newVersion;
    }

    public void sendAttributionIDChangedEvent() {
        final String attributionIdentifier = getAttributionID();
        JSONObject attributionParam = new JSONObject();
        try {
            if (attributionIdentifier != null) {
                attributionParam.put("attribution_id", attributionIdentifier);
            } else {
                attributionParam.put("attribution_id", JSONObject.NULL);
            }
            TrackerModuleProvider.get().track(InternalEvents.ATTRIBUTION_ID_CHANGED, attributionParam);
        } catch (JSONException e) {
            Logger.internal(UserModule.TAG, "Could not track " + InternalEvents.ATTRIBUTION_ID_CHANGED, e);
        }
    }

    public void sendChangeEvent() {
        try {
            final JSONObject params = new JSONObject();

            final String region = getRegion();
            if (region != null) {
                params.put("ure", region);
            }

            final String language = getLanguage();
            if (language != null) {
                params.put("ula", language);
            }

            final String customID = getCustomID();
            if (customID != null) {
                params.put("cus", customID);
            }
            params.put("upv", incrementVersion());
            TrackerModuleProvider.get().track(InternalEvents.PROFILE_CHANGED, params);
        } catch (JSONException e) {
            Logger.internal(UserModule.TAG, "Could not track " + InternalEvents.PROFILE_CHANGED, e);
        }
    }
}
