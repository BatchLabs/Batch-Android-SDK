package com.batch.android.date;

import java.util.Calendar;

public class TimezoneAwareDate extends BatchDate {

    public TimezoneAwareDate() {
        super(System.currentTimeMillis());
    }

    public TimezoneAwareDate(long timestamp) {
        super(timestamp);
    }

    @Override
    public long getTime() {
        return timestamp - Calendar.getInstance().getTimeZone().getOffset(timestamp);
    }
}
