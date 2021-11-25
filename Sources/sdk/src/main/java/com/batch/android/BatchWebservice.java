package com.batch.android;

import android.content.Context;
import android.text.TextUtils;

import com.batch.android.WebserviceMetrics.Metric;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.SystemParameterHelper;
import com.batch.android.core.SystemParameterShortName;
import com.batch.android.core.Webservice;
import com.batch.android.core.WebserviceErrorCause;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.di.providers.WebserviceMetricsProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONObject;
import com.batch.android.post.JSONPostDataProvider;
import com.batch.android.post.PostDataProvider;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Batch webservice that extends {@link Webservice} and can access {@link SystemParameterHelper}.<br>
 * Contains common methods to all webservices
 *
 * @hide
 */
abstract public class BatchWebservice extends Webservice
{
    private static final String TAG = "BatchWebservice";

    /**
     * Number of retry done for this query
     */
    protected int retryCount = 0;
    /**
     * Cause of the last try failure
     */
    protected WebserviceErrorCause lastFailureCause;

// -------------------------------------->

    /**
     * @param context
     * @param baseURLFormat an url to format with api key (ex : http://sample.com/%s/sample)
     * @throws MalformedURLException
     */
    protected BatchWebservice(Context context,
                              RequestType type,
                              String baseURLFormat,
                              String... parameters) throws MalformedURLException
    {
        super(context, type, baseURLFormat, addBatchApiKey(parameters));

        // Add property parameters if needeed
        addPropertyParameters();
    }

// -------------------------------------->

    /**
     * Prepend the API Key into the url parameters
     *
     * @param parameters
     * @return the same parameters with Batch key preprended
     */
    private static String[] addBatchApiKey(String[] parameters)
    {
        final String[] retParams = new String[parameters.length + 1];
        retParams[0] = Batch.getAPIKey();
        System.arraycopy(parameters, 0, retParams, 1, parameters.length);
        return retParams;
    }

// -------------------------------------->

    @Override
    protected void addDefaultHeaders()
    {
        super.addDefaultHeaders();

        /*
         * User Agent
         */
        String UA = generateUserAgent(applicationContext);
        if (UA != null) {
            headers.put("UserAgent", UA);
            headers.put("x-UserAgent", UA);
        }

        /*
         * Accept Language
         */
        String AL = generateAcceptLanguage(applicationContext);
        if (AL != null) {
            headers.put("Accept-Language", AL);
        }

        /*
         * Retry count
         */
        if (retryCount > 0) {
            headers.put("X-RetryCount", Integer.toString(retryCount));
        }
    }

    @Override
    protected PostDataProvider<JSONObject> getPostDataProvider()
    {
        /*
         * Build root node
         */
        JSONObject body = new JSONObject();

        /*
         * Build ids object
         */
        JSONObject ids = new JSONObject();

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
        String baseIdsParameterString = ParametersProvider.get(applicationContext).get(
                ParameterKeys.WEBSERVICE_IDS_PARAMETERS);
        String[] baseParameters;
        if (!TextUtils.isEmpty(baseIdsParameterString)) {
            baseParameters = baseIdsParameterString.split(",");
        } else {
            baseParameters = new String[]{};
        }

        String advancedIdsParameterString = ParametersProvider.get(applicationContext).get(
                ParameterKeys.WEBSERVICE_IDS_ADVANCED_PARAMETERS);
        String[] advancedParameters;
        if (!TextUtils.isEmpty(advancedIdsParameterString) && Batch.shouldUseAdvancedDeviceInformation()) {
            advancedParameters = advancedIdsParameterString.split(",");
        } else {
            advancedParameters = new String[]{};
        }

        String[] parameters;

        if (advancedParameters.length == 0) {
            parameters = baseParameters;
        } else if (baseParameters.length == 0) {
            parameters = advancedParameters;
        } else {
            parameters = Arrays.copyOf(baseParameters,
                    baseParameters.length + advancedParameters.length);
            System.arraycopy(advancedParameters,
                    0,
                    parameters,
                    baseParameters.length,
                    advancedParameters.length);
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
                    String val = ParametersProvider.get(applicationContext).get(
                            ParameterKeys.SERVER_ID_KEY);
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

                        boolean isIdfaAvailable = advertisingID.isReady() && advertisingID.isNotNull();
                        if (isIdfaAvailable) {
                            ids.put(parameter, advertisingID.get());
                        }
                    }
                } else if (SystemParameterShortName.ADVERTISING_ID_OPTIN.shortName.equals(parameter)) {
                    AdvertisingID advertisingID = Batch.getAdvertisingID();

                    boolean isIdfaAvailable = advertisingID.isReady();
                    if (isIdfaAvailable) {
                        ids.put(parameter, !advertisingID.isLimited());
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
                    String val = SystemParameterHelper.getValue(parameter, applicationContext);
                    if (val != null) {
                        ids.put(parameter, val);
                    }
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Error while adding " + parameter + " post id", e);
            }
        }

        /*
         * Add ids object to post params
         */
        try {
            body.put("ids", ids);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding ids object to global post params", e);
        }

