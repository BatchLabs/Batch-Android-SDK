package com.batch.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.event.EventAttributesValidator;
import com.batch.android.event.EventTypedAttribute;
import com.batch.android.user.AttributeType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Object holding attributes to be associated to an event
 * Keys should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
 */
@PublicSDK
public class BatchEventAttributes {

    /**
     * Reserved key used to set a label on events in the install centric data model
     * and activate compatibility flows
     */
    public static final String LABEL_KEY = "$label";

    /**
     * Reserved key used to set tags on events in the install centric data model
     * and activate compatibility flows
     */
    public static final String TAGS_KEY = "$tags";

    @NonNull
    private final Map<String, EventTypedAttribute> attributes;

    @Nullable
    private Set<String> tags;

    @Nullable
    private String label;

    public BatchEventAttributes() {
        attributes = new HashMap<>();
    }

    /**
     * @hide
     * Private getter
     */
    @NonNull
    public Map<String, EventTypedAttribute> getAttributes() {
        return attributes;
    }

    /**
     * @hide
     * Private getter
     */
    @Nullable
    public Set<String> getTags() {
        return tags;
    }

    /**
     * @hide
     * Private getter
     */
    @Nullable
    public String getLabel() {
        return label;
    }

    /**
     * Add a string attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value String value to add. Can't be longer than 64 characters, and can't be empty or null. For better results, you should trim/lowercase your strings, and use slugs when possible.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, @NonNull String value) {
        if (LABEL_KEY.equals(key)) {
            this.label = value;
            return this;
        }
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.STRING));
    }

    /**
     * Add an URL attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value URL value to add. Can't be longer than 2048 characters, and can't be empty or null.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, @NonNull URI value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.URL));
    }

    /**
     * Add a float attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Float value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, float value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.DOUBLE));
    }

    /**
     * Add a double attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Double value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, double value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.DOUBLE));
    }

    /**
     * Add a integer attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Integer value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, int value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.LONG));
    }

    /**
     * Add a long attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Long value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, long value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.LONG));
    }

    /**
     * Add a boolean attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Boolean value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, boolean value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.BOOL));
    }

    /**
     * Add a date attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Date value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, @NonNull Date value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value.getTime(), AttributeType.DATE));
    }

    /**
     * Add an object attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value BatchEventAttributes value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes put(@NonNull String key, @NonNull BatchEventAttributes value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(value, AttributeType.OBJECT));
    }

    /**
     * Add a list of object attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value A List of BatchEventAttributes value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes putObjectList(@NonNull String key, @NonNull List<BatchEventAttributes> value) {
        return this.putTypedAttribute(key, new EventTypedAttribute(new ArrayList<>(value), AttributeType.OBJECT_ARRAY));
    }

    /**
     * Add a list of string attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value A List of String value to add.
     * @return Same BatchEventAttributes instance, for chaining
     */
    public BatchEventAttributes putStringList(@NonNull String key, @NonNull List<String> value) {
        if (TAGS_KEY.equals(key)) {
            this.tags = new LinkedHashSet<>(value);
            return this;
        }
        return this.putTypedAttribute(key, new EventTypedAttribute(new ArrayList<>(value), AttributeType.STRING_ARRAY));
    }

    /**
     * Add EventTypedAttribute and normalize key
     *
     * @param key The key to use
     * @param attribute The attribute to add
     */
    private BatchEventAttributes putTypedAttribute(@NonNull String key, @NonNull EventTypedAttribute attribute) {
        attributes.put(key.toLowerCase(Locale.US), attribute);
        return this;
    }

    /**
     * Validate the event data.
     *
     * @return A list of human readable errors as strings if the event data does not validates successfully, An empty list if not. If the data does not validate, Batch will refuse to track an event with it.
     */
    public List<String> validateEventAttributes() {
        return EventAttributesValidator.computeValidationErrors(this);
    }
}
