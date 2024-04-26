package com.batch.android.event;

import com.batch.android.BatchEventAttributes;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventAttributesSerializer {

    public static JSONObject serialize(BatchEventAttributes eventAttributes) throws JSONException {
        JSONObject obj = new JSONObject();
        JSONObject attributes = serializeObject(eventAttributes);
        obj.put("attributes", attributes);
        obj.put("label", eventAttributes.getLabel());
        if (eventAttributes.getTags() != null) {
            obj.put("tags", new JSONArray(eventAttributes.getTags()));
        }
        return obj;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static JSONObject serializeObject(BatchEventAttributes eventAttributes)
        throws JSONException, ClassCastException {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, EventTypedAttribute> entry : eventAttributes.getAttributes().entrySet()) {
            EventTypedAttribute attribute = entry.getValue();
            String prefixedKey = entry.getKey().toLowerCase(Locale.US) + "." + attribute.type.getTypeChar();

            switch (attribute.type) {
                case URL:
                    obj.put(prefixedKey, attribute.value.toString());
                    break;
                case OBJECT:
                    obj.put(prefixedKey, serializeObject((BatchEventAttributes) attribute.value));
                    break;
                case OBJECT_ARRAY:
                    obj.put(prefixedKey, serializeList((List<BatchEventAttributes>) attribute.value));
                    break;
                case STRING_ARRAY:
                    obj.put(prefixedKey, new JSONArray((Collection) attribute.value));
                    break;
                default:
                    obj.put(prefixedKey, attribute.value);
                    break;
            }
        }
        return obj;
    }

    public static JSONArray serializeList(List<BatchEventAttributes> eventAttributesList) throws JSONException {
        JSONArray array = new JSONArray();

        for (BatchEventAttributes eventData : eventAttributesList) {
            array.put(serializeObject(eventData));
        }
        return array;
    }
}
