package com.batch.android.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.event.CollapsibleEvent;
import com.batch.android.event.Event;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Tracker datasource. Wraps SQLite queries (DAO).
 *
 */
public final class TrackerDatasource {

    private static final String TAG = "TrackerDatasource";

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
    private TrackerDatabaseHelper databaseHelper;

    // -------------------------------------------->

    public TrackerDatasource(Context context) throws SQLiteException {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        this.context = context.getApplicationContext();
        databaseHelper = new TrackerDatabaseHelper(this.context);
        database = databaseHelper.getWritableDatabase();
    }

    // -------------------------------------------->

    /**
     * Retrieve all events in DB (for test purpose)
     *
     * @return
     */
    protected List<Event> getAllEvents() {
        final List<Event> events = new ArrayList<>();

        try (
            Cursor cursor = database.query(TrackerDatabaseHelper.TABLE_EVENTS, null, null, null, null, null, null, null)
        ) {
            while (cursor.moveToNext()) {
                Event evt = parseEvent(cursor);

                events.add(evt);
            }

            return events;
        }
    }

    // -------------------------------------------->

    /**
     * Clear all content
     */
    public void clearDB() {
        database.delete(TrackerDatabaseHelper.TABLE_EVENTS, null, null);
    }

    /**
     * Add an event to the database
     *
     * @param event Event to add
     */
    public boolean addEvent(Event event) {
        if (event == null) {
            return false;
        }
        Logger.internal(TAG, "Add event " + event.getName() + "(" + event.getId() + ")");
        return insert(event);
    }

