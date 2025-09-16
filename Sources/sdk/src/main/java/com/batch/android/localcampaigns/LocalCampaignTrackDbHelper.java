package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class LocalCampaignTrackDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "LocalCampaignsSQLTracker.db";

    public static class LocalCampaignEntry implements BaseColumns {

        public static final String TABLE_NAME = "LocalCampaignsSQLTracker";
        public static final String COLUMN_NAME_CAMPAIGN_ID = "id";
        public static final String COLUMN_NAME_CUSTOM_USER_ID = "custom_user_id";
        public static final String COLUMN_NAME_CAMPAIGN_KIND = "kind";
        public static final String COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE = "last_oc";
        public static final String COLUMN_NAME_CAMPAIGN_COUNT = "count";

        // New table added in version 2 to store every view event tracked
        public static final String TABLE_VIEW_EVENTS_NAME = "view_events";
        public static final String COLUMN_NAME_VE_CAMPAIGN_ID = "campaign_id";
        public static final String COLUMN_NAME_VE_TIMESTAMP = "timestamp_ms";
        public static final String COLUMN_NAME_VE_CUSTOM_USER_ID = "custom_user_id";

        public static final String TRIGGER_VIEW_EVENTS_NAME = "trigger_clean_view_events";
    }

    /**
     * SQL request to create the initial view tracker table to count view events per campaign
     */
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " +
        LocalCampaignEntry.TABLE_NAME +
        " (" +
        LocalCampaignEntry._ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT," +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
        " TEXT," +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_KIND +
        " INTEGER," +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
        " INTEGER," +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
        " INTEGER," +
        LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
        " TEXT," +
        "unique (" +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
        ", " +
        LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
        ", " +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_KIND +
        ") on conflict replace)";

    /**
     * SQL request to delete the view tracker table
     */
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + LocalCampaignEntry.TABLE_NAME;

    /**
     * SQL request to create the view event table to store every view events
     * Must be clean when size is 100 entries
     */
    private static final String SQL_CREATE_VIEW_EVENTS_TABLE =
        "CREATE TABLE " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " (" +
        LocalCampaignEntry._ID +
        " INTEGER PRIMARY KEY AUTOINCREMENT," +
        LocalCampaignEntry.COLUMN_NAME_VE_CAMPAIGN_ID +
        " TEXT," +
        LocalCampaignEntry.COLUMN_NAME_VE_TIMESTAMP +
        " INTEGER NOT NULL," +
        LocalCampaignEntry.COLUMN_NAME_VE_CUSTOM_USER_ID +
        " TEXT" +
        ")";

    /**
     * SQL request to create a trigger when a new view events is inserted.
     * When triggered, check if the table has more than 100 rows and delete the oldest.
     */
    private static final String SQL_CREATE_TRIGGER_VIEW_EVENT_DELETE_ROWS =
        "CREATE TRIGGER " +
        LocalCampaignEntry.TRIGGER_VIEW_EVENTS_NAME +
        " AFTER INSERT ON " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " BEGIN" +
        " DELETE FROM " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " WHERE " +
        LocalCampaignEntry.COLUMN_NAME_VE_TIMESTAMP +
        "=(" +
        " SELECT min(" +
        LocalCampaignEntry.COLUMN_NAME_VE_TIMESTAMP +
        ") " +
        " FROM " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " )" +
        " AND (SELECT count(*) from " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " )>100;" +
        " END;";

    /**
     * SQL request to add the custom user id column to the view events table
     */
    private static final String ADD_VE_CUSTOM_USER_ID_COLUMN =
        "ALTER TABLE " +
        LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
        " ADD COLUMN " +
        LocalCampaignEntry.COLUMN_NAME_VE_CUSTOM_USER_ID +
        " text DEFAULT '';";

    /**
     * Constructor for the LocalCampaignTrackDbHelper
     * @param context the context
     */
    public LocalCampaignTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * @param sqLiteDatabase The database.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        sqLiteDatabase.execSQL(SQL_CREATE_VIEW_EVENTS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRIGGER_VIEW_EVENT_DELETE_ROWS);
    }

    /**
     * Called when the database needs to be upgraded.
     * @param sqLiteDatabase The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            sqLiteDatabase.execSQL(SQL_CREATE_VIEW_EVENTS_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_TRIGGER_VIEW_EVENT_DELETE_ROWS);
        }
        if (oldVersion < 3) {
            migrateToSchemaVersion3(sqLiteDatabase);
        }
    }

    /**
     * Migration of the database schema to version 3
     * This add a new column custom_user_id to the view events table and recreate the table LocalCampaignsSQLTracker with the new unique constraint.
     * @param sqLiteDatabase The database.
     */
    public void migrateToSchemaVersion3(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.beginTransaction();
        try {
            // Rename current table
            String tempTableName = LocalCampaignEntry.TABLE_NAME + "_temp";
            sqLiteDatabase.execSQL("ALTER TABLE " + LocalCampaignEntry.TABLE_NAME + " RENAME TO " + tempTableName);

            // Recreate table with new unique constraint
            sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);

            // Migrate data
            String sqlInsertData =
                "INSERT INTO " +
                LocalCampaignEntry.TABLE_NAME +
                " (" +
                LocalCampaignEntry._ID +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_KIND +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
                ") " +
                "SELECT " +
                LocalCampaignEntry._ID +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_KIND +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
                ", " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
                ", " +
                "'' " +
                "FROM " +
                tempTableName;
            sqLiteDatabase.execSQL(sqlInsertData);

            // Drop the old table
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + tempTableName);

            // Add custom user id column to view events table
            sqLiteDatabase.execSQL(ADD_VE_CUSTOM_USER_ID_COLUMN);
            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    /**
     * Helper function that parses a given table into a string
     * and returns it for easy printing. The string consists of
     * the table name and then each row is iterated through with
     * column_name: value pairs printed out.
     *
     * @param db the database to get the table from
     * @return the table tableName as a string
     */
    public String getTableAsString(SQLiteDatabase db) {
        StringBuilder tableString = new StringBuilder(String.format("Table %s:\n", LocalCampaignEntry.TABLE_NAME));
        Cursor allRows = db.rawQuery("SELECT * FROM " + LocalCampaignEntry.TABLE_NAME, null);
        if (allRows.moveToFirst()) {
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name : columnNames) {
                    tableString.append(
                        String.format("%s: %s\n", name, allRows.getString(allRows.getColumnIndexOrThrow(name)))
                    );
                }
                tableString.append("\n");
            } while (allRows.moveToNext());
        }
        if (!allRows.isClosed()) {
            allRows.close();
        }

        return tableString.toString();
    }
}
