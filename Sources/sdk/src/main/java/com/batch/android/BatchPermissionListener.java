package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * Interface for requesting runtime permission with callback.
 */
@PublicSDK
public interface BatchPermissionListener {
    /**
     * Method called when the permission has been requested with the user's permission result.
     *
     * @param granted Whether the permission requested has been granted.
     */
    void onPermissionRequested(boolean granted);
}
