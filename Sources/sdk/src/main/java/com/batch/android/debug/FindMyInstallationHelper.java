package com.batch.android.debug;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import com.batch.android.Batch;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Simple helper class to copy the installation id of the the user to the clipboard after
 * the app is foregrounded a number of times for a short time.
 */
public class FindMyInstallationHelper {

    private static final String TAG = "FindMyInstallationHelper";

    /**
     * Minimum number of times the app should be foregrounded
     * before copying the installation id to the clipboard
     */
    private static final int MIN_FOREGROUND = 4;

    /**
     * Delay max (in ms) between MIN_FOREGROUND foregrounds to trigger the copy
     */
    private static final int MAX_DELAY_BETWEEN_FOREGROUNDS = 20_000;

    /**
     * Whether to enable the copy of the installation id. Default: true
     */
    public static boolean isEnabled = true;

    /**
     * List of timestamp added every time the app is foregrounded
     */
    private final List<Long> timestamps = new ArrayList<>();

    /**
     * Notify the app has been foregrounded, we save the current timestamp
     */
    public void notifyForeground() {
        if (FindMyInstallationHelper.isEnabled) {
            this.timestamps.add(new Date().getTime());
            if (this.timestamps.size() >= MIN_FOREGROUND) {
                if (shouldCopyInstallationID()) {
                    timestamps.clear();
                    copyInstallationIDToClipboard(RuntimeManagerProvider.get().getContext());
                    TrackerModuleProvider.get().track(InternalEvents.FIND_MY_INSTALLATION);
                } else {
                    timestamps.remove(0);
                }
            }
        }
    }

    /**
     * Check if we should copy the installation id to the clipboard
     *
     * @return true if we should
     */
    private boolean shouldCopyInstallationID() {
        List<Long> reversed = new ArrayList<>(timestamps);
        Collections.reverse(reversed);
        for (Long timestamp : reversed) {
            Long now = new Date().getTime();
            if (now - timestamp > MAX_DELAY_BETWEEN_FOREGROUNDS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Copy the installation id to the clipboard
     *
     * @param context Context
     */
    private void copyInstallationIDToClipboard(Context context) {
        if (context == null) {
            Logger.internal(TAG, "Context is null");
            return;
        }

        String installationID = Batch.User.getInstallationID();
        if (installationID == null) {
            Logger.internal(TAG, "Could not get user installation id");
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Logger.internal(TAG, "Could not get clipboard service");
            return;
        }
        ClipData clip = ClipData.newPlainText(
            "Batch Installation ID",
            "Batch Installation ID: ".concat(installationID)
        );
        clipboard.setPrimaryClip(clip);
    }
}
