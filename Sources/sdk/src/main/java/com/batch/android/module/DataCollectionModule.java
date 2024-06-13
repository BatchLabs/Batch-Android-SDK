package com.batch.android.module;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchDataCollectionConfig;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.systemparameters.SystemParameter;
import com.batch.android.core.systemparameters.SystemParameterHelper;
import com.batch.android.core.systemparameters.SystemParameterRegistry;
import com.batch.android.core.systemparameters.SystemParameterShortName;
import com.batch.android.core.systemparameters.WatchedSystemParameter;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SystemParameterRegistryProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.util.DataCollectionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Collection Module
 */
@Module
@Singleton
public class DataCollectionModule extends BatchModule {

    private static final String TAG = "DataCollection";

    /**
     * The current data collection configuration
     */
    @NonNull
    private final BatchDataCollectionConfig dataCollectionConfig = new BatchDataCollectionConfig();

    /**
     * Data collection config in cache
     * Used to keep updates of the data collection config when the SDK isn't started yet.
     */
    @Nullable
    private BatchDataCollectionConfig cachedDataCollectionConfig;

    /**
     * DI access method
     *
     * @return A new instance of the DataCollection module
     */
    @Provide
    public static DataCollectionModule provide() {
        return new DataCollectionModule();
    }

    // region BatchModule
    /**
     * Get module identifier
     *
     * @return The module identifier
     */
    @Override
    public String getId() {
        return "datacollection";
    }

    /**
     * Get module state
     *
     * @return The module state (1 = enabled)
     */
    @Override
    public int getState() {
        return 1;
    }

    /**
     * Method called when the batch SDK received the context
     *
     * @param applicationContext The application's context
     */
    @Override
    public void batchContextBecameAvailable(@NonNull Context applicationContext) {
        super.batchContextBecameAvailable(applicationContext);
        synchronized (dataCollectionConfig) {
            // Load data collection config from shared pref
            loadDataCollectionConfig(applicationContext);

            // Check if we have a data collection config in cache
            if (cachedDataCollectionConfig != null) {
                Logger.internal(TAG, "Cached data config found, trying to send changes");
                try {
                    onDataCollectionConfigChanged(cachedDataCollectionConfig);
                } catch (JSONException e) {
                    Logger.error(TAG, "Failed sending data collection changed event", e);
                }
            }
        }
    }

    /**
     * Method called when Batch did start
     */
    @Override
    public void batchDidStart() {
        super.batchDidStart();
        Context context = RuntimeManagerProvider.get().getContext();
        if (context != null) {
            TaskExecutorProvider.get(context).submit(() -> this.systemParametersMayHaveChanged(context));
        }
    }

    // endregion

    /**
     * Check if some system parameter values have changed.
     *
     * @param context Android's context
     */
    private void systemParametersMayHaveChanged(@NonNull Context context) {
        SystemParameterRegistry registry = SystemParameterRegistryProvider.get(context);
        List<WatchedSystemParameter> parameters = registry.getWatchedParameters();
        List<WatchedSystemParameter> hasChangedParameters = new ArrayList<>();
        for (WatchedSystemParameter parameter : parameters) {
            if (parameter.hasChanged()) {
                if (parameter.isAllowed()) {
                    hasChangedParameters.add(parameter);
                }
            }
        }
        if (!hasChangedParameters.isEmpty()) {
            Logger.internal(TAG, "Some native data has changed, sending it.");
            try {
                sendNativeDataChangedEvent(SystemParameterHelper.serializeSystemParameters(hasChangedParameters));
            } catch (JSONException e) {
                Logger.error(TAG, "Some natives data has changed but the serialization failed.", e);
            }
        } else {
            Logger.internal(TAG, "No native detection change");
        }
    }

    /**
     * Update the current data collection config
     *
     * @param editor User's modification of the data collection config.
     */
    public void updateDataCollectionConfig(BatchDataCollectionConfig.Editor editor) {
        synchronized (dataCollectionConfig) {
            BatchDataCollectionConfig config = new BatchDataCollectionConfig();
            editor.edit(config);
            Logger.internal(TAG, "Updating automatic data collection configuration: ".concat(config.toString()));
            try {
                this.onDataCollectionConfigChanged(config);
            } catch (JSONException e) {
                Logger.error(TAG, "Failed sending data collection changed event", e);
            }
        }
    }

    /**
     * Get the current data collection config
     *
     * @return The data collection configuration
     */
    public BatchDataCollectionConfig getDataCollectionConfig() {
        synchronized (dataCollectionConfig) {
            return dataCollectionConfig;
        }
    }

    /**
     * Handle modification of the data collection configuration
     */
    private void onDataCollectionConfigChanged(@NonNull BatchDataCollectionConfig config) throws JSONException {
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.internal(TAG, "Context not available yet, caching config");
            // We do not have a context, we keep data collection config in cache.
            this.cachedDataCollectionConfig = config;
            return;
        }

        if (DataCollectionUtils.areConfigsEquals(config, this.dataCollectionConfig)) {
            // Do nothing is config hasn't changed
            Logger.internal(TAG, "No change detected for data collection config.");
            this.cachedDataCollectionConfig = null;
            return;
        }

        JSONObject params = new JSONObject();
        SystemParameterRegistry registry = SystemParameterRegistryProvider.get(context);

