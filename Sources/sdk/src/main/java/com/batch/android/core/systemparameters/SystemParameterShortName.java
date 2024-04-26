package com.batch.android.core.systemparameters;

/**
 * Enum of system parameter short names
 *
 */
public enum SystemParameterShortName {
    APP_BUNDLE_ID("bid", "app_bundle_id"),

    DEVICE_TIMEZONE("dtz", "device_timezone"),

    FIRST_INSTALL_DATE("fda", "first_installation_date"),

    LAST_UPDATE_DATE("lda", "last_installation_date"),

    DEVICE_BRAND("brd", "device_brand"),

    OS_SDK_LEVEL("sdk", "os_sdk_level"),

    DEVICE_TYPE("dty", "device_type"),

    DEVICE_LANGUAGE("dla", "device_language"),

    DEVICE_REGION("dre", "device_region"),

    DEVICE_CURRENT_DATE("da", null),

    /**
     * ID if the installation on the device
     */
    INSTALL_ID("di", null),
    /**
     * Date of the install of Batch's first launch
     */
    DEVICE_INSTALL_DATE("did", "device_installation_date"),

    /**
     * Session id, regenerated at each start
     */
    SESSION_ID("s", null),

    APPLICATION_VERSION("apv", "app_version"),

    APPLICATION_CODE("apc", "app_build_number"),

    OS_VERSION("osv", "os_version"),

    SDK_API_LEVEL("lvl", "sdk_api_level"),

    SDK_MESSAGING_API_LEVEL("mlvl", "sdk_m_api_level"),

    CUSTOM_USER_ID("cus", null),

    /**
     * Same bridge/plugin version as the user-agent
     */
    BRIDGE_VERSION("brv", "bridge_version"),

    PLUGIN_VERSION("plv", "plugin_version");

    /**
     * Unique short name.
     */
    public final String shortName;

    /**
     * Serialized name.
     */
    public final String serializedName;

    /**
     * Standard constructor.
     *
     * @param name The short name of the parameter
     * @param serializedName The serialized name used in webservice 'native data changed event)
     */
    SystemParameterShortName(String name, String serializedName) {
        shortName = name;
        this.serializedName = serializedName;
    }

    /**
     * Retrieve the Enum value from a string
     *
     * @param shortName The short name of the parameter
     * @return The SystemParameterShortName enum
     * @throws IllegalStateException if not found
     */
    public static SystemParameterShortName fromShortValue(String shortName) {
        if (shortName == null) {
            throw new NullPointerException("Null short name");
        }

        for (SystemParameterShortName param : values()) {
            if (param.shortName.equalsIgnoreCase(shortName)) {
                return param;
            }
        }

        throw new IllegalStateException("No system parameter found for short name : " + shortName);
    }
}
