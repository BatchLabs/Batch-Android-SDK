package com.batch.android.util;

import androidx.annotation.NonNull;
import com.batch.android.BatchDataCollectionConfig;
import java.util.Objects;

public class DataCollectionUtils {

    /**
     * Method to compare two batch privacy data config.
     * <p>
     * Globally used to check if a new config is different from the old one
     * So if one of the configs field is null, we consider it as equal because we consider a null field as an unchanged value from the previous configuration.
     * @param config The new config
     * @param config2 The old config
     * @return True if config are unchanged. Example:  null, true, false, et false, true, false will return true.
     */
    public static boolean areConfigsEquals(
        @NonNull BatchDataCollectionConfig config,
        @NonNull BatchDataCollectionConfig config2
    ) {
        boolean sameGeoip = true;
        boolean sameDeviceBrand = true;
        boolean sameDeviceModel = true;

        if (config.isGeoIpEnabled() != null && config2.isGeoIpEnabled() != null) {
            sameGeoip = Objects.equals(config.isGeoIpEnabled(), config2.isGeoIpEnabled());
        }

        if (config.isDeviceBrandEnabled() != null && config2.isDeviceBrandEnabled() != null) {
            sameDeviceBrand = Objects.equals(config.isDeviceBrandEnabled(), config2.isDeviceBrandEnabled());
        }

        if (config.isDeviceModelEnabled() != null && config2.isDeviceModelEnabled() != null) {
            sameDeviceModel = Objects.equals(config.isDeviceModelEnabled(), config2.isDeviceModelEnabled());
        }
        return sameGeoip && sameDeviceBrand && sameDeviceModel;
    }
}
