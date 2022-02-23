package com.batch.android.json;

import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper json methods that don't belong in {@link JSON}
 */
@Keep
public class JSONHelper {

    private JSONHelper() {}

    /**
     * Transforms {@link org.json.JSONObject} and {@link org.json.JSONArray} to {@link java.util.Map} and {@link java.lang.Iterable}
     * If transformation is not needed, returns the original object. Useful for JSON deserialization
     *
     * @param object Object to transform
     * @return Transformed object, if necessary
     * @throws JSONException
     */
    public static Object jsonObjectToObject(Object object) throws JSONException {
        if (object instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) object);
        } else if (object instanceof JSONArray) {
            return jsonArrayToArray((JSONArray) object);
        } else {
            return object;
        }
    }

    public static Map<String, Object> jsonObjectToMap(JSONObject object) throws JSONException {
        final Map<String, Object> map = new HashMap<>();
        final Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put(key, jsonObjectToObject(object.get(key)));
        }
        return map;
    }

    public static List<Object> jsonArrayToArray(JSONArray array) throws JSONException {
        final List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(jsonObjectToObject(array.get(i)));
        }
        return list;
    }
}
