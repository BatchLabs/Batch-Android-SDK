package com.batch.android.core;

import android.os.SystemClock;
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
   * Date sync with server
   */
  private Date mServerDate;

  /**
   * Number of millisecond since boot
   */
  private long mElapsedRealtime;

  // ------------------------------------------>

  /**
   * Method used to obtain the real date based on the server date
   * if the server date was sync
   *
   * @return the actual secure time
   */
  public Date getDate() {
    Date current = mServerDate;

    if (current == null) {
      current = new Date();
    } else {
      current.setTime(
        current.getTime() + (SystemClock.elapsedRealtime() - mElapsedRealtime)
      );
    }

    return current;
  }

  /**
   * Method used to know if the date is sync with the server
   *
   * @return true if the serverDate is sync false otherwise
   */
  public boolean isSyncDate() {
    return mServerDate != null;
  }

  /**
   * Method used to init the server date
   *
   * @param pServerDate the sync server date
   */
  public void initServerDate(final Date pServerDate) {
    mElapsedRealtime = SystemClock.elapsedRealtime();
    mServerDate = pServerDate;
  }

  @Override
  public BatchDate getCurrentDate() {
    return new UTCDate(getDate().getTime());
  }
}
