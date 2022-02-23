package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class LocalCampaignTrackDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "LocalCampaignsSQLTracker.db";

    public static class LocalCampaignEntry implements BaseColumns {

        public static final String TABLE_NAME = "LocalCampaignsSQLTracker";
        public static final String COLUMN_NAME_CAMPAIGN_ID = "id";
        public static final String COLUMN_NAME_CAMPAIGN_KIND = "kind";
        public static final String COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE = "last_oc";
        public static final String COLUMN_NAME_CAMPAIGN_COUNT = "count";
    }

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

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + LocalCampaignEntry.TABLE_NAME;

    public LocalCampaignTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // Empty now
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
