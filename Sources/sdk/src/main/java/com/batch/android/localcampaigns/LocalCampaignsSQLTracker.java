package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.Batch;
import com.batch.android.core.DateProvider;
import com.batch.android.core.Logger;
import com.batch.android.core.SystemDateProvider;
import com.batch.android.localcampaigns.LocalCampaignTrackDbHelper.LocalCampaignEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalCampaignsSQLTracker implements ViewTracker {

    private static final String TAG = "LocalCampaignsSQLTracker";
    private LocalCampaignTrackDbHelper dbHelper;
    private SQLiteDatabase database;
    private DateProvider dateProvider;
    private boolean open = false;

    public LocalCampaignsSQLTracker() {
        this.dateProvider = new SystemDateProvider();
    }

    @VisibleForTesting
    public LocalCampaignsSQLTracker(@NonNull DateProvider dateProvider) {
        this.dateProvider = dateProvider;
    }

    public void open(Context context) {
        dbHelper = new LocalCampaignTrackDbHelper(context);
        open = true;
    }

    public void close() {
        if (database != null) {
            database.close();
            database = null;
        }
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public DateProvider getDateProvider() {
        return this.dateProvider;
    }

    public void setDateProvider(@NonNull DateProvider dateProvider) {
        this.dateProvider = dateProvider;
    }

    /**
     * This function increment campaign's count, or insert it in the table if it doesn't exist
     *
     * @param campaignID Campaign ID
     * @return The updated {@link CountedViewEvent}
     */
    @Override
    public CountedViewEvent trackViewEvent(@NonNull String campaignID, @Nullable String customUserId)
        throws ViewTrackerUnavailableException {
        ensureWritableDatabase();

        CountedViewEvent ev = getViewEventByCampaignIdAndCustomId(campaignID, customUserId);
        ev.count++;
        ev.lastOccurrence = dateProvider.getCurrentDate().getTime();
        database.execSQL(
            "INSERT OR REPLACE INTO " +
            LocalCampaignEntry.TABLE_NAME +
            " (" +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_KIND +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
            ") VALUES (?, " +
            KIND_VIEW +
            ", ?, ?, ?)",
            new String[] {
                campaignID,
                Integer.toString(ev.count),
                Long.toString(ev.lastOccurrence),
                customUserId == null ? "" : customUserId,
            }
        );

        database.execSQL(
            "INSERT INTO " +
            LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
            " (" +
            LocalCampaignEntry.COLUMN_NAME_VE_CAMPAIGN_ID +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_VE_TIMESTAMP +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_VE_CUSTOM_USER_ID +
            ") VALUES (?, ?, ?)",
            new String[] { campaignID, Long.toString(ev.lastOccurrence), customUserId == null ? "" : customUserId }
        );
        return ev;
    }

    @NonNull
    @Override
    public CountedViewEvent getViewEventByCampaignId(@NonNull String campaignID)
        throws ViewTrackerUnavailableException {
        ensureWritableDatabase();

        CountedViewEvent ev = new CountedViewEvent(campaignID);

        Cursor countCursor = database.rawQuery(
            "SELECT " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
            " FROM " +
            LocalCampaignEntry.TABLE_NAME +
            " WHERE " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
            " = ?",
            new String[] { campaignID }
        );

        while (countCursor.moveToNext()) {
            ev.count += countCursor.getInt(0);
            ev.lastOccurrence = Math.max(countCursor.getLong(1), ev.lastOccurrence);
        }
        countCursor.close();
        return ev;
    }

    @NonNull
    @Override
    public CountedViewEvent getViewEventByCampaignIdAndCustomId(
        @NonNull String campaignID,
        @Nullable String customUserId
    ) throws ViewTrackerUnavailableException {
        ensureWritableDatabase();

        CountedViewEvent ev = new CountedViewEvent(campaignID, customUserId);

        Cursor countCursor = database.rawQuery(
            "SELECT " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
            ", " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
            " FROM " +
            LocalCampaignEntry.TABLE_NAME +
            " WHERE " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
            " = ? AND " +
            LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
            " = ?",
            new String[] { campaignID, customUserId == null ? "" : customUserId }
        );

        if (countCursor.moveToFirst()) {
            ev.count = countCursor.getInt(0);
            ev.lastOccurrence = countCursor.getLong(1);
        }
        countCursor.close();
        return ev;
    }

    @Override
    @NonNull
    public Map<String, Integer> getViewCountsByCampaignIds(@NonNull List<String> campaignsIds)
        throws ViewTrackerUnavailableException {
        ensureWritableDatabase();

        Map<String, Integer> views = new HashMap<>(campaignsIds.size());

        if (!campaignsIds.isEmpty()) {
            for (String campaignId : campaignsIds) {
                views.put(campaignId, 0);
            }

            final StringBuilder idsSelectArgsBuilder = new StringBuilder();
            idsSelectArgsBuilder.append('?');
            for (int i = 1; i < campaignsIds.size(); i++) {
                idsSelectArgsBuilder.append(",?");
            }

            Cursor countCursor = database.rawQuery(
                "SELECT " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                ", " +
                "SUM(" +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
                ") " +
                " FROM " +
                LocalCampaignEntry.TABLE_NAME +
                " WHERE " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                " IN (" +
                idsSelectArgsBuilder +
                ")" +
                "GROUP BY " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID,
                campaignsIds.toArray(new String[campaignsIds.size()])
            );

            while (countCursor.moveToNext()) {
                String id = countCursor.getString(0);
                int count = countCursor.getInt(1);
                views.put(id, count);
            }
            countCursor.close();
        }

        return views;
    }

    @NonNull
    @Override
    public Map<String, Integer> getViewCountsByCampaignIdsAndCustomUserId(
        @NonNull List<String> campaignsIds,
        @Nullable String customUserId
    ) throws ViewTrackerUnavailableException {
        Map<String, Integer> views = new HashMap<>(campaignsIds.size());

        if (!campaignsIds.isEmpty()) {
            for (String campaignId : campaignsIds) {
                views.put(campaignId, 0);
            }

            final StringBuilder idsSelectArgsBuilder = new StringBuilder();
            idsSelectArgsBuilder.append('?');
            for (int i = 1; i < campaignsIds.size(); i++) {
                idsSelectArgsBuilder.append(",?");
            }

            String userId = customUserId == null ? "" : customUserId;
            String[] args = new String[campaignsIds.size() + 1];
            for (int i = 0; i < campaignsIds.size(); i++) {
                args[i] = campaignsIds.get(i);
            }
            args[campaignsIds.size()] = userId;

            Cursor countCursor = database.rawQuery(
                "SELECT " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                ", " +
                "SUM(" +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_COUNT +
                ") " +
                " FROM " +
                LocalCampaignEntry.TABLE_NAME +
                " WHERE " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
                " IN (" +
                idsSelectArgsBuilder +
                ")" +
                " AND " +
                LocalCampaignEntry.COLUMN_NAME_CUSTOM_USER_ID +
                " = ?" +
                " GROUP BY " +
                LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID,
                args
            );

            while (countCursor.moveToNext()) {
                String id = countCursor.getString(0);
                int count = countCursor.getInt(1);
                views.put(id, count);
            }
            countCursor.close();
        }
        return views;
    }

    @Override
    public long getCampaignLastOccurrence(@NonNull String campaignID) throws ViewTrackerUnavailableException {
        ensureWritableDatabase();

        Cursor countCursor = database.rawQuery(
            "SELECT " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_LAST_OCCURRENCE +
            " FROM " +
            LocalCampaignEntry.TABLE_NAME +
            " WHERE " +
            LocalCampaignEntry.COLUMN_NAME_CAMPAIGN_ID +
            " = ?",
            new String[] { campaignID }
        );
        long lastOccurence = 0;

        while (countCursor.moveToNext()) {
            lastOccurence = Math.max(lastOccurence, countCursor.getLong(0));
        }
        countCursor.close();

        return lastOccurence;
    }

    @Override
    public int getNumberOfViewEventsSince(long timestamp) throws ViewTrackerUnavailableException {
        ensureWritableDatabase();
        int total = 0;
        Cursor countCursor = database.rawQuery(
            "SELECT COUNT(*) " +
            " FROM " +
            LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME +
            " WHERE " +
            LocalCampaignEntry.COLUMN_NAME_VE_TIMESTAMP +
            " > ?",
            new String[] { Long.toString(timestamp) }
        );
        if (countCursor.moveToFirst()) {
            total = countCursor.getInt(0);
        }
        countCursor.close();
        return total;
    }

    public void deleteViewEvents() throws ViewTrackerUnavailableException {
        ensureWritableDatabase();
        database.execSQL("DELETE FROM " + LocalCampaignEntry.TABLE_VIEW_EVENTS_NAME);
        database.execSQL("DELETE FROM " + LocalCampaignEntry.TABLE_NAME);
    }

    private void ensureWritableDatabase() throws ViewTrackerUnavailableException {
        if (database == null) {
            if (dbHelper == null) {
                throw new ViewTrackerUnavailableException();
            }
            try {
                database = dbHelper.getWritableDatabase();
            } catch (SQLException e) {
                Logger.internal(TAG, "Could not get a writable database", e);
                throw new ViewTrackerUnavailableException();
            }
        }
    }
}
