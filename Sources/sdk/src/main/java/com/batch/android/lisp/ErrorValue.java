package com.batch.android.lisp;

public final class ErrorValue extends Value
{

    public enum Type
    {
        Internal,
        Error,
        Parser;

        @Override
        public String toString()
        {
            switch (this) {
                case Error:
                    return "Program error";
                case Parser:
                    return "Parser error";
                case Internal:
                    return "Internal error";
            }
            return "Unknown error kind";
        }
    }

    public final Type type;

    public final String message;

    ErrorValue(Type type, String message)
    {
        this.type = type;
        this.message = message;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        } else if (!( obj instanceof ErrorValue )) {
            return false;
        } else {
            ErrorValue castedObj = (ErrorValue) obj;
            return this.type == castedObj.type && this.message.equals(castedObj.message);
        }
    }

    @Override
    public String toString()
    {
        return "<ErrorValue> Type: " + type.toString() + ", Message: \"" + message + "\"";
    }
}
