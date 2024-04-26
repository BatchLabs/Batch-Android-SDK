package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;

/**
 *  Batch Automatic Data Collection related configuration.
 */
@PublicSDK
public class BatchDataCollectionConfig {

    /**
     * Editor interface to edit the BatchDataCollectionConfig
     */
    @PublicSDK
    public interface Editor {
        void edit(BatchDataCollectionConfig config);
    }

    /**
     * Whether Batch should resolve the user's region/location from the ip address.
     * Default: false
     */
    @Nullable
    private Boolean geoIPEnabled;

    /**
     * Whether Batch should send the device brand information.
     * Default: false
     */
    @Nullable
    private Boolean deviceBrandEnabled;

    /**
     * Whether Batch should send the device model information.
     * Default: false
     */
    @Nullable
    private Boolean deviceModelEnabled;

    /**
     * Set whether Batch can resolve the user's region/location from the ip address.
     *
     * @param geoIPEnabled Whether Batch can resolve the geoip.
     * @return This BatchDataCollectionConfig instance for method chaining
     */
    public BatchDataCollectionConfig setGeoIPEnabled(boolean geoIPEnabled) {
        this.geoIPEnabled = geoIPEnabled;
        return this;
    }

    /**
     * Set whether Batch should send the device brand information.
     *
     * @param deviceBrandEnabled Whether Batch can collect the device brand.
     * @return This BatchDataCollectionConfig instance for method chaining
     */
    public BatchDataCollectionConfig setDeviceBrandEnabled(boolean deviceBrandEnabled) {
        this.deviceBrandEnabled = deviceBrandEnabled;
        return this;
    }

    /**
     * Set whether Batch should send the device model information.
     *
     * @param deviceModelEnabled Whether Batch can collect the device model.
     * @return This BatchDataCollectionConfig instance for method chaining
     */
    public BatchDataCollectionConfig setDeviceModelEnabled(boolean deviceModelEnabled) {
        this.deviceModelEnabled = deviceModelEnabled;
        return this;
    }

    /**
     * Get whether the geoip is enabled to resolve the user's location/region on server side.
     *
     * @return whether the geoip is enabled to resolve the user's location/region.
     */
    @Nullable
    public Boolean isGeoIpEnabled() {
        return geoIPEnabled;
    }

    /**
     * Get whether the device brand collect is enabled (null mean unchanged from last modification or default value : false).
     *
     * @return Whether the device brand collect is enabled t.
     */
    @Nullable
    public Boolean isDeviceBrandEnabled() {
        return deviceBrandEnabled;
    }

    /**
     * Get whether the device model collect is enabled (null mean unchanged from last modification or default value : false).
     *
     * @return Whether the device model collect is enabled.
     */
    @Nullable
    public Boolean isDeviceModelEnabled() {
        return deviceModelEnabled;
    }

    /**
     * To String method
     * @return A string representation of the data collection config.
     */
    @NonNull
    @Override
    public String toString() {
        return (
            "BatchDataCollectionConfig{" +
            "geoIPEnabled=" +
            geoIPEnabled +
            ", deviceBrandEnabled=" +
            deviceBrandEnabled +
            ", deviceModelEnabled=" +
            deviceModelEnabled +
            '}'
        );
    }
}
