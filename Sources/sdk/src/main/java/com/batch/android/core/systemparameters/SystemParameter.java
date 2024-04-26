package com.batch.android.core.systemparameters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SystemParameter {

    /**
     * Specific implementation of the value to get
     */
    public interface SystemParameterGetter {
        String get();
    }

    /**
     * The system parameter short name enum
     */
    @NonNull
    protected SystemParameterShortName shortName;

    /**
     * Specific implementation of the system parameter value to get
     */
    @NonNull
    protected SystemParameterGetter getter;

    /**
     * Whether this parameter is allowed to be tracked
     */
    protected boolean allowed = true;

    /**
     * Constructor
     *
     * @param shortName The system parameter short name enum
     * @param getter Specific implementation of the system parameter value to get
     */
    public SystemParameter(@NonNull SystemParameterShortName shortName, @NonNull SystemParameterGetter getter) {
        this.shortName = shortName;
        this.getter = getter;
    }

    /**
     * Constructor
     *
     * @param shortName The system parameter short name enum
     * @param getter Specific implementation of the system parameter value to get
     * @param allowed Flag indicating whether this parameter is allowed to be send
     */
    public SystemParameter(
        @NonNull SystemParameterShortName shortName,
        @NonNull SystemParameterGetter getter,
        boolean allowed
    ) {
        this.shortName = shortName;
        this.getter = getter;
        this.allowed = allowed;
    }

    /**
     * Get the value
     *
     * @return The value of the parameter
     */
    @Nullable
    public String getValue() {
        return getter.get();
    }

    /**
     * Get the system parameter short name enum
     *
     * @return the system parameter short name enum
     */
    @NonNull
    public SystemParameterShortName getShortName() {
        return shortName;
    }

    /**
     * Whether this parameter is allowed to be tracked
     *
     * @return true when allowed, false otherwise
     */
    public boolean isAllowed() {
        return this.allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
