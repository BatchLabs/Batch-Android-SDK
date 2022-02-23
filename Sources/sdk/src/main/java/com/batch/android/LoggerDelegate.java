package com.batch.android;

import com.batch.android.annotation.PublicSDK;

/**
 * Interface declaring methods that needs to be implemented for a Logger delegate.
 * Works in a way very similar to {@link android.util.Log}.
 *
 */
@PublicSDK
public interface LoggerDelegate {
    /**
     * Send a {@link android.util.Log#ERROR} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    void error(String tag, String msg, Throwable t);

    /**
     * Send a {@link android.util.Log#WARN} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    void warn(String tag, String msg, Throwable t);

    /**
     * Send a {@link android.util.Log#DEBUG} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    void debug(String tag, String msg, Throwable t);

    /**
     * Send a {@link android.util.Log#INFO} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    void info(String tag, String msg, Throwable t);

    /**
     * Send a {@link android.util.Log#VERBOSE} log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    void verbose(String tag, String msg, Throwable t);
}
