package com.batch.android;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.SystemParameterHelper;
import com.batch.android.core.SystemParameterShortName;
import com.batch.android.core.Webservice;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.json.JSONObject;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to easily get webservice parameters
 */
public class WebserviceParameterUtils {

    private static final String TAG = "WebserviceParameterUtils";

    /**
     * Get webservice ids parameters as map
     * @param context context
     * @return A Map of parameters to attach in a webservice
     */
    public static Map<String, Object> getWebserviceIdsAsMap(Context context) {
        return buildIds(context);
    }

    /**
     * Get webservice ids parameters as json object
     * @param context context
     * @return A JSONObject of parameters to attach in a webservice
     */
    public static JSONObject getWebserviceIdsAsJson(Context context) {
        return new JSONObject(buildIds(context));
    }

    /**
     * Build ids
     * @param context context
     * @return map of ids
     */
    private static Map<String, Object> buildIds(@NonNull Context context) {
        /*
         * Build ids object
         */
        Map<String, Object> ids = new HashMap<>();

        /*
         * Add modules data
         */
        try {
            ids.put("m_e", TrackerModuleProvider.get().getState());
            ids.put("m_p", PushModuleProvider.get().getState());
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding module parameters into parameters", e);
        }

        /*
         * Build all parameters
         */
        String baseIdsParameterString = ParametersProvider.get(context).get(ParameterKeys.WEBSERVICE_IDS_PARAMETERS);
        String[] baseParameters;
        if (!TextUtils.isEmpty(baseIdsParameterString)) {
            baseParameters = baseIdsParameterString.split(",");
        } else {
            baseParameters = new String[] {};
        }

        String advancedIdsParameterString = ParametersProvider
            .get(context)
            .get(ParameterKeys.WEBSERVICE_IDS_ADVANCED_PARAMETERS);
        String[] advancedParameters;
        if (!TextUtils.isEmpty(advancedIdsParameterString) && Batch.shouldUseAdvancedDeviceInformation()) {
            advancedParameters = advancedIdsParameterString.split(",");
        } else {
            advancedParameters = new String[] {};
        }

        String[] parameters;

        if (advancedParameters.length == 0) {
            parameters = baseParameters;
        } else if (baseParameters.length == 0) {
            parameters = advancedParameters;
        } else {
            parameters = Arrays.copyOf(baseParameters, baseParameters.length + advancedParameters.length);
            System.arraycopy(advancedParameters, 0, parameters, baseParameters.length, advancedParameters.length);
        }

        for (String parameter : parameters) {
            try {
                if (SystemParameterShortName.INSTALL_ID.shortName.equals(parameter)) {
                    Install install = Batch.getInstall();
                    String val = install.getInstallID();

                    if (val != null) {
                        ids.put(parameter, val);
                    }
                } else if (SystemParameterShortName.DEVICE_INSTALL_DATE.shortName.equals(parameter)) {
                    Install install = Batch.getInstall();
                    Date val = install.getInstallDate();

                    if (val != null) {
                        ids.put(parameter, Webservice.formatDate(val));
                    }
                } else if (SystemParameterShortName.SERVER_ID.shortName.equals(parameter)) {
                    String val = ParametersProvider.get(context).get(ParameterKeys.SERVER_ID_KEY);
                    if (val != null) {
                        ids.put(parameter, val);
                    }
                } else if (SystemParameterShortName.SESSION_ID.shortName.equals(parameter)) {
                    String val = Batch.getSessionID();
                    if (val != null) {
                        ids.put(parameter, val);
                    }
                } else if (SystemParameterShortName.CUSTOM_USER_ID.shortName.equals(parameter)) {
                    User user = Batch.getUser();
                    if (user != null) {
                        String customID = user.getCustomID();
                        if (customID != null) {
                            ids.put(parameter, customID);
                        }
                    }
                } else if (SystemParameterShortName.ADVERTISING_ID.shortName.equals(parameter)) {
                    if (Batch.shouldUseAdvertisingID()) {
                        AdvertisingID advertisingID = Batch.getAdvertisingID();
                        if (advertisingID != null) {
                            boolean isIdfaAvailable = advertisingID.isReady() && advertisingID.isNotNull();
                            if (isIdfaAvailable) {
                                ids.put(parameter, advertisingID.get());
                            }
                        }
                    }
                } else if (SystemParameterShortName.ADVERTISING_ID_OPTIN.shortName.equals(parameter)) {
                    AdvertisingID advertisingID = Batch.getAdvertisingID();
                    if (advertisingID != null) {
                        boolean isIdfaAvailable = advertisingID.isReady();
                        if (isIdfaAvailable) {
                            ids.put(parameter, !advertisingID.isLimited());
                        }
                    }
                } else if (SystemParameterShortName.BRIDGE_VERSION.shortName.equals(parameter)) {
                    String val = SystemParameterHelper.getBridgeVersion();

                    if (val != null && !val.isEmpty()) {
                        ids.put(parameter, val);
                    }
                } else if (SystemParameterShortName.PLUGIN_VERSION.shortName.equals(parameter)) {
                    String val = SystemParameterHelper.getPluginVersion();

                    if (val != null && !val.isEmpty()) {
                        ids.put(parameter, val);
                    }
                } else {
                    String val = SystemParameterHelper.getValue(parameter, context);
                    if (val != null) {
                        ids.put(parameter, val);
                    }
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Error while adding " + parameter + " post id", e);
            }
        }
        return ids;
    }
}
