package com.batch.android.eventdispatcher;

import androidx.annotation.NonNull;
import com.batch.android.BatchEventDispatcher;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Simple class to serialize event dispatchers
 */
public class DispatcherSerializer {

    public static final String FIREBASE_DISPATCHER_NAME = "firebase";
    public static final String AT_INTERNET_DISPATCHER_NAME = "at_internet";
    public static final String MIXPANEL_DISPATCHER_NAME = "mixpanel";
    public static final String GOOGLE_ANALYTICS_DISPATCHER_NAME = "google_analytics";
    public static final String BATCH_PLUGINS_DISPATCHER_NAME = "batch_plugins";

    private static final String CUSTOM_DISPATCHER_NAME = "other";

    /**
     * List of dispatchers handled by Batch
     */
    private static final List<String> knownDispatchers = Arrays.asList(
        FIREBASE_DISPATCHER_NAME,
        AT_INTERNET_DISPATCHER_NAME,
        MIXPANEL_DISPATCHER_NAME,
        GOOGLE_ANALYTICS_DISPATCHER_NAME,
        BATCH_PLUGINS_DISPATCHER_NAME
    );

    /**
     * Serialize a list of dispatchers
     *
     * @param dispatchers dispatchers to serialize
     * @return json object
     */
    @NonNull
    public static JSONObject serialize(@NonNull Set<BatchEventDispatcher> dispatchers) {
        JSONObject json = new JSONObject();
        for (BatchEventDispatcher dispatcher : dispatchers) {
            try {
                if (dispatcher.getName() == null) {
                    continue;
                }
                String name = knownDispatchers.contains(dispatcher.getName())
                    ? dispatcher.getName()
                    : CUSTOM_DISPATCHER_NAME;
                json.put(name, dispatcher.getVersion());
            } catch (JSONException | AbstractMethodError ignored) {}
        }
        return json;
    }
}