    /**
     * Extract a set of events and set their state to {@link Event.State#SENDING}
     *
     * @param limit
     * @return a list of events (can be empty on error)
     */
    public List<Event> extractEventsToSend(int limit) {
        final List<Event> events = new ArrayList<>(limit);
        Cursor cursor = null;

        try {
            /*
             * Build a list of retrieved ids to update them
             */
            final List<String> ids = new ArrayList<>(limit);

            /*
             * Query the DB
             */
            String limitStr = Integer.toString(limit);

            cursor =
                database.query(
                    TrackerDatabaseHelper.TABLE_EVENTS,
                    null,
                    TrackerDatabaseHelper.COLUMN_STATE +
                    " IN (" +
                    Event.State.NEW.getValue() +
                    "," +
                    Event.State.OLD.getValue() +
                    ")",
                    null,
                    null,
                    null,
                    "CASE WHEN " +
                    TrackerDatabaseHelper.COLUMN_NAME +
                    " LIKE '\\_%' ESCAPE '\\' THEN 1 ELSE 0 END DESC, " +
                    TrackerDatabaseHelper.COLUMN_DB_ID +
                    " desc",
                    limitStr
                );
            while (cursor.moveToNext()) {
                Event evt = parseEvent(cursor);

                events.add(evt);
                ids.add(evt.getId());
            }

            Logger.internal(TAG, "Retreived " + events.size() + " events from DB");

            /*
             * Update the records setting them to SENDING
             */

            int update = updateEventsToNewState(ids.toArray(new String[ids.size()]), Event.State.SENDING);

            if (update != events.size()) {
                throw new IllegalStateException("Updated rows are not equals to selected ones");
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Error while extracting event to send", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return events;
    }

    /**
     * Parse a DB event to Event obj
     *
     * @param cursor
     * @return
     */
    private Event parseEvent(Cursor cursor) {
        long serverTimestamp = 0;
        try {
            serverTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_SERVER_TIME));
        } catch (Exception e) {
            // Can throw an exception on null, nothing to do
        }

        Date secureDate = null;
        try {
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_SECURE_DATE))) {
                secureDate =
                    new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_SECURE_DATE)));
            }
        } catch (Exception e) {
            // Can throw an exception on null, nothing to do
        }

        String session = null;
        try {
            if (!cursor.isNull(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_SESSION_ID))) {
                session = cursor.getString(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_SESSION_ID));
            }
        } catch (Exception e) {
            // Can throw an exception on null, nothing to do
        }

        return new Event(
            cursor.getString(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_ID)),
            cursor.getString(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_NAME)),
            new Date(cursor.getLong(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_DATE))),
            TimeZone.getTimeZone(cursor.getString(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_TIMEZONE))),
            cursor.getString(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_PARAMETERS)),
            Event.State.fromValue(cursor.getInt(cursor.getColumnIndexOrThrow(TrackerDatabaseHelper.COLUMN_STATE))),
            serverTimestamp,
            secureDate,
            session
        );
    }

    /**
     * Set the State to NEW for the given events ids
     *
     * @param eventIDs
     * @return true on success, false otherwise
     */
    public boolean updateEventsToNew(String[] eventIDs) {
        return updateEventsToNewState(eventIDs, Event.State.NEW) == eventIDs.length;
    }

    /**
     * Set the State to OLD for the given events ids
     *
     * @param eventIDs
     * @return true on success, false otherwise
     */
    public boolean updateEventsToOld(String[] eventIDs) {
        return updateEventsToNewState(eventIDs, Event.State.OLD) == eventIDs.length;
    }

    /**
     * Update list of events to a new state
     *
     * @param eventIDs
     * @param newState
     * @return number of raws updated, -1 on error
     */
    private int updateEventsToNewState(String[] eventIDs, Event.State newState) {
        try {
            final StringBuilder builder = new StringBuilder(TrackerDatabaseHelper.COLUMN_ID);
            builder.append(" IN (");
            for (int i = 0; i < eventIDs.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append('?');
            }
            builder.append(")");

            ContentValues args = new ContentValues();
            args.put(TrackerDatabaseHelper.COLUMN_STATE, newState.getValue());

            return database.update(TrackerDatabaseHelper.TABLE_EVENTS, args, builder.toString(), eventIDs);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while updating events to new state", e);
            return -1;
        }
    }

    /**
     * Delete events by their ID
     *
     * @param eventIDs IDs of the events to delete
     * @return int number of raws deleted (-1 on error)
     */
    public int deleteEvents(String[] eventIDs) {
        if (eventIDs == null || eventIDs.length == 0) {
            return -1;
        }

        try {
            final StringBuilder builder = new StringBuilder(TrackerDatabaseHelper.COLUMN_ID);
            builder.append(" IN (");
            for (int i = 0; i < eventIDs.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append('?');
            }
            builder.append(")");

            return database.delete(TrackerDatabaseHelper.TABLE_EVENTS, builder.toString(), eventIDs);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while deleting events", e);
            return -1;
        }
    }

    /**
     * Delete events if there's too much in DB
     *
     * @param limit maximum number of events wanted
     * @return the number of events deleted
     */
    public int deleteOverflowEvents(int limit) {
        return database.delete(
            TrackerDatabaseHelper.TABLE_EVENTS,
            TrackerDatabaseHelper.COLUMN_DB_ID +
            " NOT IN (SELECT " +
            TrackerDatabaseHelper.COLUMN_DB_ID +
            " FROM " +
            TrackerDatabaseHelper.TABLE_EVENTS +
            " ORDER BY " +
            TrackerDatabaseHelper.COLUMN_DB_ID +
            " DESC LIMIT " +
            limit +
            ")",
            null
        );
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
     * Insert an event to SQLite.
     *
     * @param event Event to insert
     * @return If the insert succeeded or not
     */
    private boolean insert(Event event) {
        if (database == null) {
            Logger.internal(TAG, "Attempted to insert an event to a closed database");
            database = databaseHelper.getWritableDatabase();
        }

        if (event == null) {
            throw new NullPointerException("event==null");
        }

        try {
            if (event instanceof CollapsibleEvent && !TextUtils.isEmpty(event.getName())) {
                Logger.internal(TAG, "Deleting old instances of collapsible event");
                database.delete(
                    TrackerDatabaseHelper.TABLE_EVENTS,
                    TrackerDatabaseHelper.COLUMN_NAME + "=?",
                    new String[] { event.getName() }
                );
            }

            final ContentValues values = new ContentValues();
            values.put(TrackerDatabaseHelper.COLUMN_ID, event.getId());
            values.put(TrackerDatabaseHelper.COLUMN_NAME, event.getName());
            values.put(TrackerDatabaseHelper.COLUMN_DATE, event.getDate().getTime());
            values.put(TrackerDatabaseHelper.COLUMN_TIMEZONE, event.getTimezone().getID());

            if (event.getParameters() != null) {
                values.put(TrackerDatabaseHelper.COLUMN_PARAMETERS, event.getParameters());
            } else {
                values.putNull(TrackerDatabaseHelper.COLUMN_PARAMETERS);
            }

            values.put(TrackerDatabaseHelper.COLUMN_STATE, event.getState().getValue());

            if (event.getServerTimestamp() != 0) {
                values.put(TrackerDatabaseHelper.COLUMN_SERVER_TIME, event.getServerTimestamp());
            } else {
                String serverTS = ParametersProvider.get(context).get(ParameterKeys.SERVER_TIMESTAMP);
                if (serverTS != null) {
                    values.put(TrackerDatabaseHelper.COLUMN_SERVER_TIME, Long.parseLong(serverTS));
                } else {
                    values.put(TrackerDatabaseHelper.COLUMN_SERVER_TIME, event.getServerTimestamp());
                }
            }

            if (event.getSecureDate() != null) {
                values.put(TrackerDatabaseHelper.COLUMN_SECURE_DATE, event.getSecureDate().getTime());
            }

            if (event.getSessionID() != null) {
                values.put(TrackerDatabaseHelper.COLUMN_SESSION_ID, event.getSessionID());
            }

            database.insertOrThrow(TrackerDatabaseHelper.TABLE_EVENTS, null, values);

            Logger.internal(TAG, "Successfully inserted event " + event.getName() + "(" + event.getId() + ") into DB");
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while writing event to SQLite.", e);
        }

        return false;
    }

    /**
     * Reset all event to {@link Event.State#OLD} state
     *
     * @return
     */
    public boolean resetEventStatus() {
        try {
            ContentValues args = new ContentValues();
            args.put(TrackerDatabaseHelper.COLUMN_STATE, Event.State.OLD.getValue());

            database.update(
                TrackerDatabaseHelper.TABLE_EVENTS,
                args,
                TrackerDatabaseHelper.COLUMN_STATE +
                " IN (" +
                Event.State.SENDING.getValue() +
                "," +
                Event.State.NEW.getValue() +
                ")",
                null
            );
            return true;
        } catch (Exception e) {
            Logger.internal(TAG, "Error while reseting sending in DB", e);
            return false;
        }
    }
}
