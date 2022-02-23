package com.batch.android.core;

import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.batch.android.date.BatchDate;
import com.batch.android.date.UTCDate;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.Date;

/**
 * Class used to get the real time based of the server sync date
 *
 */
@Module
@Singleton
public class SecureDateProvider implements DateProvider {

    /**
     * Is the secure date enabled on this device
     */
    private final boolean mSecureDateEnabled;

    /**
     * Last known server timestamp
     */
    private Date mServerDate;

    /**
     * Number of millisecond since boot when mServerDate was set
     */
    private long mElapsedRealtime;

    // ------------------------------------------>

    public SecureDateProvider() {
        mSecureDateEnabled = canEnableSecureDate();
    }

    /**
     * Method used to obtain the real date based on the server date
     * if the server date was sync
     *
     * @return the actual secure time
     */
    public Date getDate() {
        Date current = mServerDate;

        if (!mSecureDateEnabled || current == null) {
            current = new Date();
        } else {
            current.setTime(current.getTime() + (SystemClock.elapsedRealtime() - mElapsedRealtime));
        }

        return current;
    }

    /**
     * Method used to know if the date is sync with the server
     *
     * @return true if the secure date is available false otherwise (in which case the system date
     *         will be returned)
     */
    public boolean isSecureDateAvailable() {
        return mSecureDateEnabled && mServerDate != null;
    }

    /**
     * Method used to init the server date
     *
     * @param pServerDate the sync server date
     */
    public void initServerDate(final Date pServerDate) {
        if (!mSecureDateEnabled) {
            return;
        }
        mElapsedRealtime = SystemClock.elapsedRealtime();
        mServerDate = pServerDate;
    }

    /**
     * Returns whether secure date can be enabled on this device
     */
    @VisibleForTesting
    protected boolean canEnableSecureDate() {
        // Disable secure date on Samsung devices as their uptime isn't monotonic:
        // When coming out of sleep, the uptime it way greater than it should be (as in, days ahead)
        // and slowly comes back to its expected value. Unfortunately this breaks important features
        // such as In-App start/end dates.
        if ("samsung".equalsIgnoreCase(Build.BRAND)) {
            return false;
        }
        return true;
    }

    @NonNull
    @Override
    public BatchDate getCurrentDate() {
        return new UTCDate(getDate().getTime());
    }
}
