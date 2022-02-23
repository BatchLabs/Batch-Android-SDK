package com.batch.android.core;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiscoveryServiceHelper {

    private static final String TAG = "DiscoveryServiceHelper";

    public static List<String> getComponentNames(
        final Context context,
        Class<? extends Service> discoveryService,
        String sentinelValue,
        String keyPrefix
    ) {
        Bundle metadata = getMetadata(context, discoveryService);
        if (metadata == null) {
            return Collections.emptyList();
        }

        List<String> registrarNames = new ArrayList<>();
        for (String key : metadata.keySet()) {
            Object rawValue = metadata.get(key);
            if (sentinelValue.equals(rawValue) && key.startsWith(keyPrefix)) {
                registrarNames.add(key.substring(keyPrefix.length()));
            }
        }
        return registrarNames;
    }

    private static Bundle getMetadata(final Context context, Class<? extends Service> discoveryService) {
        try {
            PackageManager manager = context.getPackageManager();
            if (manager == null) {
                Logger.internal(TAG, "Context has no PackageManager.");
                return null;
            }

            ServiceInfo info = manager.getServiceInfo(
                new ComponentName(context, discoveryService),
                PackageManager.GET_META_DATA
            );
            if (info == null) {
                Logger.internal(TAG, discoveryService.getSimpleName() + " has no service info.");
                return null;
            }
            return info.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.error(TAG, "Application info not found.", e);
            return null;
        }
    }
}
