package com.batch.android;

import com.batch.android.annotation.PublicSDK;

@PublicSDK
public enum LoggerLevel {
    INTERNAL(0),
    VERBOSE(1),
    INFO(2),
    WARNING(3),
    ERROR(4);

    private final int level;

    LoggerLevel(int level) {
        this.level = level;
    }

    // Returns whether the specified log can be logged using this log level
    public boolean canLog(LoggerLevel targetLevel) {
        return this.level <= targetLevel.level;
    }
}
