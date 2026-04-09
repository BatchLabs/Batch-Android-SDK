package com.batch.android.profile;

import androidx.annotation.Nullable;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Collection;
import java.util.List;

/**
 * A simple class that wrap a String to handle usage of null to delete a value
 * since java doesn't have undefined equivalent.
 */
public class ProfileDeletableAttribute<T> {

    /**
     * The string value
     */
    @Nullable
    private T value;

    /**
     * If we should explicitly set null in json to delete the attribute
     */
    private boolean shouldDelete;

    public ProfileDeletableAttribute() {}

    /**
     * Constructor
     * @param value The string value. If null enforce the delete of the attribute on server-side
     */
    public ProfileDeletableAttribute(@Nullable T value) {
        this.value = value;
        this.shouldDelete = this.value == null;
    }

    /**
     * Get if we should explicitly set null in json to delete the attribute
     * @return If we should explicitly set null in json to delete the attribute
     */
    public boolean shouldDelete() {
        return shouldDelete;
    }

    /**
     * Get the string value
     * @return the string value. Can be null.
     */
    @Nullable
    public T getValue() {
        return value;
    }

    /**
     * Set the string value
     * @param value The value to set
     */
    public void setValue(@Nullable T value) {
        this.value = value;
        this.shouldDelete = this.value == null;
    }

    /**
     * Get a serialized json object of this class.
     * @return A serialized json object of this class.
     */
    @Nullable
    public Object getSerializedValue() throws JSONException {
        if (value == null && shouldDelete) {
            return JSONObject.NULL;
        } else if (value instanceof List) {
            return new JSONArray((Collection<?>) value);
        } else {
            return value;
        }
    }
}
