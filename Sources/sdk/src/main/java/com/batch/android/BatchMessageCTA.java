package com.batch.android;

import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;

/**
 * Represents a BatchAction triggerable by a basic CTA messaging component
 */
@PublicSDK
public class BatchMessageCTA extends BatchMessageAction {

    private String label;

    /**
     * This is a private constructor
     *
     * @hide
     */
    public BatchMessageCTA(@NonNull com.batch.android.messaging.model.CTA from) {
        super(from);
        label = from.label;
    }

    @NonNull
    public String getLabel() {
        return label;
    }
}
