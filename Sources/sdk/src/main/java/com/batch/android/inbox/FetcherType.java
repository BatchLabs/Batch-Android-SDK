package com.batch.android.inbox;

enum FetcherType {
    INSTALLATION(0),
    USER_IDENTIFIER(1);

    private int value;

    FetcherType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String toWSPathElement() {
        switch (this) {
            case INSTALLATION:
            default:
                return "install";
            case USER_IDENTIFIER:
                return "custom";
        }
    }
}
