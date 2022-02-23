package com.batch.android.core;

import androidx.annotation.NonNull;
import com.batch.android.date.BatchDate;

/**
 * Simple interface for a mockable date provider
 */

public interface DateProvider {
    @NonNull
    BatchDate getCurrentDate();
}
