package com.batch.android.lisp;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public final class PrimitiveValue extends Value
{
    public enum Type
    {
        Nil,
        String,
        Double,
        Bool,
        StringSet;

        @Override
        public java.lang.String toString()
        {
            switch (this) {
                case Nil:
                    return "Nil";
                case Bool:
                    return "Bool";
                case Double:
                    return "Double";
                case String:
                    return "String";
                case StringSet:
                    return "String Set";
            }
            return "Unknown primitive type";
        }
    }

    public final Type type;

    public final Object value;

    public static PrimitiveValue nilValue()
    {
        return new PrimitiveValue(Type.Nil, null);
    }

    private PrimitiveValue(Type type, Object value)
    {
        this.type = type;
        this.value = value;
    }

    public PrimitiveValue(String value)
    {
        this(Type.String, value);
    }

    public PrimitiveValue(Double value)
    {
        this(Type.Double, value);
    }

    public PrimitiveValue(Integer value)
    {
        this(Type.Double, Double.valueOf(value));
    }

    public PrimitiveValue(Boolean value)
    {
        this(Type.Bool, value);
    }

    public PrimitiveValue(Set<String> value)
    {
        this(Type.StringSet, new HashSet<>(value));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        } else if (!( obj instanceof PrimitiveValue )) {
            return false;
        } else {
            PrimitiveValue castedObj = (PrimitiveValue) obj;
            return this.type == castedObj.type && ( this.value == castedObj.value || this.value.equals(
                    castedObj.value) );
        }
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public String toString()
    {
        if (this.type == Type.Nil) {
            return this.type.toString();
        }

        String valueToString = this.value.toString();

        if (this.type == Type.String) {
            valueToString = "\"" + Value.escapedString(valueToString) + "\"";
        } else if (this.type == Type.StringSet) {
            valueToString = Value.setToString((Set<String>) this.value);
        }

        return valueToString;
    }
}

