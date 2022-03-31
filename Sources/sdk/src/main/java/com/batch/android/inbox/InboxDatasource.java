package com.batch.android.inbox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inbox datasource. Wraps SQLite queries (DAO).
 */
@Module
@Singleton
public final class InboxDatasource {

    private static final String TAG = "InboxDatasource";

    /**
     * Saved app context
     */
    private Context context;
    /**
     * The SQLLite DB
     */
    private SQLiteDatabase database;
    /**
     * The DB Helper
     */
    private InboxDatabaseHelper databaseHelper;

    // -------------------------------------------->

    public InboxDatasource(Context context) throws SQLiteException {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        this.context = context.getApplicationContext();
        databaseHelper = new InboxDatabaseHelper(this.context);
        database = databaseHelper.getWritableDatabase();
    }

    /**
     * Clear all content
     */
    public void wipeData() {
        if (database == null) {
            Logger.internal(TAG, "Attempted to wipe data on a closed database");
            database = databaseHelper.getWritableDatabase();
        }

        database.delete(InboxDatabaseHelper.TABLE_NOTIFICATIONS, null, null);
        database.delete(InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS, null, null);
        database.delete(InboxDatabaseHelper.TABLE_FETCHERS, null, null);
    }

    /**
     * Close the datasource. You should not make any other call to this datasource once this has been called.
     */
    public void close() {
        if (database != null) {
            database.close();
            database = null;
        }
    }

    /**
     * Get database (for test purpose)
     *
     * @return
     */
    protected SQLiteDatabase getDatabase() {
        return database;
    }

    public List<InboxNotificationContentInternal> getNotifications(List<String> notificationIds, long fetcherId) {
        final List<InboxNotificationContentInternal> notifications = new ArrayList<>();

        String query =
            "SELECT * " +
            " FROM " +
            InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
            " INNER JOIN " +
            InboxDatabaseHelper.TABLE_NOTIFICATIONS +
            " ON " +
            InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
            "." +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            " = " +
            InboxDatabaseHelper.TABLE_NOTIFICATIONS +
            "." +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            " WHERE " +
            InboxDatabaseHelper.COLUMN_FETCHER_ID +
            " = ?" +
            " AND " +
            InboxDatabaseHelper.TABLE_NOTIFICATIONS +
            "." +
            InboxDatabaseHelper.COLUMN_DELETED +
            "= 0" +
            " AND " +
            InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
            "." +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            " IN (" +
            createInClause(notificationIds.size()) +
            ")" +
            " ORDER BY " +
            InboxDatabaseHelper.COLUMN_DATE +
            " DESC";

        String[] args = new String[notificationIds.size() + 1];
        args[0] = Long.toString(fetcherId);
        int i = 1;
        for (String notificationId : notificationIds) {
            args[i] = notificationId;
            ++i;
        }

        try (Cursor cursor = database.rawQuery(query, args)) {
            while (cursor.moveToNext()) {
                InboxNotificationContentInternal notification = parseNotification(cursor);
                notifications.add(notification);
            }

            return notifications;
        } catch (Exception e) {
            Logger.internal(TAG, "Could not get notifications", e);
        }

        return notifications;
    }

