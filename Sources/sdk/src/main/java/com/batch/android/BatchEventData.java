package com.batch.android;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.TrackerModule;
import com.batch.android.user.AttributeType;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Object holding data to be associated to an event
 * Keys should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
 */
@PublicSDK
public class BatchEventData {

    private static final int MAXIMUM_VALUES = 15;
    private static final int MAXIMUM_TAGS = 10;
    private static final int MAXIMUM_STRING_LENGTH = 64;
    private static final int MAXIMUM_URL_LENGTH = 2048;

    private Map<String, TypedAttribute> attributes;
    private Set<String> tags;
    private boolean convertedFromLegacyAPI = false;

    public BatchEventData() {
        init();
    }

    BatchEventData(@NonNull JSONObject fromJSON) {
        init();
        convertedFromLegacyAPI = true;

        TreeSet<String> sortedLegacyKeys = new TreeSet<>((o1, o2) ->
            o1.toLowerCase(Locale.US).compareTo(o2.toLowerCase(Locale.US))
        );
        sortedLegacyKeys.addAll(fromJSON.keySet());

        for (String key : sortedLegacyKeys) {
            Object value = fromJSON.opt(key);
            if (value != null) {
                if (value instanceof String) {
                    put(key, (String) value);
                } else if (value instanceof Boolean) {
                    put(key, (Boolean) value);
                } else if (value instanceof Float) {
                    put(key, (Float) value);
                } else if (value instanceof Double) {
                    put(key, (Double) value);
                } else if (value instanceof Integer) {
                    put(key, (Integer) value);
                } else if (value instanceof Long) {
                    put(key, (Long) value);
                } else if (value instanceof URI) {
                    put(key, (URI) value);
                }
            }
        }
    }

    private void init() {
        attributes = new HashMap<>();
        tags = new HashSet<>();
    }

    Map<String, TypedAttribute> getAttributes() {
        return attributes;
    }

    Set<String> getTags() {
        return tags;
    }

    boolean getConvertedFromLegacyAPI() {
        return convertedFromLegacyAPI;
    }

    /**
     * Add a tag
     *
     * @param tag Tag to add. Can't be longer than 64 characters, and can't be empty or null. For better results, you should trim/lowercase your strings, and use slugs when possible.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData addTag(String tag) {
        if (tags.size() >= MAXIMUM_TAGS) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: Event data cannot hold more than 10 tags. Ignoring: '" + tag + "'"
            );
            return this;
        }

        if (enforceStringValue(tag)) {
            tags.add(tag.toLowerCase(Locale.US));
            return this;
        }

        return this;
    }

    /**
     * Add a string attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value String value to add. Can't be longer than 64 characters, and can't be empty or null. For better results, you should trim/lowercase your strings, and use slugs when possible.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, String value) {
        if (enforceAttributesCount(key) && enforceAttributeName(key) && enforceStringValue(value)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.STRING));
            return this;
        }
        return this;
    }

    /**
     * Add an URL attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value URL value to add. Can't be longer than 2048 characters, and can't be empty or null.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, URI value) {
        if (enforceAttributesCount(key) && enforceAttributeName(key) && enforceURIValue(value)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.URL));
            return this;
        }
        return this;
    }

    /**
     * Add a float attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Float value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, float value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.DOUBLE));
            return this;
        }
        return this;
    }

    /**
     * Add a double attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Double value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, double value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.DOUBLE));
            return this;
        }
        return this;
    }

    /**
     * Add a integer attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Integer value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, int value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.LONG));
            return this;
        }
        return this;
    }

    /**
     * Add a long attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Long value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, long value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.LONG));
            return this;
        }
        return this;
    }

    /**
     * Add a boolean attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Boolean value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, boolean value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value, AttributeType.BOOL));
            return this;
        }
        return this;
    }

    /**
     * Add a date attribute for the specified key
     *
     * @param key   Attribute key. Should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Date value to add.
     * @return Same BatchEventData instance, for chaining
     */
    public BatchEventData put(String key, Date value) {
        if (enforceAttributeName(key) && enforceAttributesCount(key) && enforceDateValue(value)) {
            attributes.put(normalizeKey(key), new TypedAttribute(value.getTime(), AttributeType.DATE));
            return this;
        }
        return this;
    }

    JSONObject toInternalJSON() throws JSONException {
        JSONObject obj = new JSONObject();
        JSONObject attributes = new JSONObject();

        TypedAttribute attribute;
        for (Map.Entry<String, TypedAttribute> entry : this.attributes.entrySet()) {
            attribute = entry.getValue();
            attributes.put(entry.getKey().toLowerCase(Locale.US) + "." + attribute.type.getTypeChar(), attribute.value);
        }

        obj.put("attributes", attributes);
        obj.put("tags", new JSONArray(tags));

        if (convertedFromLegacyAPI) {
            obj.put("converted", true);
        }

        return obj;
    }

    private boolean enforceAttributesCount(String key) {
        if (attributes.size() == MAXIMUM_VALUES && !attributes.containsKey(key)) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: Event data cannot hold more than 15 attributes. Ignoring attribute: '" + key + "'"
            );
            return false;
        }
        return true;
    }

    private boolean enforceStringValue(String value) {
        if (TextUtils.isEmpty(value)) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: Cannot add a null or empty string attribute/tag. Ignoring."
            );
            return false;
        }

        if (value.length() > MAXIMUM_STRING_LENGTH) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: String attributes and tags can't be longer than " +
                MAXIMUM_STRING_LENGTH +
                " characters. Ignoring."
            );
            return false;
        }

        return true;
    }

    private boolean enforceURIValue(URI value) {
        if (TextUtils.isEmpty(value.toString())) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: Cannot add a null or empty URL attribute/tag. Ignoring."
            );
            return false;
        }

        if (value.toString().length() > MAXIMUM_URL_LENGTH) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: URL attributes can't be longer than " + MAXIMUM_URL_LENGTH + " characters. Ignoring."
            );
            return false;
        }

        if (value.getScheme() == null || value.getAuthority() == null) {
            Logger.error(
                TrackerModule.TAG,
                "BatchEventData: URL attributes must follow the format 'scheme://[authority][path][?query][#fragment]'. Ignoring."
            );
            return false;
        }

        return true;
    }

    private boolean enforceDateValue(Date value) {
        if (value == null) {
            Logger.internal(TrackerModule.TAG, "BatchEventData: Cannot add a null date attribute/tag. Ignoring.");
            return false;
        }

        return true;
    }

    private boolean enforceAttributeName(String key) {
        if (TextUtils.isEmpty(key) || !BatchUserDataEditor.ATTR_KEY_PATTERN.matcher(key).matches()) {
            Logger.internal(
                TrackerModule.TAG,
                "BatchEventData: Invalid key. Please make sure that the key is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters. Ignoring value '" +
                key +
                "'."
            );
            return false;
        }

        return true;
    }

    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.US);
    }

    static class TypedAttribute {

        public Object value;
        public AttributeType type;

        TypedAttribute(Object value, AttributeType type) {
            this.value = value;
            this.type = type;
        }
    }
}
