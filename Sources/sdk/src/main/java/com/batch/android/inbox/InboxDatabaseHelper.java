package com.batch.android.inbox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Database helper for Batch's Inbox
 */
public final class InboxDatabaseHelper extends SQLiteOpenHelper {

    protected static final String COLUMN_DB_ID = "_db_id";

    protected static final String TABLE_FETCHERS = "fetchers";
    protected static final String COLUMN_FETCHER_TYPE = "type";
    protected static final String COLUMN_FETCHER_IDENTIFIER = "identifier";

    protected static final String TABLE_FETCHERS_NOTIFICATIONS = "fetcher_notifications";
    protected static final String COLUMN_FETCHER_ID = "fetcher_id";
    protected static final String COLUMN_INSTALL_ID = "install_id";
    protected static final String COLUMN_CUSTOM_ID = "custom_id";

    protected static final String TABLE_NOTIFICATIONS = "notifications";
    protected static final String COLUMN_NOTIFICATION_ID = "notification_id";
    protected static final String COLUMN_SEND_ID = "send_id";
    protected static final String COLUMN_TITLE = "title";
    protected static final String COLUMN_BODY = "body";
    protected static final String COLUMN_UNREAD = "unread";
    protected static final String COLUMN_DELETED = "deleted";
    protected static final String COLUMN_DATE = "date";
    protected static final String COLUMN_PAYLOAD = "payload";

    // -------------------------------------------->

    private static final String DATABASE_NAME = "ba_in.db";
    private static final int DATABASE_VERSION = 2;

    // -------------------------------------------->

    public InboxDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
            "create table " +
            TABLE_FETCHERS +
            "(" +
            COLUMN_DB_ID +
            " integer primary key autoincrement, " +
            COLUMN_FETCHER_TYPE +
            " integer not null, " +
            COLUMN_FETCHER_IDENTIFIER +
            " text not null, " +
            "unique(" +
            COLUMN_FETCHER_TYPE +
            ", " +
            COLUMN_FETCHER_IDENTIFIER +
            "));"
        );

        database.execSQL(
            "create table " +
            TABLE_NOTIFICATIONS +
            "(" +
            COLUMN_DB_ID +
            " integer primary key autoincrement, " +
            COLUMN_NOTIFICATION_ID +
            " text not null, " +
            COLUMN_SEND_ID +
            " text not null, " +
            COLUMN_TITLE +
            " text not null, " +
            COLUMN_BODY +
            " text not null, " +
            COLUMN_UNREAD +
            " integer not null default 0 check(" +
            COLUMN_UNREAD +
            " IN (0,1)), " +
            COLUMN_DELETED +
            " integer not null default 0 check(" +
            COLUMN_DELETED +
            " IN (0,1)), " +
            COLUMN_DATE +
            " integer not null, " +
            COLUMN_PAYLOAD +
            " text, " +
            "unique(" +
            COLUMN_NOTIFICATION_ID +
            "," +
            COLUMN_SEND_ID +
            "));"
        );

        database.execSQL(
            "create table " +
            TABLE_FETCHERS_NOTIFICATIONS +
            "(" +
            COLUMN_DB_ID +
            " integer primary key autoincrement, " +
            COLUMN_FETCHER_ID +
            " integer not null, " +
            COLUMN_NOTIFICATION_ID +
            " text not null, " +
            COLUMN_INSTALL_ID +
            " text, " +
            COLUMN_CUSTOM_ID +
            " text, " +
            "unique(" +
            COLUMN_FETCHER_ID +
            ", " +
            COLUMN_NOTIFICATION_ID +
            "), " +
            "foreign key(" +
            COLUMN_FETCHER_ID +
            ") references " +
            TABLE_FETCHERS +
            "(" +
            COLUMN_DB_ID +
            "), " +
            "foreign key(" +
            COLUMN_NOTIFICATION_ID +
            ") references " +
            TABLE_NOTIFICATIONS +
            "(" +
            COLUMN_NOTIFICATION_ID +
            "));"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL(
                "ALTER TABLE " +
                TABLE_NOTIFICATIONS +
                " ADD COLUMN " +
                COLUMN_DELETED +
                " integer not null default 0 check(" +
                COLUMN_DELETED +
                " IN (0,1)) "
            );
        }
    }
}
