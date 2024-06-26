package com.batch.android;

import android.content.Context;
import android.text.TextUtils;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.Webservice;
import com.batch.android.core.WebserviceErrorCause;
import com.batch.android.core.systemparameters.SystemParameter;
import com.batch.android.core.systemparameters.SystemParameterHelper;
import com.batch.android.core.systemparameters.SystemParameterRegistry;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.SystemParameterRegistryProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONObject;
import com.batch.android.module.UserModule;
import com.batch.android.post.JSONPostDataProvider;
import com.batch.android.post.PostDataProvider;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Batch webservice that extends {@link Webservice} and can access {@link SystemParameterHelper}.<br>
 * Contains common methods to all webservices
 *
 * @hide
 */
public abstract class BatchWebservice extends Webservice {

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
    protected BatchWebservice(Context context, RequestType type, String baseURLFormat, String... parameters)
        throws MalformedURLException {
        super(context, type, baseURLFormat, addBatchApiKey(parameters));
        // Add property parameters if needed
        addPropertyParameters();
    }

    // -------------------------------------->

    @Override
    protected void addDefaultHeaders() {
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
    protected PostDataProvider<JSONObject> getPostDataProvider() {
        /*
         * Build root node
         */
        JSONObject body = new JSONObject();

        /*
         * Build ids object
         */
        JSONObject ids = WebserviceParameterUtils.getWebserviceIdsAsJson(applicationContext);

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
            UserModule userModule = UserModuleProvider.get();
            String language = userModule.getLanguage(applicationContext);
            String region = userModule.getRegion(applicationContext);

            if (!TextUtils.isEmpty(language) || !TextUtils.isEmpty(region)) {
                JSONObject upr = new JSONObject();

                if (!TextUtils.isEmpty(language)) {
                    upr.put("ula", language);
                }

                if (!TextUtils.isEmpty(region)) {
                    upr.put("ure", region);
                }

                upr.put("upv", userModule.getVersion(applicationContext));
                body.put("upr", upr);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding upr to body", e);
        }
        return new JSONPostDataProvider(body);
    }

    @Override
    protected void onRetry(WebserviceErrorCause cause) {
        super.onRetry(cause);

        // Increment retry count & put last failure
        retryCount++;
        lastFailureCause = cause;
    }

    // -------------------------------------->

    /**
     * Add the property parameters to get parameters
     */
    private void addPropertyParameters() {
        try {
            String parameterKey = getPropertyParameterKey();
            if (parameterKey != null && !parameterKey.isEmpty()) {
                // Retrieve value from the given key.
                String value = ParametersProvider.get(applicationContext).get(parameterKey);
                if (value == null || value.isEmpty()) {
                    return;
                }

                // Build the list.
                List<String> list = new ArrayList<>(Arrays.asList(value.split(",")));
                if (list.isEmpty()) {
                    return;
                }

                // Add available data.
                SystemParameterRegistry systemParameterRegistry = SystemParameterRegistryProvider.get(
                    applicationContext
                );
                for (String string : list) {
                    // Parameters
                    String data = ParametersProvider.get(applicationContext).get(string);

                    // System parameters
                    if (data == null || data.isEmpty()) {
                        SystemParameter parameter = systemParameterRegistry.getSystemParamByShortname(string);
                        if (parameter != null) {
                            data = parameter.getValue();
                        }
                    }

                    if (data == null || data.isEmpty()) {
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
    protected void handleParameters(JSONObject body) {
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
                        boolean save = parameterObject.has("s") && !parameterObject.isNull("s")
                            ? parameterObject.getBoolean("s")
                            : false;

                        ParametersProvider.get(applicationContext).set(name, value, save);
                    } catch (Exception e) {
                        Logger.internal(TAG, "Error while reading parameter #" + i, e);
                    }
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reading parameters into WS response", e);
        }
    }

    // ---------------------------------------->

    /**
     * Generate UserAgent header value
     *
     * @param applicationContext
     * @return String if succeed, null otherwise
     */
    private static String generateUserAgent(Context applicationContext) {
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

            return String.format(
                "%s%s/%s %s/%s (%s;%s)",
                pluginUA,
                Parameters.LIBRARY_BUNDLE,
                kdsVersion,
                bundleName,
                versionApp,
                deviceModel,
                OS
            );
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
    private static String generateAcceptLanguage(Context applicationContext) {
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
