package com.batch.android.core;

public class VersionHelper {

    /**
     * Extracts the major version number from a version string.
     *
     * <p>The version string is expected to be in the format "major.minor.patch" (e.g., "1.2.3").
     * If the string cannot be parsed or is in an unexpected format, this method logs an error
     * and returns 0.
     *
     * @param version The version string to parse.
     * @return The major version number as an integer, or 0 if parsing fails.
     */
    public static int getMajor(String version) {
        try {
            return Integer.parseInt(version.split("\\.")[0]);
        } catch (Exception e) {
            Logger.internal("Error parsing version strings: " + version, e);

            return 0;
        }
    }
}
