package com.batch.android.core.stores;

import android.content.Context;
import androidx.annotation.NonNull;

// Represents an Application Store
public interface StoreApplication {
    // Try to open the store for the current application
    void open(@NonNull Context context);
}
