package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class JSONObjectPartialMatcher implements ArgumentMatcher<JSONObject> {

    @Nullable
    private final JSONObject expected;

    public JSONObjectPartialMatcher(@Nullable JSONObject expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(JSONObject argument) {
        return isJSONObjectEqual(expected, argument);
    }

    private boolean isJSONObjectEqual(JSONObject expected, JSONObject actual) {
        if (expected == null) {
            return actual == null;
        }

        if (actual == expected) {
            return true;
        }
        for (String key : expected.keySet()) {
            try {
                if (!expected.get(key).equals(actual.get(key))) {
                    if (expected.get(key) instanceof JSONObject) {
                        return isJSONObjectEqual(expected.getJSONObject(key), actual.getJSONObject(key));
                    } else if (expected.get(key) instanceof JSONArray) {
                        //FIXME: This is shit but does the job as long we respect the order
                        return expected.get(key).toString().equals(actual.get(key).toString());
                    }
                    return false;
                }
            } catch (JSONException e) {
                return false;
            }
        }
        return true;
    }

    public static JSONObject eq(@Nullable JSONObject expected) {
        return Mockito.argThat(new JSONObjectPartialMatcher(expected));
    }

    @NonNull
    @Override
    public String toString() {
        return "JSONObjectPartialMatcher{" + "expected=" + expected + '}';
    }
}
