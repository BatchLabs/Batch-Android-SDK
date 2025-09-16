package com.batch.android.core;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BuildConfig;
import com.batch.android.di.providers.KVUserPreferencesStorageProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Parameters to get and set parameters
 *
 */
@Module
@Singleton
public final class Parameters {

    /**
     * Common part of the default crypt key base 64 encoded (4 char long) used to decrypt internal data
     * internal = local files
     */
    protected static final String COMMON_INTERNAL_CRYPT_BASE_KEY = "Rkt2Qg=="; //FKvB

    /**
     * Common part of the signature crypt key base 64 encoded (4 char long) used to decrypt external data
     * external = webservice data
     */
    protected static final String COMMON_EXTERNAL_CRYPT_SIGNATURE_KEY = "QiExXW9PdC8="; //B!1]oOt/

    /**
     * Common part of the default crypt key base 64 encoded (4 char long) used to decrypt external data
     * external = webservice data
     */
    protected static final String COMMON_EXTERNAL_CRYPT_BASE_KEY = "d2dIRA=="; //wgHD

    /**
     * Common part of the default crypt key base 64 encoded (4 char long) used to decrypt external data
     * external = webservice data
     */
    protected static final String COMMON_EXTERNAL_CRYPT_BASE_KEY_V2 = "amdmeA=="; //jgfx

    /**
     * Dev logs ? Set to false in production
     */
    public static final boolean ENABLE_DEV_LOGS = BuildConfig.ENABLE_DEBUG_LOGGER;

    /**
     * WS interceptor allowed ? Set to false in production
     */
    public static final boolean ENABLE_WS_INTERCEPTOR = BuildConfig.ENABLE_WS_INTERCEPTOR;

    /**
     * Name of the current version
     */
    public static final String SDK_VERSION = BuildConfig.SDK_VERSION;
    /**
     * API level of the sdk
     */
    public static final int API_LEVEL = BuildConfig.API_LEVEL;
    /**
     * Messaging API level of the sdk
     */
    public static final int MESSAGING_API_LEVEL = BuildConfig.MESSAGING_API_LEVEL;
    /**
     * Bundle of Batch
     */
    public static final String LIBRARY_BUNDLE = BuildConfig.LIBRARY_PACKAGE_NAME;
    /**
     * URL of the domain used in logs
     */
    public static final String DOMAIN_URL = "https://batch.com/";

    /**
     * Environement var to get Batch plugin version (to put it into User-Agent)
     */
    public static final String PLUGIN_VERSION_ENVIRONEMENT_VAR = "batch.plugin.version";
    /**
     * Environement var to get Batch bridge version (to put it into User-Agent)
     */
    public static final String BRIDGE_VERSION_ENVIRONEMENT_VAR = "batch.bridge.version";

    // -------------------------------------------------->

    /**
     * App parameters hard coded
     */
    private static final Map<String, String> appParameters;

    //@formatter:off

