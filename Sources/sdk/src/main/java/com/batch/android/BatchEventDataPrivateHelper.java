package com.batch.android;

import com.batch.android.lisp.PrimitiveValue;
import com.batch.android.lisp.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @hide
 */
public class BatchEventDataPrivateHelper
{
    public static Map<String, Value> getAttributesFromEventData(BatchEventData data)
    {
        Map<String, BatchEventData.TypedAttribute> rawAttributes = data.getAttributes();

        Map<String, Value> result = new HashMap<>();

        for (String key : rawAttributes.keySet()) {
            BatchEventData.TypedAttribute attribute = rawAttributes.get(key);

            if (attribute == null) {
                continue;
            }

            switch (attribute.type) {
                case DOUBLE:
                    if (attribute.value instanceof Float) {
                        Double doubleValue = Double.valueOf((Float) attribute.value);
                        result.put(key, new PrimitiveValue(doubleValue));
                    } else {
                        result.put(key, new PrimitiveValue((Double) attribute.value));
                    }
                    break;
                case LONG:
                    if (attribute.value instanceof Integer) {
                        Double doubleValue = Double.valueOf((Integer) attribute.value);
                        result.put(key, new PrimitiveValue(doubleValue));
                    } else {
                        result.put(key,
                                new PrimitiveValue(Double.valueOf((Long) attribute.value)));
                    }
                    break;
                case BOOL:
                    result.put(key,
                            new PrimitiveValue((boolean) attribute.value));
                    break;
                case STRING:
                    result.put(key,
                            new PrimitiveValue((String) attribute.value));
                default:
            }
        }

        return result;
    }

    public static Set<String> getTagsFromEventData(BatchEventData data)
    {
        return data.getTags();
    }

    public static boolean getConvertedFromLegacyAPIFromEvent(BatchEventData data)
    {
        return data.getConvertedFromLegacyAPI();
    }
}
