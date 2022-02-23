package com.batch.android.date;

public class UTCDate extends BatchDate {

    public UTCDate() {
        super(System.currentTimeMillis());
    }

    public UTCDate(long timestamp) {
        super(timestamp);
    }
}
