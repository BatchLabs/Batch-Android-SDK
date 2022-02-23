package com.batch.android.core;

/**
 * Enum of system parameter short names
 *
 */
public enum SystemParameterShortName {
    BUNDLE_NAME("bid"),

    DEVICE_TIMEZONE("dtz"),

    FIRST_INSTALL_DATE("fda"),

    LAST_UPDATE_DATE("lda"),

    BRAND("brd"),

    SDK_LEVEL("sdk"),

    DEVICE_TYPE("dty"),

    DEVICE_LANGUAGE("dla"),

    DEVICE_REGION("dre"),

    DEVICE_DATE("da"),

    /**
     * ID if the installation on the device
     */
    INSTALL_ID("di"),
    /**
     * Date of the install of Batch's first launch
     */
    DEVICE_INSTALL_DATE("did"),

    /**
     * ID of the installation on the server
     */
    SERVER_ID("i"),

    ADVERTISING_ID("idv"),

    /**
     * Session id, regenerated at each start
     */
    SESSION_ID("s"),

    /**
     * Can use identifier
     */
    ADVERTISING_ID_OPTIN("cifa"),

    APPLICATION_VERSION("apv"),

    APPLICATION_CODE("apc"),

    OS_VERSION("osv"),

    SIM_OPERATOR_NAME("son"),

    SIM_OPERATOR("sop"),

    SIM_COUNTRY("sco"),

    NETWORK_NAME("ntn"),

    NETWORK_COUNTRY("ntc"),

    ROAMING("roa"),

    API_LEVEL("lvl"),

    MESSAGING_API_LEVEL("mlvl"),

    CUSTOM_USER_ID("cus"),

    /**
     * Same bridge/plugin version as the useragent
     */
    BRIDGE_VERSION("brv"),

    PLUGIN_VERSION("plv"),

    SCREEN_WIDTH("sw"),

    SCREEN_HEIGHT("sh"),

    SCREEN_ORIENTATION("so"),

    NETWORK_KIND("nkd");

    /**
     * Unique short name.
     */
    public String shortName;

    /**
     * Standad constructor.
     *
     * @param name
     */
    SystemParameterShortName(String name) {
        shortName = name;
    }

    /**
     * Retrieve the Enum value from a string
     *
     * @param shortName
     * @return
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
