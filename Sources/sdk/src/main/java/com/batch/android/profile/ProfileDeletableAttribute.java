package com.batch.android.profile;

import androidx.annotation.Nullable;
import com.batch.android.json.JSONObject;

/**
 * A simple class that wrap a String to handle usage of null to delete a value
 * since java doesn't have undefined equivalent.
 */
public class ProfileDeletableAttribute {

    /**
     * The string value
     */
    @Nullable
    private final String value;

    /**
     * If we should explicitly set null in json to delete the attribute
     */
    private final boolean shouldDelete;

    /**
     * Constructor
     * @param value The string value. If null enforce the delete of the attribute on server-side
     */
    public ProfileDeletableAttribute(@Nullable String value) {
        this.value = value;
        shouldDelete = this.value == null;
    }

    /**
     * Get the string value
     * @return the string value. Can be null.
     */
    @Nullable
    public String getValue() {
        return value;
    }

    /**
     * Get a serialized json object of this class.
     * @return A serialized json object of this class.
     */
    @Nullable
    public Object getSerializedValue() {
        if (value == null && shouldDelete) {
            return JSONObject.NULL;
        } else {
            return value;
        }
    }
}
