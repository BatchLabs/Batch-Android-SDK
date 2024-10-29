package com.batch.android.core.systemparameters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.core.Webservice;
import com.batch.android.di.providers.DataCollectionModuleProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Singleton;

/**
 * Simple class to centralize all system parameters (native data)
 */
@Module
@Singleton
public class SystemParameterRegistry {

    /**
     * List of all natives
     */
    private final List<SystemParameter> parameters = new ArrayList<>(22);

    @Provide
    public static SystemParameterRegistry provide(@NonNull Context context) {
        return new SystemParameterRegistry(context);
    }

    /**
     * Constructor
     * Build all system parameters
     */
    private SystemParameterRegistry(@NonNull Context context) {
        // Non-watched system parameters
        SystemParameter installId = new SystemParameter(
            SystemParameterShortName.INSTALL_ID,
            Batch.User::getInstallationID
        );
        SystemParameter sessionId = new SystemParameter(SystemParameterShortName.SESSION_ID, Batch::getSessionID);

        SystemParameter customUserId = new SystemParameter(
            SystemParameterShortName.CUSTOM_USER_ID,
            () -> UserModuleProvider.get().getCustomID(context)
        );
        SystemParameter deviceDate = new SystemParameter(
            SystemParameterShortName.DEVICE_CURRENT_DATE,
            SystemParameterHelper::getDeviceDate
        );

        // Watched system parameter
        SystemParameter deviceInstallDate = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_INSTALL_DATE,
            () -> {
                Long date = SystemParameterHelper.getDeviceInstallDate(context);
                return date != null ? Webservice.formatDate(new Date(date)) : null;
            }
        );

        SystemParameter bundleName = new WatchedSystemParameter(
            context,
            SystemParameterShortName.APP_BUNDLE_ID,
            () -> SystemParameterHelper.getBundleName(context)
        );

        SystemParameter deviceLanguage = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_LANGUAGE,
            SystemParameterHelper::getDeviceLanguage
        );
        SystemParameter deviceRegion = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_REGION,
            SystemParameterHelper::getDeviceCountry
        );
        SystemParameter deviceTimezone = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_TIMEZONE,
            SystemParameterHelper::getDeviceTimezone
        );

        SystemParameter deviceType = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_MODEL,
            SystemParameterHelper::getDeviceModel,
            Boolean.TRUE.equals(DataCollectionModuleProvider.get().getDataCollectionConfig().isDeviceModelEnabled())
        );

        SystemParameter deviceBrand = new WatchedSystemParameter(
            context,
            SystemParameterShortName.DEVICE_BRAND,
            SystemParameterHelper::getDeviceBrand,
            Boolean.TRUE.equals(DataCollectionModuleProvider.get().getDataCollectionConfig().isDeviceBrandEnabled())
        );

        SystemParameter appVersion = new WatchedSystemParameter(
            context,
            SystemParameterShortName.APPLICATION_VERSION,
            () -> SystemParameterHelper.getAppVersion(context)
        );
        SystemParameter appBuildNumber = new WatchedSystemParameter(
            context,
            SystemParameterShortName.APPLICATION_CODE,
            () -> String.valueOf(SystemParameterHelper.getAppVersionCode(context))
        );

        SystemParameter osVersion = new WatchedSystemParameter(
            context,
            SystemParameterShortName.OS_VERSION,
            SystemParameterHelper::getOSVersion
        );
        SystemParameter osSdkLevel = new WatchedSystemParameter(
            context,
            SystemParameterShortName.OS_SDK_LEVEL,
            SystemParameterHelper::getOSSdkLevel
        );

        SystemParameter sdkApiLevel = new WatchedSystemParameter(
            context,
            SystemParameterShortName.SDK_API_LEVEL,
            SystemParameterHelper::getSdkApiLevel
        );
        SystemParameter messagingApiLevel = new WatchedSystemParameter(
            context,
            SystemParameterShortName.SDK_MESSAGING_API_LEVEL,
            SystemParameterHelper::getSdkMessagingApiLevel
        );

        SystemParameter firstInstallDate = new WatchedSystemParameter(
            context,
            SystemParameterShortName.FIRST_INSTALL_DATE,
            () -> {
                Long firstDate = SystemParameterHelper.getFirstInstallDate(context);
                return firstDate != null ? Webservice.formatDate(new Date(firstDate)) : null;
            }
        );
        SystemParameter lastUpdateDate = new WatchedSystemParameter(
            context,
            SystemParameterShortName.LAST_UPDATE_DATE,
            () -> {
                Long updateDate = SystemParameterHelper.getLastUpdateDate(context);
                return updateDate != null ? Webservice.formatDate(new Date(updateDate)) : null;
            }
        );

        SystemParameter bridgeVersion = new WatchedSystemParameter(
            context,
            SystemParameterShortName.BRIDGE_VERSION,
            SystemParameterHelper::getBridgeVersion
        );
        SystemParameter pluginVersion = new WatchedSystemParameter(
            context,
            SystemParameterShortName.PLUGIN_VERSION,
            SystemParameterHelper::getPluginVersion
        );

        parameters.add(installId);
        parameters.add(deviceInstallDate);
        parameters.add(bundleName);
        parameters.add(sessionId);
        parameters.add(customUserId);
        parameters.add(deviceLanguage);
        parameters.add(deviceRegion);
        parameters.add(deviceTimezone);
        parameters.add(deviceType);
        parameters.add(deviceDate);
        parameters.add(deviceBrand);
        parameters.add(appVersion);
        parameters.add(appBuildNumber);
        parameters.add(osVersion);
        parameters.add(osSdkLevel);
        parameters.add(sdkApiLevel);
        parameters.add(messagingApiLevel);
        parameters.add(firstInstallDate);
        parameters.add(lastUpdateDate);
        parameters.add(bridgeVersion);
        parameters.add(pluginVersion);
    }

    /**
     * Get all system parameters
     * @return All system parameters
     */
    public List<SystemParameter> getParameters() {
        return parameters;
    }

    /**
     * Get all watched parameters
     * <p>
     * Watched parameter mean value is persisted from shared preferences and detect changes.
     * @return watched parameters
     */
    public List<WatchedSystemParameter> getWatchedParameters() {
        List<WatchedSystemParameter> syncedParameters = new ArrayList<>(17);
        for (SystemParameter parameter : this.parameters) {
            if (parameter instanceof WatchedSystemParameter) {
                syncedParameters.add((WatchedSystemParameter) parameter);
            }
        }
        return syncedParameters;
    }

    /**
     * Get a specific system parameter object by its value
     *
     * @param shortname the short name parameter (eg: "di" for install_id)
     * @return The system parameter associated to the given shortname or null.
     */
    @Nullable
    public SystemParameter getSystemParamByShortname(String shortname) {
        for (SystemParameter parameter : this.parameters) {
            if (parameter.getShortName().shortName.equals(shortname)) {
                return parameter;
            }
        }
        return null;
    }
}
