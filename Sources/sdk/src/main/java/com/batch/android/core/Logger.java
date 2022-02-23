package com.batch.android.core;

import android.util.Log;
import com.batch.android.Batch;
import com.batch.android.LoggerDelegate;
import com.batch.android.LoggerLevel;

/**
 * Helper to easily log into the application.
 * <p>
 * Internal logs can be enabled using setprop  log.tag.BatchInternal VERBOSE
 *
 */
public final class Logger {

    /**
     * Public logger tag.
     */
    public static final String PUBLIC_TAG = "Batch";

    /**
     * Internal logger tag.
     */
    public static final String INTERNAL_TAG = "BatchInternal";

    /**
     * Logger delegate (optional)
     * The logger delegate is another logger which receives the same logs as this logger
     */
    public static LoggerDelegate loggerDelegate = null;

    /**
     * Is the logger in dev mode
     */
    public static final boolean dev = shouldEnableDevLogs();

    // ----------------------------------------->

    private static boolean shouldEnableDevLogs() {
        //noinspection ConstantConditions
        return Parameters.ENABLE_DEV_LOGS || Log.isLoggable(INTERNAL_TAG, Log.DEBUG);
    }

    // ----------------------------------------->

    public static boolean shouldLogForLevel(LoggerLevel level) {
        return dev || Batch.getLoggerLevel().canLog(level);
    }

    /**
     * Verbose log with module, message and throwable.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     * @param t      Throwable exception.
     */
    public static void verbose(String module, String msg, Throwable t) {
        if (shouldLogForLevel(LoggerLevel.VERBOSE)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.v(PUBLIC_TAG, message, t);
            if (loggerDelegate != null) {
                loggerDelegate.verbose(PUBLIC_TAG, message, t);
            }
        }
    }

    /**
     * Verbose log with message and throwable.
     *
     * @param msg Message of the error.
     * @param t   Throwable exception.
     */
    public static void verbose(String msg, Throwable t) {
        verbose(null, msg, t);
    }

    /**
     * Verbose log with module and message.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     */
    public static void verbose(String module, String msg) {
        if (shouldLogForLevel(LoggerLevel.VERBOSE)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.v(PUBLIC_TAG, message);
            if (loggerDelegate != null) {
                loggerDelegate.verbose(PUBLIC_TAG, message, null);
            }
        }
    }

    /**
     * Verbose log with message.
     *
     * @param msg Message of the error.
     */
    public static void verbose(String msg) {
        verbose(null, msg);
    }

    // ----------------------------------------->

    /**
     * Info log with module, message and throwable.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     * @param t      Throwable exception.
     */
    public static void info(String module, String msg, Throwable t) {
        if (shouldLogForLevel(LoggerLevel.INFO)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.i(PUBLIC_TAG, message, t);
            if (loggerDelegate != null) {
                loggerDelegate.info(PUBLIC_TAG, message, t);
            }
        }
    }

    /**
     * Info log with message and throwable.
     *
     * @param msg Message of the error.
     * @param t   Throwable exception.
     */
    public static void info(String msg, Throwable t) {
        info(null, msg, t);
    }

    /**
     * Info log with module and message.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     */
    public static void info(String module, String msg) {
        if (shouldLogForLevel(LoggerLevel.INFO)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.i(PUBLIC_TAG, message);
            if (loggerDelegate != null) {
                loggerDelegate.info(PUBLIC_TAG, message, null);
            }
        }
    }

    /**
     * Verbose log with message.
     *
     * @param msg Message of the error.
     */
    public static void info(String msg) {
        info(null, msg);
    }

    // ----------------------------------------->

    /**
     * Warning log with module, message and throwable.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     * @param t      Throwable exception.
     */
    public static void warning(String module, String msg, Throwable t) {
        if (shouldLogForLevel(LoggerLevel.WARNING)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.w(PUBLIC_TAG, message, t);
            if (loggerDelegate != null) {
                loggerDelegate.warn(PUBLIC_TAG, message, t);
            }
        }
    }

    /**
     * Warning log with message and throwable.
     *
     * @param msg Message of the error.
     * @param t   Throwable exception.
     */
    public static void warning(String msg, Throwable t) {
        warning(null, msg, t);
    }

    /**
     * Warning log with module and message.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     */
    public static void warning(String module, String msg) {
        if (shouldLogForLevel(LoggerLevel.WARNING)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.w(PUBLIC_TAG, message);
            if (loggerDelegate != null) {
                loggerDelegate.warn(PUBLIC_TAG, message, null);
            }
        }
    }

    /**
     * Warning log with message.
     *
     * @param msg Message of the error.
     */
    public static void warning(String msg) {
        warning(null, msg);
    }

    // ----------------------------------------->

    /**
     * Error log with module, message and throwable.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     * @param t      Throwable exception.
     */
    public static void error(String module, String msg, Throwable t) {
        if (shouldLogForLevel(LoggerLevel.ERROR)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.e(PUBLIC_TAG, message, t);
            if (loggerDelegate != null) {
                loggerDelegate.error(PUBLIC_TAG, message, t);
            }
        }
    }

    /**
     * Error log with message and throwable.
     *
     * @param msg Message of the error.
     * @param t   Throwable exception.
     */
    public static void error(String msg, Throwable t) {
        error(null, msg, t);
    }

    /**
     * Error log with module and message.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     */
    public static void error(String module, String msg) {
        if (shouldLogForLevel(LoggerLevel.ERROR)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.e(PUBLIC_TAG, message);
            if (loggerDelegate != null) {
                loggerDelegate.error(PUBLIC_TAG, message, null);
            }
        }
    }

    /**
     * Error log with message.
     *
     * @param msg Message of the error.
     */
    public static void error(String msg) {
        error(null, msg);
    }

    // ----------------------------------------->

    /**
     * Debug log with module, message and throwable.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     * @param t      Throwable exception.
     */
    public static void internal(String module, String msg, Throwable t) {
        if (shouldLogForLevel(LoggerLevel.INTERNAL)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.d(INTERNAL_TAG, message, t);
            if (loggerDelegate != null) {
                loggerDelegate.debug(INTERNAL_TAG, message, t);
            }
        }
    }

    /**
     * Internal log with message and throwable.
     *
     * @param msg Message of the error.
     * @param t   Throwable exception.
     */
    public static void internal(String msg, Throwable t) {
        internal(null, msg, t);
    }

    /**
     * Debug log with module and message.
     *
     * @param module Module name.
     * @param msg    Message of the error.
     */
    public static void internal(String module, String msg) {
        if (shouldLogForLevel(LoggerLevel.INTERNAL)) {
            String message = msg;
            if (module != null) {
                message = module + " - " + msg;
            }

            Log.d(INTERNAL_TAG, message);
            if (loggerDelegate != null) {
                loggerDelegate.debug(INTERNAL_TAG, message, null);
            }
        }
    }

    /**
     * Debug log with message.
     *
     * @param msg Message of the error.
     */
    public static void internal(String msg) {
        internal(null, msg);
    }
}
