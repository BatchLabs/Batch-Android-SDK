package com.batch.android.date;

import androidx.annotation.NonNull;

public abstract class BatchDate implements Comparable<BatchDate> {

    protected long timestamp;

    public BatchDate(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setTime(long time) {
        timestamp = time;
    }

    public long getTime() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchDate batchDate = (BatchDate) o;

        return timestamp == batchDate.timestamp;
    }

    @Override
    public int hashCode() {
        return (int) (timestamp ^ (timestamp >>> 32));
    }

    @Override
    public int compareTo(@NonNull BatchDate otherDate) {
        long thisVal = getTime();
        long anotherVal = otherDate.getTime();
        //Suppress the inspection as it's not available on API 15 (IntelliJ believes it is though)
        //noinspection UseCompareMethod
        return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
    }
}
