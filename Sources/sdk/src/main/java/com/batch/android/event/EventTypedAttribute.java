package com.batch.android.event;

import com.batch.android.user.AttributeType;

public class EventTypedAttribute {

    public Object value;
    public AttributeType type;

    public EventTypedAttribute(Object value, AttributeType type) {
        this.value = value;
        this.type = type;
    }
}
