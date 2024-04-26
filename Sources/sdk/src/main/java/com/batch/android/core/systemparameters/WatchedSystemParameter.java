package com.batch.android.core.systemparameters;

import static com.batch.android.core.Parameters.PARAMETERS_KEY_PREFIX;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.batch.android.di.providers.KVUserPreferencesStorageProvider;
import java.util.Objects;

public class WatchedSystemParameter extends SystemParameter {

    private static final String SHARED_PREFERENCES_KEY_PREFIX = PARAMETERS_KEY_PREFIX.concat("system.param.");

    /**
     * The value of the parameter
     */
    @Nullable
    private String lastValue;

    /**
     * Android's context requires to access the shared preferences.
     */
    @NonNull
    private final Context context;

    /**
     * Constructor
     *
     * @param context Android's context requires to access the shared preferences.
     * @param shortName The system parameter short name enum
     * @param getter Specific implementation of the system parameter value to get
     */
    public WatchedSystemParameter(
        @NonNull Context context,
        @NonNull SystemParameterShortName shortName,
        @NonNull SystemParameterGetter getter
    ) {
        super(shortName, getter);
        this.context = context;
    }

    /**
     * Constructor
     *
     * @param context Android's context requires to access the shared preferences.
     * @param shortName The system parameter short name enum
     * @param getter Specific implementation of the system parameter value to get
     * @param allowed Flag indicating whether this parameter is allowed to be send
     */
    public WatchedSystemParameter(
        @NonNull Context context,
        @NonNull SystemParameterShortName shortName,
        @NonNull SystemParameterGetter getter,
        boolean allowed
    ) {
        super(shortName, getter, allowed);
        this.context = context;
    }

    /**
     * Detect if the values has changed since the last time we get it.
     *
     * <p>
     * This will save the new value if it changed
     *
     * @return whether this parameter has changed or not
     */
    @WorkerThread
    public boolean hasChanged() {
        // Get the last saved value
        this.lastValue = KVUserPreferencesStorageProvider.get(context).get(getSharedPreferencesKey(), null);

        // Get current value
        String value = getter.get();

        // Check if value has changed
        boolean hasChanged = !Objects.equals(value, this.lastValue);

        // Keep new value as last
        this.lastValue = value;

        // Save if value has changed
        if (hasChanged) {
            KVUserPreferencesStorageProvider.get(context).persist(getSharedPreferencesKey(), lastValue);
        }
        return hasChanged;
    }

    /**
     * Get the last value saved
     * This mean return value from shared pref if you didn't call ``detectChanges` yet,
     * or the last value read from the specific getter if value has changed
     * @return The last value read
     */
    @Nullable
    public String getLastValue() {
        return lastValue;
    }

    /**
     * Build the shared preference key
     *
     * @return the shared preference key
     */
    private String getSharedPreferencesKey() {
        return SHARED_PREFERENCES_KEY_PREFIX.concat(this.shortName.shortName);
    }
}
