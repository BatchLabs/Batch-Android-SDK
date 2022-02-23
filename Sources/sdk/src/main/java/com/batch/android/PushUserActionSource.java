package com.batch.android;

import android.os.Bundle;
import com.batch.android.annotation.PublicSDK;

/**
 * Represents a push user action source
 */
@PublicSDK
public interface PushUserActionSource extends UserActionSource {
    Bundle getPushBundle();
}
