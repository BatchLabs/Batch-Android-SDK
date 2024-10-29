package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.core.systemparameters.SystemParameter;
import com.batch.android.core.systemparameters.SystemParameterRegistry;
import com.batch.android.di.providers.DataCollectionModuleProvider;
import com.batch.android.di.providers.SystemParameterRegistryProvider;
import com.batch.android.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to easily get webservice parameters
 */
public class WebserviceParameterUtils {

    /**
     * Get webservice ids parameters as map
     * @param context context
     * @return A Map of parameters to attach in a webservice
     */
    public static Map<String, Object> getWebserviceIdsAsMap(@NonNull Context context) {
        return buildIds(context);
    }

    /**
     * Get webservice ids parameters as json object
     * @param context context
     * @return A JSONObject of parameters to attach in a webservice
     */
    public static JSONObject getWebserviceIdsAsJson(@NonNull Context context) {
        return new JSONObject(buildIds(context));
    }

    /**
     * Build ids
     * @param context context
     * @return map of ids
     */
    private static Map<String, Object> buildIds(@NonNull Context context) {
        Map<String, Object> ids = new HashMap<>();

        // Adding system parameters entries
        SystemParameterRegistry systemParameterRegistry = SystemParameterRegistryProvider.get(context);
        for (SystemParameter parameter : systemParameterRegistry.getParameters()) {
            // Ensure this data is allowed to be send
            if (parameter.isAllowed()) {
                String val = parameter.getValue();
                if (val != null && !val.isEmpty()) {
                    ids.put(parameter.getShortName().shortName, val);
                }
            }
        }
        // Adding data collection entry
        Map<String, Object> dataCollection = new HashMap<>();
        boolean geoipEnabled = Boolean.TRUE.equals(
            DataCollectionModuleProvider.get().getDataCollectionConfig().isGeoIpEnabled()
        );
        dataCollection.put("geoip", geoipEnabled);
        ids.put("data_collection", dataCollection);
        return ids;
    }
}
