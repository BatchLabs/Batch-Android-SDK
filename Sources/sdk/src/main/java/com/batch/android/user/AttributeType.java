package com.batch.android.user;

/**
 * Define the type of an attribute
 */
public enum AttributeType {
    DELETED(0, 'x'),

    STRING(1, 's'),

    LONG(2, 'i'),

    DOUBLE(3, 'f'),

    BOOL(4, 'b'),

    DATE(5, 't'),

    URL(6, 'u');

    // ---------------------------------------->

    private int value;

    private char typeChar;

    AttributeType(int value, char typeChar) {
        this.value = value;
        this.typeChar = typeChar;
    }

    public int getValue() {
        return value;
    }

    public char getTypeChar() {
        return typeChar;
    }

    // ----------------------------------------->

    /**
     * Mode from value
     *
     * @param value
     * @return mode if found, null otherwise
     */
    public static AttributeType fromValue(int value) {
        for (AttributeType mode : values()) {
            if (mode.getValue() == value) {
                return mode;
            }
        }

        return null;
    }
}
