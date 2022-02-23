package com.batch.android.tracker;

/**
 * Define the running mode of the Tracker module
 *
 */
public enum TrackerMode {
    /**
     * Tracker is OFF (no Sqlite & webservice)
     */
    OFF(0),

    /**
     * Tracker is only storing event in DB, not sending
     */
    DB_ONLY(1),

    /**
     * Tracker is up & running
     */
    ON(2);

    // ---------------------------------------->

    private int value;

    TrackerMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // ----------------------------------------->

    /**
     * Mode from value
     *
     * @param value
     * @return mode if found, null otherwise
     */
    public static TrackerMode fromValue(int value) {
        for (TrackerMode mode : values()) {
            if (mode.getValue() == value) {
                return mode;
            }
        }

        return null;
    }
}
