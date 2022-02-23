package com.batch.android.core;

import com.batch.android.date.BatchDate;
import com.batch.android.date.UTCDate;

public class SystemDateProvider implements DateProvider {

    @Override
    public BatchDate getCurrentDate() {
        return new UTCDate();
    }
}
