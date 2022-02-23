package com.batch.android.eventdispatcher;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Service used to retrieve meta-data of event dispatcher
 */
public class DispatcherDiscoveryService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
