package com.batch.android.runtime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.LoggerDelegate;
import com.batch.android.LoggerLevel;
import com.batch.android.core.Logger;

/**
 * Class to build a runtime configuration for Batch SDK
 *
 */
public final class Config {

    /**
     * The API key used for Batch
     */
    @Nullable
    private String apikey;

    /**
     * Should Batch send Logs to a 3rd party class
     */
    @Nullable
    private LoggerDelegate loggerDelegate = null;

    /**
     * Level of log Batch should use
     */
    @NonNull
    private LoggerLevel loggerLevel = LoggerLevel.INFO;

    /**
     * Migrations related configuration
     */
    @Nullable
    private Integer migrations = null;

    /**
     * Constructor
     */
    public Config() {}

    /**
     * Get the Batch API Key.
     *
     * @return The Batch API Key.
     */
    @Nullable
    public String getApikey() {
        return apikey;
    }

    /**
     * Set the Batch SDK API Key
     * @param apikey The Batch SDK API Key
     */
    public void setApikey(@Nullable String apikey) {
        this.apikey = apikey;
    }

    /**
     * Get the current logger delegate
     *
     * @return The current logger delegate
     */
    @Nullable
    public LoggerDelegate getLoggerDelegate() {
        return loggerDelegate;
    }

    /**
     * Set if Batch should send its logs to an object of yours (default = null)<br>
     * <br>
     * Be careful with your implementation: setting this can impact stability and performance<br>
     * You should only use it if you know what you are doing.
     *
     * @param delegate An object implementing {@link LoggerDelegate}
     */
    public void setLoggerDelegate(@Nullable LoggerDelegate delegate) {
        Logger.loggerDelegate = delegate;
        loggerDelegate = delegate;
    }

    /**
     * Get the current logger level
     *
     * @return The current logger level
     */
    @NonNull
    public LoggerLevel getLoggerLevel() {
        return loggerLevel;
    }

    /**
     * Set the log level Batch should use
     *
     * @param level The level of the logger to set
     */
    public void setLoggerLevel(@NonNull LoggerLevel level) {
        Logger.loggerLevel = level;
        loggerLevel = level;
    }

    /**
     * Get the migrations configuration
     *
     * @return the migrations configuration
     */
    @Nullable
    public Integer getMigrations() {
        return migrations;
    }

    /**
     * Set the current migrations related configuration
     * @param migrations The migrations to disable
     */
    public void setMigrations(@Nullable Integer migrations) {
        this.migrations = migrations;
    }

    /**
     * Make deep copy of this configuration
     *
     * @return A new config object
     */
    @NonNull
    public Config copy() {
        Config copy = new Config();
        copy.setApikey(this.apikey);
        copy.setLoggerDelegate(this.loggerDelegate);
        copy.setLoggerLevel(this.loggerLevel);
        copy.setMigrations(migrations);
        return copy;
    }
}