    protected long getNotificationTime(String notificationId) {
        try (
            Cursor cursor = database.query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                new String[] { InboxDatabaseHelper.COLUMN_DATE },
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { notificationId },
                null,
                null,
                null,
                "1"
            )
        ) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_DATE));
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Could not get notification time", e);
        }

        return -1;
    }

    /**
     * Try to get the fetcher ID from the SQLite
     * Create and insert it if it doesn't exist
     *
     * @param type
     * @param identifier
     */
    public long getFetcherID(FetcherType type, String identifier) {
        if (TextUtils.isEmpty(identifier)) {
            return -1;
        }

        if (database == null) {
            Logger.internal(TAG, "Attempted to get/insert a fetcher to a closed database");
            database = databaseHelper.getWritableDatabase();
        }

        final ContentValues values = new ContentValues();
        values.put(InboxDatabaseHelper.COLUMN_FETCHER_TYPE, type.getValue());
        values.put(InboxDatabaseHelper.COLUMN_FETCHER_IDENTIFIER, identifier);

        try {
            return database.insertWithOnConflict(
                InboxDatabaseHelper.TABLE_FETCHERS,
                null,
                values,
                SQLiteDatabase.CONFLICT_ABORT
            );
        } catch (SQLiteConstraintException e) {
            // Constraint (uniqueness of fetcher IDs) failed
            // The fetcher already exists in db
            try (
                Cursor cursor = database.query(
                    InboxDatabaseHelper.TABLE_FETCHERS,
                    new String[] { InboxDatabaseHelper.COLUMN_DB_ID },
                    InboxDatabaseHelper.COLUMN_FETCHER_TYPE +
                    "=? and " +
                    InboxDatabaseHelper.COLUMN_FETCHER_IDENTIFIER +
                    "=?",
                    new String[] { Integer.toString(type.getValue()), identifier },
                    null,
                    null,
                    null,
                    "1"
                )
            ) {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_DB_ID));
                }
            } catch (Exception ex) {
                Logger.internal(TAG, "Error when getting fetcher id", ex);
                return -1;
            }
        }

        Logger.internal(TAG, "Could not find or create fetcher");
        return -1;
    }

    /**
     * Look in database for cached notifications
     *
     * @param cursor
     * @param fetcherId
     * @return
     */
    public List<InboxCandidateNotificationInternal> getCandidateNotifications(
        @Nullable String cursor,
        int limit,
        long fetcherId
    ) {
        List<InboxCandidateNotificationInternal> candidates = new ArrayList<>();

        if (!TextUtils.isEmpty(cursor)) {
            long cursorTime = getNotificationTime(cursor);
            if (cursorTime != -1) {
                String query =
                    "SELECT " +
                    InboxDatabaseHelper.COLUMN_FETCHER_ID +
                    ", " +
                    InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                    "." +
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                    ", " +
                    InboxDatabaseHelper.COLUMN_UNREAD +
                    ", " +
                    InboxDatabaseHelper.COLUMN_DATE +
                    " FROM " +
                    InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                    " INNER JOIN " +
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS +
                    " ON " +
                    InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                    "." +
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                    " = " +
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS +
                    "." +
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                    " WHERE " +
                    InboxDatabaseHelper.COLUMN_DATE +
                    " < ?" +
                    " AND " +
                    InboxDatabaseHelper.COLUMN_FETCHER_ID +
                    " = ?" +
                    " ORDER BY " +
                    InboxDatabaseHelper.COLUMN_DATE +
                    " DESC" +
                    " LIMIT " +
                    limit;
                try (
                    Cursor result = database.rawQuery(
                        query,
                        new String[] { Long.toString(cursorTime), Long.toString(fetcherId) }
                    )
                ) {
                    while (result.moveToNext()) {
                        InboxCandidateNotificationInternal candidate = parseCandidateNotification(result);
                        candidates.add(candidate);
                    }
                } catch (Exception e) {
                    Logger.internal(TAG, "Could not get candidates notifications", e);
                }
            }
        } else {
            String query =
                "SELECT " +
                InboxDatabaseHelper.COLUMN_FETCHER_ID +
                ", " +
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                "." +
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                ", " +
                InboxDatabaseHelper.COLUMN_UNREAD +
                ", " +
                InboxDatabaseHelper.COLUMN_DATE +
                " FROM " +
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                " INNER JOIN " +
                InboxDatabaseHelper.TABLE_NOTIFICATIONS +
                " ON " +
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
                "." +
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                " = " +
                InboxDatabaseHelper.TABLE_NOTIFICATIONS +
                "." +
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                " WHERE " +
                InboxDatabaseHelper.COLUMN_FETCHER_ID +
                " = ?" +
                " ORDER BY " +
                InboxDatabaseHelper.COLUMN_DATE +
                " DESC" +
                " LIMIT " +
                limit;
            try (Cursor result = database.rawQuery(query, new String[] { Long.toString(fetcherId) })) {
                while (result.moveToNext()) {
                    InboxCandidateNotificationInternal candidate = parseCandidateNotification(result);
                    candidates.add(candidate);
                }
            } catch (Exception e) {
                Logger.internal(TAG, "Could not get candidates notifications", e);
            }
        }
        return candidates;
    }

    /**
     * Add a response's notification to the database
     *
     * @param response  response to add
     * @param fetcherId
     */
    public boolean insertResponse(InboxWebserviceResponse response, long fetcherId) {
        if (response == null || fetcherId <= 0) {
            return false;
        }

        for (InboxNotificationContentInternal notification : response.notifications) {
            Logger.internal(TAG, "Add notification in DB: " + notification.identifiers.identifier);
            insert(notification, fetcherId);
        }

        return true;
    }

    /**
     * Insert a notification to SQLite.
     *
     * @param notification Notification to insert
     * @param fetcherId    Fetcher id
     * @return If the insert succeeded or not
     */
    protected boolean insert(InboxNotificationContentInternal notification, long fetcherId) {
        if (database == null) {
            Logger.internal(TAG, "Attempted to insert a notification to a closed database");
            database = databaseHelper.getWritableDatabase();
        }

        if (notification == null) {
            throw new NullPointerException("notification==null");
        }

        try {
            final ContentValues values = new ContentValues();
            values.put(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID, notification.identifiers.identifier);
            values.put(InboxDatabaseHelper.COLUMN_SEND_ID, notification.identifiers.sendID);
            values.put(InboxDatabaseHelper.COLUMN_TITLE, notification.title != null ? notification.title : "");
            values.put(InboxDatabaseHelper.COLUMN_BODY, notification.body != null ? notification.body : "");
            values.put(InboxDatabaseHelper.COLUMN_UNREAD, notification.isUnread ? 1 : 0);
            values.put(InboxDatabaseHelper.COLUMN_DATE, notification.date.getTime());

            values.put(InboxDatabaseHelper.COLUMN_PAYLOAD, new JSONObject(notification.payload).toString());

            ContentValues linkValues = new ContentValues();
            linkValues.put(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID, notification.identifiers.identifier);
            linkValues.put(InboxDatabaseHelper.COLUMN_FETCHER_ID, fetcherId);
            linkValues.put(InboxDatabaseHelper.COLUMN_INSTALL_ID, notification.identifiers.installID);
            linkValues.put(InboxDatabaseHelper.COLUMN_CUSTOM_ID, notification.identifiers.customID);

            database.beginTransactionNonExclusive();
            try {
                database.insertWithOnConflict(
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                );

                database.insertWithOnConflict(
                    InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                    null,
                    linkValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                );

                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            Logger.internal(
                TAG,
                "Successfully inserted notification " + notification.identifiers.identifier + " into DB"
            );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while writing event to SQLite.", e);
        }

        return false;
    }

    /**
     * Read the notification object and update the row in database
     * We update different values depending on what the server actually sent
     *
     * @param notification
     * @param fetcherId
     * @return
     */
    public String updateNotification(JSONObject notification, long fetcherId) {
        try {
            String notificationId = notification.getString("notificationId");

            ContentValues notificationsValues = new ContentValues();
            ContentValues values = new ContentValues();
            for (String key : notification.keySet()) {
                switch (key) {
                    case "sendId":
                        String sendId = notification.getString("sendId");
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_SEND_ID, sendId);
                        break;
                    case Batch.Push.TITLE_KEY:
                        String title = notification.getString(Batch.Push.TITLE_KEY);
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_TITLE, title);
                        break;
                    case Batch.Push.BODY_KEY:
                        String body = notification.getString(Batch.Push.BODY_KEY);
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_BODY, body);
                        break;
                    case "read":
                        boolean unread =
                            !notification.reallyOptBoolean("read", false) &&
                            !notification.reallyOptBoolean("opened", false);
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_UNREAD, unread ? 1 : 0);
                        break;
                    case "notificationTime":
                        Date date = new Date(notification.getLong("notificationTime"));
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_DATE, date.getTime());
                        break;
                    case InboxDatabaseHelper.COLUMN_PAYLOAD:
                        JSONObject payload = notification.getJSONObject("payload");
                        notificationsValues.put(InboxDatabaseHelper.COLUMN_PAYLOAD, payload.toString());
                        break;
                    case "installId":
                        String installId = notification.getString("installId");
                        values.put(InboxDatabaseHelper.COLUMN_INSTALL_ID, installId);
                        break;
                    case "customId":
                        String customId = notification.getString("customId");
                        values.put(InboxDatabaseHelper.COLUMN_CUSTOM_ID, customId);
                        break;
                }
            }

            if (notificationsValues.size() <= 0) {
                // JSON contains only notificationId
                // Meaning we have the latest payload and states in DB
                return notificationId;
            }

            database.beginTransactionNonExclusive();
            try {
                database.update(
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                    notificationsValues,
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                    new String[] { notificationId }
                );

                if (values.size() > 0) {
                    database.update(
                        InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                        values,
                        InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
                        " =? and " +
                        InboxDatabaseHelper.COLUMN_FETCHER_ID +
                        " =?",
                        new String[] { notificationId, Long.toString(fetcherId) }
                    );
                }

                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            return notificationId;
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not parse sync payload", e);
        }
        return null;
    }

    /**
     * Mark all notification received before a specified time as read
     *
     * @param time
     * @param fetcherId
     * @return
     */
    public int markAllAsRead(long time, long fetcherId) {
        ContentValues values = new ContentValues();
        values.put(InboxDatabaseHelper.COLUMN_UNREAD, 0);

        return database.update(
            InboxDatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            InboxDatabaseHelper.COLUMN_DATE +
            " <= ?" +
            " AND EXISTS (" +
            " SELECT " +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            " FROM " +
            InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS +
            " WHERE " +
            InboxDatabaseHelper.COLUMN_FETCHER_ID +
            " = ?" +
            " AND " +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            " = " +
            InboxDatabaseHelper.TABLE_NOTIFICATIONS +
            "." +
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID +
            ")",
            new String[] { Long.toString(time), Long.toString(fetcherId) }
        );
    }

    /**
     * Mark a notification as read
     *
     * @param notificationID the notification identifier
     */
    public void markNotificationAsRead(String notificationID) {
        ContentValues values = new ContentValues();
        values.put(InboxDatabaseHelper.COLUMN_UNREAD, 0);
        database.update(
            InboxDatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " = ?",
            new String[] { notificationID }
        );
    }

    /**
     * Mark a notification as deleted locally
     *
     * @param notificationID the notification identifier
     */
    public void markNotificationAsDeleted(String notificationID) {
        ContentValues values = new ContentValues();
        values.put(InboxDatabaseHelper.COLUMN_DELETED, 1);
        database.update(
            InboxDatabaseHelper.TABLE_NOTIFICATIONS,
            values,
            InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " = ?",
            new String[] { notificationID }
        );
    }

    /**
     * Delete notification by ID
     *
     * @param notificationIds IDs of the notifications to delete
     * @return boolean
     */
    public boolean deleteNotifications(List<String> notificationIds) {
        if (notificationIds.size() <= 0) {
            return false;
        }

        String[] args = notificationIds.toArray(new String[0]);

        database.beginTransactionNonExclusive();
        try {
            database.delete(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " IN (" + createInClause(args.length) + ")",
                args
            );

            database.delete(
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " IN (" + createInClause(args.length) + ")",
                args
            );

            database.setTransactionSuccessful();
        } catch (Exception e) {
            Logger.internal(TAG, "Could not delete notifications", e);
            return false;
        } finally {
            database.endTransaction();
        }
        return true;
    }

    /**
     * Remove notifications older than 90 days
     * Also remove related row in other tables
     */
    public boolean cleanDatabase() {
        long expireTime = System.currentTimeMillis() - 7776000000L;

        try (
            Cursor cursor = database.query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                new String[] { InboxDatabaseHelper.COLUMN_NOTIFICATION_ID },
                InboxDatabaseHelper.COLUMN_DATE + " <= ?",
                new String[] { Long.toString(expireTime) },
                null,
                null,
                null,
                null
            )
        ) {
            List<String> idsToDelete = new ArrayList<>();
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID));
                if (!TextUtils.isEmpty(id)) {
                    idsToDelete.add(id);
                }
            }

            return deleteNotifications(idsToDelete);
        } catch (Exception e) {
            Logger.internal(TAG, "Could not clean database", e);
        }
        return false;
    }

    /**
     * Parse a DB cursor to Notification object
     *
     * @param cursor
     * @return
     */
    private InboxNotificationContentInternal parseNotification(Cursor cursor) {
        try {
            final JSONObject payload = new JSONObject(
                cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_PAYLOAD))
            );

            final InternalPushData batchData = new InternalPushData(payload.getString("com.batch"));
            final NotificationIdentifiers identifiers = new NotificationIdentifiers(
                cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_SEND_ID))
            );

            identifiers.customID = cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_CUSTOM_ID));
            identifiers.installID =
                cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_INSTALL_ID));
            identifiers.additionalData = batchData.getExtraParameters();

            final Map<String, String> convertedPayload = new HashMap<>();
            for (String payloadKey : payload.keySet()) {
                try {
                    convertedPayload.put(payloadKey, payload.getString(payloadKey));
                } catch (JSONException ignored) {
                    Logger.internal(
                        TAG,
                        "Could not coalesce payload value to string for key \"" + payloadKey + "\". Ignoring."
                    );
                }
            }

            final InboxNotificationContentInternal c = new InboxNotificationContentInternal(
                batchData.getSource(),
                new Date(cursor.getLong(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_DATE))),
                convertedPayload,
                identifiers
            );

            c.body = payload.reallyOptString(Batch.Push.BODY_KEY, null);
            c.title = payload.reallyOptString(Batch.Push.TITLE_KEY, null);
            c.isUnread = cursor.getInt(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_UNREAD)) != 0;

            return c;
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not parse notification from DB", e);
        }

        // JSON IN DB IS INVALID -- TODO DELETE LINE
        return null;
    }

    /**
     * Parse a DB cursor to Notification object
     *
     * @param cursor
     * @return
     */
    private InboxCandidateNotificationInternal parseCandidateNotification(Cursor cursor) {
        return new InboxCandidateNotificationInternal(
            cursor.getString(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)),
            cursor.getInt(cursor.getColumnIndexOrThrow(InboxDatabaseHelper.COLUMN_UNREAD)) != 0
        );
    }

    /**
     * Create a placeholder string for arguments substitution in IN clauses
     *
     * @param length
     * @return
     */
    private String createInClause(int length) {
        if (length < 1) {
            return "";
        }

        StringBuilder sb = new StringBuilder(length * 2 - 1);
        sb.append("?");
        for (int i = 1; i < length; i++) {
            sb.append(",?");
        }
        return sb.toString();
    }
}