        // Check whether geoip has changed (null = unchanged)
        if (config.isGeoIpEnabled() != null && config.isGeoIpEnabled() != this.dataCollectionConfig.isGeoIpEnabled()) {
            params.put("geoip_resolution", config.isGeoIpEnabled());
        }

        // Check whether device brand has changed (null = unchanged)
        if (
            config.isDeviceBrandEnabled() != null &&
            config.isDeviceBrandEnabled() != this.dataCollectionConfig.isDeviceBrandEnabled()
        ) {
            SystemParameter deviceBrandParameter = registry.getSystemParamByShortname(
                SystemParameterShortName.DEVICE_BRAND.shortName
            );
            if (deviceBrandParameter != null) {
                deviceBrandParameter.setAllowed(Boolean.TRUE.equals(config.isDeviceBrandEnabled()));
                if (Boolean.FALSE.equals(config.isDeviceBrandEnabled())) {
                    params.put(deviceBrandParameter.getShortName().serializedName, JSONObject.NULL);
                } else {
                    params.put(deviceBrandParameter.getShortName().serializedName, deviceBrandParameter.getValue());
                }
            }
        }

        // Check whether device model has changed (null = unchanged)
        if (
            config.isDeviceModelEnabled() != null &&
            config.isDeviceModelEnabled() != this.dataCollectionConfig.isDeviceModelEnabled()
        ) {
            SystemParameter deviceModelParameter = registry.getSystemParamByShortname(
                SystemParameterShortName.DEVICE_MODEL.shortName
            );
            if (deviceModelParameter != null) {
                deviceModelParameter.setAllowed(Boolean.TRUE.equals(config.isDeviceModelEnabled()));
                if (Boolean.FALSE.equals(config.isDeviceModelEnabled())) {
                    params.put(deviceModelParameter.getShortName().serializedName, JSONObject.NULL);
                } else {
                    params.put(deviceModelParameter.getShortName().serializedName, deviceModelParameter.getValue());
                }
            }
        }
        // Send native data changed event
        this.sendNativeDataChangedEvent(params);

        // Update current config
        if (config.isDeviceBrandEnabled() != null) {
            this.dataCollectionConfig.setDeviceBrandEnabled(Boolean.TRUE.equals(config.isDeviceBrandEnabled()));
        }
        if (config.isDeviceModelEnabled() != null) {
            this.dataCollectionConfig.setDeviceModelEnabled(Boolean.TRUE.equals(config.isDeviceModelEnabled()));
        }
        if (config.isGeoIpEnabled() != null) {
            this.dataCollectionConfig.setGeoIPEnabled(Boolean.TRUE.equals(config.isGeoIpEnabled()));
        }

        // Persist current config
        this.persistDataCollectionConfig(context, dataCollectionConfig);

        // Delete cached config
        this.cachedDataCollectionConfig = null;
    }

    /**
     * Send a NATIVE_DATA_CHANGED event to the backend
     *
     * @param params The JSON parameters
     */
    private void sendNativeDataChangedEvent(JSONObject params) {
        TrackerModuleProvider.get().track(InternalEvents.NATIVE_DATA_CHANGED, params);
    }

    /**
     * Persist a data collection config in the shared preferences as a json string.
     *
     * @param context The context to access shared preferences
     * @param config The BatchDataCollectionConfig to persist
     */
    private void persistDataCollectionConfig(@NonNull Context context, @NonNull BatchDataCollectionConfig config) {
        JSONObject serializedDataCollectionConfig = new JSONObject();
        try {
            serializedDataCollectionConfig.put("geoip", config.isGeoIpEnabled());
            serializedDataCollectionConfig.put("deviceBrand", config.isDeviceBrandEnabled());
            serializedDataCollectionConfig.put("deviceModel", config.isDeviceModelEnabled());
            ParametersProvider
                .get(context)
                .set(ParameterKeys.DATA_COLLECTION_CONFIG_KEY, serializedDataCollectionConfig.toString(), true);
        } catch (JSONException e) {
            Logger.error(TAG, "Persisting data collection config has failed", e);
        }
    }

    /**
     * Load the last data collection configuration from the shared preferences
     *
     * @param context The context to access shared preferences
     */
    private void loadDataCollectionConfig(@NonNull Context context) {
        String serializedDataCollectionConfig = ParametersProvider
            .get(context)
            .get(ParameterKeys.DATA_COLLECTION_CONFIG_KEY);
        if (serializedDataCollectionConfig != null) {
            try {
                JSONObject jsonDataCollectionConfig = new JSONObject(serializedDataCollectionConfig);
                this.dataCollectionConfig.setGeoIPEnabled(jsonDataCollectionConfig.optBoolean("geoip"));
                this.dataCollectionConfig.setDeviceBrandEnabled(jsonDataCollectionConfig.optBoolean("deviceBrand"));
                this.dataCollectionConfig.setDeviceModelEnabled(jsonDataCollectionConfig.optBoolean("deviceModel"));
            } catch (JSONException e) {
                Logger.error(TAG, "Loading data collection config has failed", e);
                this.setDefaultDataCollectionConfig();
            }
        } else {
            // If no configuration found in shared pref, init with default values
            this.setDefaultDataCollectionConfig();
        }
    }

    /**
     * Set default values to the current data collection config.
     */
    private void setDefaultDataCollectionConfig() {
        this.dataCollectionConfig.setGeoIPEnabled(false);
        this.dataCollectionConfig.setDeviceBrandEnabled(false);
        this.dataCollectionConfig.setDeviceModelEnabled(false);
    }
}