        try {
            body.put("rc", retryCount);

            if (lastFailureCause != null) {
                JSONObject failureJson = new JSONObject();

                failureJson.put("cause", lastFailureCause.toString());

                body.put("lastFail", failureJson);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding retry count data to global post params", e);
        }

        /*
         * Add user data if any
         */
        try {
            User user = new User(applicationContext);
            String language = user.getLanguage();
            String region = user.getRegion();

            if (!TextUtils.isEmpty(language) || !TextUtils.isEmpty(region)) {
                JSONObject upr = new JSONObject();

                if (!TextUtils.isEmpty(language)) {
                    upr.put("ula", language);
                }

                if (!TextUtils.isEmpty(region)) {
                    upr.put("ure", region);
                }

                upr.put("upv", user.getVersion());
                body.put("upr", upr);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding upr to body", e);
        }

        /*
         * Add metrics if any
         */
        try {
            Map<String, Metric> metrics = WebserviceMetricsProvider.get().getMetrics();
            if (metrics != null && !metrics.isEmpty()) {
                JSONArray metricsJSON = new JSONArray();

                for (String wsShortName : metrics.keySet()) {
                    Metric metric = metrics.get(wsShortName);

                    JSONObject metricJSON = new JSONObject();
                    metricJSON.put("u", wsShortName);
                    metricJSON.put("s", metric.success);
                    metricJSON.put("t", metric.time);

                    metricsJSON.put(metricJSON);
                }

                body.put("metrics", metricsJSON);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding metrics to the body", e);
        }

        return new JSONPostDataProvider(body);
    }

    @Override
    protected void onRetry(WebserviceErrorCause cause)
    {
        super.onRetry(cause);

        // Increment retry count & put last failure
        retryCount++;
        lastFailureCause = cause;
    }

// -------------------------------------->

    /**
     * Add the property parameters to get parameters
     */
    private void addPropertyParameters()
    {
        try {
            String parameterKey = getPropertyParameterKey();
            if (parameterKey != null && parameterKey.length() > 0) {
                // Retrieve value from the given key.
                String value = ParametersProvider.get(applicationContext).get(
                        parameterKey);
                if (value == null || value.length() == 0) {
                    return;
                }

                // Build the list.
                List<String> list = new ArrayList<>(Arrays.asList(value.split(",")));
                if (list == null || list.size() == 0) {
                    return;
                }

                // Add available data.
                for (String string : list) {
                    // Parameters
                    String data = ParametersProvider.get(applicationContext).get(string);

                    // System parameters
                    if (data == null || data.length() == 0) {
                        data = SystemParameterHelper.getValue(string, applicationContext);
                    }

                    if (data == null || data.length() == 0) {
                        Logger.internal(TAG, "Unable to find parameter value for key " + string);
                        continue;
                    }

                    addGetParameter(string, data);
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, "Error while building property parameters", e);
        }
    }

    /**
     * Get property parameter key to add parameter to the get chain
     *
     * @return property key or null if no property is wanted for this webservice
     */
    protected abstract String getPropertyParameterKey();


// ---------------------------------------->

    /**
     * Read parameters from ws response
     *
     * @param body
     */
    protected void handleParameters(JSONObject body)
    {
        if (body == null) {
            throw new NullPointerException("Null body json");
        }

        try {
            if (body.has("parameters") && !body.isNull("parameters")) {
                JSONArray parametersArray = body.getJSONArray("parameters");

                if (parametersArray.length() <= 0) {
                    return;
                }

                for (int i = 0; i < parametersArray.length(); i++) {
                    // Serialize parameter and register it
                    try {
                        JSONObject parameterObject = parametersArray.getJSONObject(i);

                        String name = parameterObject.getString("n");
                        String value = parameterObject.getString("v");
                        boolean save = parameterObject.has("s") && !parameterObject.isNull("s") ? parameterObject.getBoolean(
                                "s") : false;

                        ParametersProvider.get(applicationContext).set(name,
                                value,
                                save);
                    } catch (Exception e) {
                        Logger.internal(TAG, "Error while reading parameter #" + i, e);
                    }
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading parameters into WS response", e);
        }
    }

    /**
     * Read server id from ws response
     *
     * @param body
     */
    protected void handleServerID(JSONObject body)
    {
        if (body == null) {
            throw new NullPointerException("Null body json");
        }

        try {
            if (body.has("i") && !body.isNull("i")) {
                ParametersProvider.get(applicationContext).set(ParameterKeys.SERVER_ID_KEY,
                        body.getString("i"),
                        true);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading server id into WS response", e);
        }
    }

// ---------------------------------------->

    /**
     * Generate UserAgent header value
     *
     * @param applicationContext
     * @return String if succeed, null otherwise
     */
    private static String generateUserAgent(Context applicationContext)
    {
        try {
            String kdsVersion = Parameters.SDK_VERSION;
            String bundleName = SystemParameterHelper.getBundleName(applicationContext);
            String versionApp = SystemParameterHelper.getAppVersion(applicationContext);
            String OS = SystemParameterHelper.getOSVersion();
            String deviceModel = SystemParameterHelper.getDeviceModel();

            // Check if we are used by a Plugin + Bridge. If so, send their version.
            String pluginVersion = SystemParameterHelper.getPluginVersion();
            String bridgeVersion = SystemParameterHelper.getBridgeVersion();
            String pluginUA = String.format("%s %s", pluginVersion, bridgeVersion).trim();
            if (pluginUA.length() > 0) {
                pluginUA += " ";
            }

            return String.format("%s%s/%s %s/%s (%s;%s)",
                    pluginUA,
                    Parameters.LIBRARY_BUNDLE,
                    kdsVersion,
                    bundleName,
                    versionApp,
                    deviceModel,
                    OS);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while building User Agent header", e);
            return null;
        }
    }

    /**
     * Generate AcceptLanguage value
     *
     * @param applicationContext
     * @return
     */
    private static String generateAcceptLanguage(Context applicationContext)
    {
        try {
            String language = SystemParameterHelper.getDeviceLanguage();
            String region = SystemParameterHelper.getDeviceCountry();
            return String.format("%s-%s", language, region);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while building Accept Language header", e);
            return null;
        }
    }

}
