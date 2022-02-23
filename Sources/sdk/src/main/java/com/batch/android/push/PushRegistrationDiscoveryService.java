package com.batch.android.push;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Service used to retrieve meta-data of push registration provider
 */
public class PushRegistrationDiscoveryService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
