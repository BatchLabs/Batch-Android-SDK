package com.batch.android.json;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class JSONUtils {

    /**
     * Helper method that checks whether values from a JSONObject are contained into an other.
     * <p>
     * This method is NOT an equals method !
     *
     * @param expected The expected json object for comparison
     * @param actual The actual json object to check
     * @return true if all the values from the expected json object are contained in the actual json object
     */
    public static boolean jsonObjectContainsValuesFrom(@Nullable JSONObject expected, @Nullable JSONObject actual) {
        if (expected == null) {
            return actual == null;
        }

        if (actual == expected) {
            return true;
        }

        if (actual == null) {
            return false;
        }

        for (String key : expected.keySet()) {
            try {
                if (!actual.has(key)) {
                    return false;
                }
                if (!expected.get(key).equals(actual.get(key))) {
                    boolean found = false;
                    if (expected.get(key) instanceof JSONObject) {
                        found = jsonObjectContainsValuesFrom(expected.getJSONObject(key), actual.getJSONObject(key));
                    } else if (expected.get(key) instanceof JSONArray) {
                        found = jsonArrayContainsValuesFrom(expected.getJSONArray(key), actual.getJSONArray(key));
                    }
                    if (!found) {
                        return false;
                    }
                }
            } catch (JSONException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method that checks whether values from a JSONArray are contained into an other.
     * <p>
     * This method is NOT an equals method !
     *
     * @param expected The expected json array for comparison
     * @param actual The actual json array to check
     * @return true if all the values from the expected json array are contained in the actual json array
     */
    public static boolean jsonArrayContainsValuesFrom(@Nullable JSONArray expected, @Nullable JSONArray actual) {
        if (expected == null) {
            return actual == null;
        }

        if (actual == null) {
            return false;
        }

        if (expected.length() > actual.length()) {
            return false;
        }

        List<Object> actualList = new ArrayList<>();
        for (int i = 0; i < actual.length(); i++) {
            try {
                actualList.add(actual.get(i));
            } catch (JSONException e) {
                return false;
            }
        }

        for (int i = 0; i < expected.length(); i++) {
            try {
                Object expectedValue = expected.get(i);
                boolean found = false;
                for (int j = 0; j < actualList.size(); j++) {
                    Object actualValue = actualList.get(j);
                    if (expectedValue instanceof JSONObject && actualValue instanceof JSONObject) {
                        if (jsonObjectContainsValuesFrom((JSONObject) expectedValue, (JSONObject) actualValue)) {
                            actualList.remove(j);
                            found = true;
                            break;
                        }
                    } else if (expectedValue instanceof JSONArray && actualValue instanceof JSONArray) {
                        if (jsonArrayContainsValuesFrom((JSONArray) expectedValue, (JSONArray) actualValue)) {
                            actualList.remove(j);
                            found = true;
                            break;
                        }
                    } else if (expectedValue.equals(actualValue)) {
                        actualList.remove(j);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            } catch (JSONException e) {
                return false;
            }
        }
        return true;
    }
}
