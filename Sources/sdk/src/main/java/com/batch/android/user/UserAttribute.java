package com.batch.android.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserAttribute {

    public Object value;
    public AttributeType type;

    public UserAttribute(@NonNull Object value, @NonNull AttributeType type) {
        this.value = value;
        this.type = type;
    }

    public static Map<String, Object> getServerMapRepresentation(Map<String, UserAttribute> attributes) {
        final Map<String, Object> representation = new HashMap<>();
        if (attributes == null) {
            return representation;
        }

        for (Map.Entry<String, UserAttribute> entry : attributes.entrySet()) {
            Object convertedValue = entry.getValue().value;

            if (convertedValue instanceof Date) {
                convertedValue = ((Date) convertedValue).getTime();
            }

            representation.put(entry.getKey().substring(2) + "." + entry.getValue().type.getTypeChar(), convertedValue);
        }

        return representation;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof UserAttribute)) {
            return false;
        }

        UserAttribute castedObj = (UserAttribute) obj;

        return this.type == castedObj.type && this.value.equals(castedObj.value);
    }

    @Override
    public String toString() {
        return "type:" + type.getTypeChar() + "' value: '" + value.toString() + "'";
    }
}
