package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONObject;
import com.batch.android.json.JSONUtils;
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
        return JSONUtils.jsonObjectContainsValuesFrom(expected, argument);
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
