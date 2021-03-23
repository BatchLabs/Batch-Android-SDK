package com.batch.android.lisp;

import com.batch.android.user.UserAttribute;
import com.batch.android.user.UserDatasource;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public final class UserAttributeContext implements EvaluationContext
{
    private final UserDatasource dataSource;
    private Map<String, UserAttribute> attributes;
    private Map<String, Set<String>> tagCollections;

    public UserAttributeContext(UserDatasource dataSource)
    {
        this.dataSource = dataSource;

        // Attributes and tags are lazily fetched
        // The code could be optimized further by tweaking the datasources to be able
        // to only fetch a specific attribute
    }

    @Override
    public Value resolveVariableNamed(String name)
    {
        if (name.length() > 2) {
            if (name.startsWith("c.")) {

                fetchAttributes();
                if (attributes != null) {
                    for (String key : attributes.keySet()) {
                        UserAttribute attr = attributes.get(key);
                        // Attributes are stored with "c."
                        if (name.compareToIgnoreCase(key) == 0) {
                            return attributeToValue(attr);
                        }
                    }
                }

                return PrimitiveValue.nilValue();
            } else if (name.startsWith("t.")) {

                String wantedCollection = name.substring(2);
                if (!wantedCollection.isEmpty()) {
                    fetchTags();
                    if (tagCollections != null) {
                        Set<String> collection = tagCollections.get(wantedCollection);
                        if (collection != null) {
                            return new PrimitiveValue(collection);
                        }
                    }
                }

                return PrimitiveValue.nilValue();
            }
        }

        return null;
    }

    private void fetchAttributes()
    {
        if (attributes == null) {
            attributes = dataSource.getAttributes();
        }
    }

    private void fetchTags()
    {
        if (tagCollections == null) {
            tagCollections = dataSource.getTagCollections();
        }
    }

    private Value attributeToValue(UserAttribute attribute)
    {
        if (attribute == null) {
            return PrimitiveValue.nilValue();
        }

        switch (attribute.type) {
            case DATE:
                if (attribute.value instanceof Date) {
                    Date date = (Date) attribute.value;
                    return new PrimitiveValue((double) date.getTime());
                }
                break;
            case BOOL:
                if (attribute.value instanceof Boolean) {
                    return new PrimitiveValue((Boolean) attribute.value);
                }
                break;
            case DOUBLE:
            case LONG:
                if (attribute.value instanceof Number) {
                    return new PrimitiveValue(( (Number) attribute.value ).doubleValue());
                }
                break;
            case STRING:
                if (attribute.value instanceof String) {
                    return new PrimitiveValue((String) attribute.value);
                }
                break;
            default:
                return PrimitiveValue.nilValue();
        }

        return PrimitiveValue.nilValue();
    }
}
