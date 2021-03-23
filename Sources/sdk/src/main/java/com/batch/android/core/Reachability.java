package com.batch.android.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.RequiresPermission;

import com.batch.android.di.providers.LocalBroadcastManagerProvider;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reachability class that provide network status and network event
 *
 */
public class Reachability
{
    /**
     * Event broadcasted when connectivity change
     */
    public static final String CONNECTIVITY_CHANGED_EVENT = "ba_network_changed";
    /**
     * Key to retrieve the boolean extra in the Intent
     */
    public static final String IS_CONNECTED_KEY = "ba_is_connected";

// ---------------------------------------------->

    /**
     * Boolean that contains current connection state.
     */
    private AtomicBoolean isConnected;

    /**
     * Our receiver for the connectivity manager intent.
     */
    private BroadcastReceiver networkStateReceiver;

    /**
     * Saved context
     */
    private Context context;

// ----------------------------------------------->

    /**
     * Default constructor.
     *
     * @param context Creation context.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    public Reachability(Context context)
    {
        if (context == null) {
            throw new NullPointerException("Null context");
        }

        this.context = context.getApplicationContext();

        /*
         * Set initial value
         */
        isConnected = new AtomicBoolean(isConnectedNow());

        /*
         * Subscribe to ConnectivityManager intent
         */
        networkStateReceiver = new BroadcastReceiver()
        {
            @Override
            @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
            public void onReceive(Context context, Intent intent)
            {
                boolean isNowConnected = isConnectedNow();

                if (isConnected.compareAndSet(!isNowConnected, isNowConnected)) {
                    onConnectivityChange();
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        LocalBroadcastManagerProvider.get(this.context).registerReceiver(
                networkStateReceiver,
                filter);
    }

    /**
     * Unregister the receiver
     */
    public void stop()
    {
        LocalBroadcastManagerProvider.get(this.context).unregisterReceiver(
                networkStateReceiver);
    }

    /**
     * Is the network currently available.
     *
     * @return true if connected, false otherwise.
     */
    public boolean isConnected()
    {
        return isConnected.get();
    }

    /**
     * Called everytime connectivity change and broadcast en event
     */
    private void onConnectivityChange()
    {
        Intent intent = new Intent(CONNECTIVITY_CHANGED_EVENT);
        intent.putExtra(IS_CONNECTED_KEY, isConnected());

        LocalBroadcastManagerProvider.get(context).sendBroadcast(intent);
    }

// ----------------------------------------------->

    /**
     * Get connectivity manager current state.
     *
     * @return true if connected, false otherwise.
     */
    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    private boolean isConnectedNow()
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected() && activeNetwork.isAvailable();
    }

}
