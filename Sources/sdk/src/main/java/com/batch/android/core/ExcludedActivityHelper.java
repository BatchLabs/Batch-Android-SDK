package com.batch.android.core;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import com.batch.android.IntentParser;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple helper class to check if an activity should be excluded from the Batch's lifecycle
 * and save the intent if there's a push payload attached.
 */
public class ExcludedActivityHelper {

    private static final String TAG = "ExcludedActivityHelper";

    /**
     * Meta-data name to tag an activity excluded from Batch
     */
    private static final String EXCLUDED_ACTIVITY_META_DATA = "com.batch.android.EXCLUDE_FROM_LIFECYCLE";

    /**
     * Intent with a push payload from the excluded activity
     */
    private Intent intent;

    /**
     * Map of activities already checked for the EXCLUDE_FROM_LIFECYCLE meta-data info, to avoid
     * reading activityInfo each time.
     */
    private static final Map<Class<?>, Boolean> checkedActivities = new HashMap<>();

    /**
     * Check if the activity has a Batch push payload and saving it.
     *
     * @param activity the created activity
     */
    public void saveIntentIfNeeded(@NonNull Activity activity) {
        if (activity.getIntent() != null) {
            IntentParser intentParser = new IntentParser(activity);
            if (intentParser.hasPushPayload()) {
                intent = activity.getIntent();
                Logger.internal(TAG, "Saving the intent for the next start");
            }
        }
    }

    /**
     * Check if the activity has the EXCLUDE_FROM_LIFECYCLE meta-data info
     *
     * @param activity the activity to check
     * @return true if the activity should be excluded from Batch
     */
    public static boolean activityIsExcludedFromManifest(@NonNull Activity activity) {
        // Check if the activity meta-data info is already cached
        Boolean isExcluded = checkedActivities.get(activity.getClass());
        if (isExcluded != null) {
            return isExcluded;
        }

        // Getting the activity meta-data info
        isExcluded = false;
        try {
            ActivityInfo activityInfo = activity
                .getPackageManager()
                .getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA);
            if (activityInfo.metaData != null) {
                isExcluded = activityInfo.metaData.getBoolean(EXCLUDED_ACTIVITY_META_DATA, false);
                checkedActivities.put(activity.getClass(), isExcluded);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Package not found
        }
        return isExcluded;
    }

    /**
     * Check if there's a pending ignored intent with a push payload
     *
     * @return true if there's one
     */
    public boolean hasIntent() {
        return intent != null;
    }

    /**
     * Return the pending ignored intent with a push payload and remove it.
     *
     * @return intent
     */
    public Intent popIntent() {
        Intent intent = this.intent;
        this.intent = null;
        return intent;
    }
}
