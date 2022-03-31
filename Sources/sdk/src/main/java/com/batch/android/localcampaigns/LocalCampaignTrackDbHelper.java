package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class LocalCampaignTrackDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "LocalCampaignsSQLTracker.db";

    public static class LocalCampaignEntry implements BaseColumns {

        public static final String TABLE_NAME = "LocalCampaignsSQLTracker";
        public static final String COLUMN_NAME_CAMPAIGN_ID = "id";
        public static final String COLUMN_NAME_CAMPAIGN_KIND = "kind";
        public static final String COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE = "last_oc";
        public static final String COLUMN_NAME_CAMPAIGN_COUNT = "count";

        // New table added in version 2 to store every view event tracked
        public static final String TABLE_VIEW_EVENTS_NAME = "view_events";
        public static final String COLUMN_NAME_VE_CAMPAIGN_ID = "campaign_id";
        public static final String COLUMN_NAME_VE_TIMESTAMP = "timestamp_ms";

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
        "unique (" +
        LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
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
        " INTEGER NOT NULL" +
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

    public LocalCampaignTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        sqLiteDatabase.execSQL(SQL_CREATE_VIEW_EVENTS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_TRIGGER_VIEW_EVENT_DELETE_ROWS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            sqLiteDatabase.execSQL(SQL_CREATE_VIEW_EVENTS_TABLE);
            sqLiteDatabase.execSQL(SQL_CREATE_TRIGGER_VIEW_EVENT_DELETE_ROWS);
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
