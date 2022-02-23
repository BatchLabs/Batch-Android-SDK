package com.batch.android.user;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite Database helper for Batch's User Data (attributes)
 *
 */
public final class UserDatabaseHelper extends SQLiteOpenHelper {

    protected static final String TABLE_ATTRIBUTES = "attributes";
    protected static final String COLUMN_ATTR_NAME = "name";
    protected static final String COLUMN_ATTR_TYPE = "type";
    protected static final String COLUMN_ATTR_VALUE = "value";
    protected static final String COLUMN_ATTR_CHANGESET = "changeset";

    // -------------------------------------------->

    protected static final String TABLE_TAGS = "tags";
    protected static final String COLUMN_TAG_COLLECTION = "collection";
    protected static final String COLUMN_TAG_VALUE = "value";
    protected static final String COLUMN_TAG_CHANGESET = "changeset";

    // -------------------------------------------->

    private static final String DATABASE_NAME = "ba_user_profile.db";
    private static final int DATABASE_VERSION = 1;

    // -------------------------------------------->

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
            "create table " +
            TABLE_ATTRIBUTES +
            "(" +
            COLUMN_ATTR_NAME +
            " text not null, " +
            COLUMN_ATTR_TYPE +
            " integer, " +
            COLUMN_ATTR_VALUE +
            " text, " +
            COLUMN_ATTR_CHANGESET +
            " integer, " +
            "unique(" +
            COLUMN_ATTR_NAME +
            ") on conflict replace, " +
            "unique(" +
            COLUMN_ATTR_NAME +
            "," +
            COLUMN_ATTR_TYPE +
            "," +
            COLUMN_ATTR_VALUE +
            ") on conflict abort);"
        );

        database.execSQL(
            "create table " +
            TABLE_TAGS +
            "(" +
            COLUMN_TAG_COLLECTION +
            " text not null, " +
            COLUMN_TAG_VALUE +
            " text not null, " +
            COLUMN_TAG_CHANGESET +
            " integer, " +
            "unique(" +
            COLUMN_TAG_COLLECTION +
            "," +
            COLUMN_TAG_VALUE +
            ") on conflict abort);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {}
}