    static
    {
        appParameters = new HashMap<>();

        // Cryptor = EAS base 64
        appParameters.put(ParameterKeys.START_WS_READ_CRYPTORTYPE_KEY, 			"5");
        appParameters.put(ParameterKeys.START_WS_POST_CRYPTORTYPE_KEY, 			"5");
        appParameters.put(ParameterKeys.TRACKER_WS_READ_CRYPTORTYPE_KEY, 		"5");
        appParameters.put(ParameterKeys.TRACKER_WS_POST_CRYPTORTYPE_KEY, 		"5");
        appParameters.put(ParameterKeys.TRACKER_WS_RETRYCOUNT_KEY, 				"0");
        appParameters.put(ParameterKeys.PUSH_WS_READ_CRYPTORTYPE_KEY, 			"5");
        appParameters.put(ParameterKeys.PUSH_WS_POST_CRYPTORTYPE_KEY, 			"5");
        appParameters.put(ParameterKeys.ATTR_SEND_WS_READ_CRYPTORTYPE_KEY, 		"5");
        appParameters.put(ParameterKeys.ATTR_SEND_WS_POST_CRYPTORTYPE_KEY, 		"5");
        appParameters.put(ParameterKeys.ATTR_CHECK_WS_READ_CRYPTORTYPE_KEY, 	"5");
        appParameters.put(ParameterKeys.ATTR_CHECK_WS_POST_CRYPTORTYPE_KEY, 	"5");
        appParameters.put(ParameterKeys.INBOX_WS_READ_CRYPTORTYPE_KEY, 			"5");
        appParameters.put(ParameterKeys.INBOX_WS_POST_CRYPTORTYPE_KEY,          "5");
        appParameters.put(ParameterKeys.INBOX_WS_RETRYCOUNT_KEY, 				"0");
        appParameters.put(ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_READ_CRYPTORTYPE_KEY, 	"5");
        appParameters.put(ParameterKeys.ATTR_LOCAL_CAMPAIGNS_WS_POST_CRYPTORTYPE_KEY, 	"5");

        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_READ_CRYPTORTYPE_KEY, 	"5");
        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_POST_CRYPTORTYPE_KEY, 	"5");
        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_RETRYCOUNT_KEY, 	           "0");
        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_READ_TIMEOUT_KEY, 	    "1000");
        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_JIT_WS_CONNECT_TIMEOUT_KEY, 	"1000");

        appParameters.put(ParameterKeys.DISPLAY_RECEIPT_WS_CRYPTORTYPE_KEY,                "5");
        appParameters.put(ParameterKeys.DISPLAY_RECEIPT_WS_RETRYCOUNT_KEY, 		           "0");
        appParameters.put(ParameterKeys.METRIC_WS_RETRYCOUNT_KEY, 		                   "0");

        appParameters.put(ParameterKeys.LOCAL_CAMPAIGNS_WS_INITIAL_DELAY, 	"5");
        appParameters.put(ParameterKeys.EVENT_TRACKER_INITIAL_DELAY, 		"10000");
        appParameters.put(ParameterKeys.EVENT_TRACKER_MAX_DELAY, 	 		"120000");
        appParameters.put(ParameterKeys.EVENT_TRACKER_BATCH_QUANTITY, 		"20");
        appParameters.put(ParameterKeys.EVENT_TRACKER_EVENTS_LIMIT, 		"10000");
        appParameters.put(ParameterKeys.DEFAULT_CONNECT_TIMEOUT_KEY,		"10000");
        appParameters.put(ParameterKeys.DEFAULT_READ_TIMEOUT_KEY,			"10000");
        appParameters.put(ParameterKeys.DEFAULT_RETRY_NUMBER_KEY, 			"2");
        appParameters.put(ParameterKeys.TASK_EXECUTOR_MIN_POOL, 			"0");
        appParameters.put(ParameterKeys.TASK_EXECUTOR_MAX_POOL, 			"5");
        appParameters.put(ParameterKeys.TASK_EXECUTOR_THREADTTL, 			"1000");
        appParameters.put(ParameterKeys.SCHEME_CODE_PATTERN, 				"^batch[A-Za-z0-9]{4,}://unlock/code/([^/\\?]+)");
    }

    //@formatter:on

    // ------------------------------------------->

    /**
     * A prefix applied to every user parameter saved into the storage
     */
    public static final String PARAMETERS_KEY_PREFIX = "batch_parameter_";

    // ------------------------------------------->

    /**
     * Application context
     */
    private final Context applicationContext;

    /**
     * Cache parameters, lost at app close
     */
    private final Map<String, String> cacheParameters;

    // ------------------------------------------->

    /**
     * @param context The Android's context
     */
    public Parameters(@Nullable Context context) {
        if (context == null) {
            throw new NullPointerException("Null applicationContext");
        }

        // Store the application's context
        this.applicationContext = context.getApplicationContext();

        // Create empty cache parameters
        cacheParameters = new HashMap<>();
    }

    // ------------------------------------------->

    /**
     * Get parameter value
     *
     * @param key The key of the parameter
     * @return value if found, null otherwise
     */
    public String get(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }

        synchronized (cacheParameters) {
            String cacheValue = cacheParameters.get(key);
            if (cacheValue != null) {
                return cacheValue;
            }
        }

        String userValue = KVUserPreferencesStorageProvider.get(applicationContext).get(PARAMETERS_KEY_PREFIX + key);
        if (userValue != null) {
            return userValue;
        }

        return appParameters.get(key);
    }

    /**
     * Get parameter value.
     *
     * @param key The key of the parameter
     * @param fallback The fallback if no value found
     * @return value if found, failure otherwise.
     */
    public String get(String key, String fallback) {
        String value = get(key);
        if (value == null || value.length() == 0) {
            return fallback;
        }

        return value;
    }

    /**
     * Set the parameter
     *
     * @param key The key of the parameter
     * @param value the value of the parameter
     * @param save  if true, the value will be here at next app launch, it will be lost otherwise
     */
    public void set(@NonNull String key, @NonNull String value, boolean save) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }

        if (value == null) {
            throw new NullPointerException("Null value");
        }

        synchronized (cacheParameters) {
            cacheParameters.put(key, value);
        }

        if (save) {
            KVUserPreferencesStorageProvider.get(applicationContext).persist(PARAMETERS_KEY_PREFIX + key, value);
        }
    }

    /**
     * Set the parameter or remove it if null
     *
     * @param key The key of the parameter
     * @param value the value of the parameter
     * @param save  if true, the value will be here at next app launch, it will be lost otherwise
     */
    public void setOrRemove(@NonNull String key, @Nullable String value, boolean save) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }

        if (value == null) {
            this.remove(key);
            return;
        }

        synchronized (cacheParameters) {
            cacheParameters.put(key, value);
        }

        if (save) {
            KVUserPreferencesStorageProvider.get(applicationContext).persist(PARAMETERS_KEY_PREFIX + key, value);
        }
    }

    /**
     * Remove the value of the given parameter
     *
     * @param key The key of the parameter to remove
     */
    public void remove(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }

        synchronized (cacheParameters) {
            cacheParameters.remove(key);
        }

        KVUserPreferencesStorageProvider.get(applicationContext).remove(PARAMETERS_KEY_PREFIX + key);
    }

    /**
     * Clear all parameters
     */
    public void wipeData() {
        synchronized (cacheParameters) {
            cacheParameters.clear();
        }
        KVUserPreferencesStorageProvider.get(applicationContext).clear();
    }
}
