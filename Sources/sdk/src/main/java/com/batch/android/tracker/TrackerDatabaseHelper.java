package com.batch.android.tracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Database helper for Batch's Tracker
 *
 */
public final class TrackerDatabaseHelper extends SQLiteOpenHelper {

    protected static final String TABLE_EVENTS = "events";
    protected static final String COLUMN_DB_ID = "_db_id";
    protected static final String COLUMN_ID = "id";
    protected static final String COLUMN_NAME = "name";
    protected static final String COLUMN_DATE = "date";
    protected static final String COLUMN_TIMEZONE = "timezone";
    protected static final String COLUMN_PARAMETERS = "parameters";
    protected static final String COLUMN_STATE = "state";
    protected static final String COLUMN_SERVER_TIME = "serverts";
    protected static final String COLUMN_SECURE_DATE = "sdate";
    protected static final String COLUMN_SESSION_ID = "session_id";

    // -------------------------------------------->

    private static final String DATABASE_NAME = "ba_tr.db";
    private static final int DATABASE_VERSION = 3;

    // -------------------------------------------->

    public TrackerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
            "create table " +
            TABLE_EVENTS +
            "(" +
            COLUMN_DB_ID +
            " integer primary key autoincrement, " +
            COLUMN_ID +
            " text not null, " +
            COLUMN_NAME +
            " text not null, " +
            COLUMN_DATE +
            " integer not null, " +
            COLUMN_TIMEZONE +
            " text not null, " +
            COLUMN_PARAMETERS +
            " text, " +
            COLUMN_STATE +
            " integer not null, " +
            COLUMN_SERVER_TIME +
            " integer, " +
            COLUMN_SECURE_DATE +
            " integer null, " +
            COLUMN_SESSION_ID +
            " text null);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_SECURE_DATE + " integer null;");
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_SESSION_ID + " text null;");
        }
    }
}
