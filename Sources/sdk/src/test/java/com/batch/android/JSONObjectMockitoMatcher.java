package com.batch.android;

import androidx.annotation.Nullable;
import com.batch.android.json.JSONObject;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

public class JSONObjectMockitoMatcher implements ArgumentMatcher<JSONObject> {

    @Nullable
    private final JSONObject expected;

    public JSONObjectMockitoMatcher(@Nullable JSONObject expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(JSONObject argument) {
        if (expected == null) {
            return argument == null;
        }

        if (argument == expected) {
            return true;
        }

        return expected.toString().equals(argument.toString());
    }

    public static JSONObject eq(@Nullable JSONObject expected) {
        return Mockito.argThat(new JSONObjectMockitoMatcher(expected));
    }
}
