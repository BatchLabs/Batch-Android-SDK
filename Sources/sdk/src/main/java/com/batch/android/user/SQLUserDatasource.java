package com.batch.android.user;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;
import com.batch.android.module.UserModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Singleton;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User data (attributes) datasource. Wraps SQLite queries (DAO).
 * <p>
 * This class is NOT thread or context safe at all. You must make sure it is <b>ALWAYS</b> acessed
 * using its singleton, and that all of the calls happen on a single thread.
 */
@Module
@Singleton
public final class SQLUserDatasource implements UserDatasource {

    private static final String TAG = "SQLUserDatasource";

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
    private UserDatabaseHelper databaseHelper;

    /**
     * Is a transaction currently occurring?
     * Prevents from doing a lot of non revertable stuff and makes performance better.
     */
    private boolean transactionOccurring = false;

    /**
     * Current transaction changeset. Only relevant when a transaction is occurring
     */
    private long currentChangeset = 0;

    public SQLUserDatasource(Context context) throws SQLiteException {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        this.context = context.getApplicationContext();
        databaseHelper = new UserDatabaseHelper(this.context);
        database = databaseHelper.getWritableDatabase();
    }

    // region Utility methods -------------------------------

    @Override
    public void close() {
        if (transactionOccurring) {
            try {
                rollbackTransaction();
            } catch (UserDatabaseException e) {
                // Don't care, it will log anyway
            }
        }

        database.close();
    }

    // endregion

    // region Transaction methods ---------------------------

    // @category test

    /**
     * Acquires a transaction for a changeset. Needed before doing other changes.
     *
     * @param changeset Changeset for modifications
     */
    @Override
    public void acquireTransactionLock(long changeset) throws UserDatabaseException {
        if (changeset > 0 && !transactionOccurring) {
            try {
                database.execSQL("BEGIN TRANSACTION;");
                transactionOccurring = true;
                currentChangeset = changeset;
            } catch (SQLiteException e) {
                logAndThrow("Error while starting the SQLite transaction", e);
            }
        } else {
            throwInvalidStateException();
        }
    }

    /**
     * Commits the transaction, releasing the transaction lock
     *
     * @throws UserDatabaseException
     */
    @Override
    public void commitTransaction() throws UserDatabaseException {
        if (transactionOccurring) {
            try {
                database.execSQL("COMMIT TRANSACTION;");
                transactionOccurring = false;
                currentChangeset = 0;
            } catch (SQLiteException e) {
                logAndThrow("Error while committing the SQLite transaction", e);
            }
        } else {
            throwInvalidStateException();
        }
    }

    @Override
    public void rollbackTransaction() throws UserDatabaseException {
        if (transactionOccurring) {
            try {
                database.execSQL("ROLLBACK TRANSACTION;");
                transactionOccurring = false;
                currentChangeset = 0;
            } catch (SQLiteException e) {
                logAndThrow("Error while rolling back the SQLite transaction", e);
            }
        } else {
            throwInvalidStateException();
        }
    }

    // endregion

    // region Attributes methods ----------------------------

    @Override
    public void setAttribute(@NonNull String key, long attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute);
        setAttribute(key, cv, AttributeType.LONG, false);
    }

    @Override
    public void setAttribute(@NonNull String key, double attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute);
        setAttribute(key, cv, AttributeType.DOUBLE, false);
    }

    @Override
    public void setAttribute(@NonNull String key, boolean attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute);
        setAttribute(key, cv, AttributeType.BOOL, false);
    }

    @Override
    public void setAttribute(@NonNull String key, @NonNull String attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute);
        setAttribute(key, cv, AttributeType.STRING, false);
    }

    @Override
    public void setAttribute(@NonNull String key, @NonNull Date attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute.getTime());
        setAttribute(key, cv, AttributeType.DATE, false);
    }

    @Override
    public void setAttribute(@NonNull String key, @NonNull URI attribute) throws UserDatabaseException {
        final ContentValues cv = new ContentValues();
        cv.put(UserDatabaseHelper.COLUMN_ATTR_VALUE, attribute.toString());
        setAttribute(key, cv, AttributeType.URL, false);
    }

    @Override
    public void removeAttribute(@NonNull String key) throws UserDatabaseException {
        deleteAttribute(key, false);
    }

    // endregion

    // region Tags methods ----------------------------------

    @Override
    public void addTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException {
        writeTag(collection, tag);
    }

    @Override
    public void removeTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException {
        deleteTag(collection, tag);
    }

    // endregion

    // region Cleanup methods -------------------------------

    /**
     * Clear everything. Not meant to be used during a transaction!
     */
    @Override
    public void clear() {
        if (transactionOccurring) {
            return;
        }

        database.delete(UserDatabaseHelper.TABLE_ATTRIBUTES, null, null);
        database.delete(UserDatabaseHelper.TABLE_TAGS, null, null);
    }

    @Override
    public void clearTags() {
        if (!transactionOccurring || currentChangeset <= 0) {
            return;
        }

        database.delete(UserDatabaseHelper.TABLE_TAGS, null, null);
    }

    @Override
    public void clearTags(String collection) {
        if (!transactionOccurring || currentChangeset <= 0 || TextUtils.isEmpty(collection)) {
            return;
        }

        database.delete(
            UserDatabaseHelper.TABLE_TAGS,
            UserDatabaseHelper.COLUMN_TAG_COLLECTION + "=?",
            new String[] { collection }
        );
    }

    @Override
    public void clearAttributes() {
        if (!transactionOccurring || currentChangeset <= 0) {
            return;
        }

        database.delete(UserDatabaseHelper.TABLE_ATTRIBUTES, null, null);
    }

    // endregion

    // region Attribute helpers ------------------------------

    // Set an attribute. The value should already be inserted in contentValues for the VALUE column.
    // It also assumes that everything has already been validated.
    private void setAttribute(String key, ContentValues values, AttributeType type, boolean isNative)
        throws UserDatabaseException {
        if (!transactionOccurring || TextUtils.isEmpty(key) || currentChangeset <= 0) {
            throwInvalidStateException();
            return;
        }

        try {
            values.put(UserDatabaseHelper.COLUMN_ATTR_NAME, (isNative ? "n." : "c.") + key);
            values.put(UserDatabaseHelper.COLUMN_ATTR_TYPE, type.getValue());
            // Value has already been added
            values.put(UserDatabaseHelper.COLUMN_ATTR_CHANGESET, currentChangeset);

            database.insertOrThrow(UserDatabaseHelper.TABLE_ATTRIBUTES, null, values);
        } catch (SQLiteConstraintException e) {
            // Means that the name+value+type are the same, so no data changed.
            // This catch is just here so that a false error isn't shown, and the operation still succeeds
        } catch (SQLException e) {
            logAndThrow("Error while inserting custom attribute '" + key + "'", e);
        }
    }

    private void deleteAttribute(String key, boolean isNative) throws UserDatabaseException {
        if (!transactionOccurring || TextUtils.isEmpty(key)) {
            throwInvalidStateException();
            return;
        }

        try {
            database.delete(
                UserDatabaseHelper.TABLE_ATTRIBUTES,
                UserDatabaseHelper.COLUMN_ATTR_NAME + "=?",
                new String[] { (isNative ? "n." : "c.") + key }
            );
        } catch (SQLException e) {
            logAndThrow("Error while deleting custom attribute '" + key + "'", e);
        }
    }

    // endregion

    // region Tag helpers ------------------------------------

    private void writeTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException {
        if (!transactionOccurring || currentChangeset <= 0 || TextUtils.isEmpty(collection) || TextUtils.isEmpty(tag)) {
            throwInvalidStateException();
            return;
        }

        try {
            final ContentValues values = new ContentValues();
            values.put(UserDatabaseHelper.COLUMN_TAG_COLLECTION, collection);
            values.put(UserDatabaseHelper.COLUMN_TAG_VALUE, tag);
            values.put(UserDatabaseHelper.COLUMN_TAG_CHANGESET, currentChangeset);

            database.insertOrThrow(UserDatabaseHelper.TABLE_TAGS, null, values);
        } catch (SQLiteConstraintException e) {
            // Means that the tag existed, so no data changed.
            // This catch is just here so that a false error isn't shown, and the operation still succeeds
        } catch (SQLException e) {
            logAndThrow(String.format("Error while adding tag '%s' in collection '%s'", tag, collection), e);
        }
    }

    private void deleteTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException {
        if (!transactionOccurring || currentChangeset <= 0 || TextUtils.isEmpty(collection) || TextUtils.isEmpty(tag)) {
            throwInvalidStateException();
            return;
        }

        try {
            database.delete(
                UserDatabaseHelper.TABLE_TAGS,
                UserDatabaseHelper.COLUMN_TAG_COLLECTION + "=? AND " + UserDatabaseHelper.COLUMN_TAG_VALUE + "=?",
                new String[] { collection, tag }
            );
        } catch (SQLException e) {
            logAndThrow(String.format("Error while removing tag '%s' in collection '%s'", tag, collection), e);
        }
    }

    // endregion

    // region Reader methods

    @Override
    public @NonNull Map<String, Set<String>> getTagCollections() {
        final Map<String, Set<String>> tagCollections = new HashMap<>();

        // Keeping the ORDER BY is mandatory for that "algorithm" to work

        try (
            Cursor cursor = database.query(
                UserDatabaseHelper.TABLE_TAGS,
                new String[] { UserDatabaseHelper.COLUMN_TAG_COLLECTION, UserDatabaseHelper.COLUMN_TAG_VALUE },
                null,
                null,
                null,
                null,
                UserDatabaseHelper.COLUMN_TAG_COLLECTION,
                null
            )
        ) {
            if (cursor == null) {
                return tagCollections;
            }

            String currentCollection = null;
            Set<String> currentTags = null;

            while (cursor.moveToNext()) {
                try {
                    String collection = cursor.getString(
                        cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_TAG_COLLECTION)
                    );
                    String value = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_TAG_VALUE));

                    if (collection == null || value == null) {
                        Logger.internal(
                            TAG,
                            "Consistency error while reading tags: collection or value null, skipping"
                        );
                    }

                    if (!TextUtils.equals(currentCollection, collection)) {
                        if (currentCollection != null && currentTags != null) {
                            tagCollections.put(currentCollection, currentTags);
                        }

                        currentCollection = collection;
                        currentTags = new HashSet<>();
                    }

                    if (currentTags != null) {
                        currentTags.add(value);
                    }
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while reading tag", e);
                }
            }

            if (currentCollection != null && currentTags != null) {
                tagCollections.put(currentCollection, currentTags);
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Unexpected error while reading attributes", e);
        }

        return tagCollections;
    }

    @Override
    public @NonNull HashMap<String, UserAttribute> getAttributes() {
        final HashMap<String, UserAttribute> attributes = new HashMap<>();

        try (
            Cursor cursor = database.query(
                UserDatabaseHelper.TABLE_ATTRIBUTES,
                new String[] {
                    UserDatabaseHelper.COLUMN_ATTR_NAME,
                    UserDatabaseHelper.COLUMN_ATTR_TYPE,
                    UserDatabaseHelper.COLUMN_ATTR_VALUE,
                },
                null,
                null,
                null,
                null,
                null,
                null
            )
        ) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        AttributeType type = AttributeType.fromValue(
                            cursor.getInt(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_ATTR_TYPE))
                        );

                        if (type == null) {
                            continue;
                        }

                        int valueColIndex = cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_ATTR_VALUE);
                        if (valueColIndex == -1) {
                            continue;
                        }

                        Object typedValue = null;

                        switch (type) {
                            case STRING:
                                typedValue = cursor.getString(valueColIndex);
                                break;
                            case DATE:
                                typedValue = new Date(cursor.getLong(valueColIndex));
                                break;
                            case BOOL:
                                {
                                    int intVal = cursor.getInt(valueColIndex);
                                    typedValue = (intVal != 0);
                                    break;
                                }
                            case LONG:
                                typedValue = cursor.getLong(valueColIndex);
                                break;
                            case DOUBLE:
                                typedValue = cursor.getDouble(valueColIndex);
                                break;
                            case URL:
                                typedValue = new URI(cursor.getString(valueColIndex));
                                break;
                            default:
                                continue;
                        }

                        if (typedValue == null) {
                            continue;
                        }

                        String name = cursor.getString(
                            cursor.getColumnIndexOrThrow(UserDatabaseHelper.COLUMN_ATTR_NAME)
                        );

                        attributes.put(name, new UserAttribute(typedValue, type));
                    } catch (Exception e) {
                        Logger.internal(TAG, "Error while reading attribute", e);
                    }
                }
            }
        } catch (Exception e) {
            Logger.internal(TAG, "Unexpected error while reading attributes", e);
        }

        return attributes;
    }

    // endregion

    // region Debug

    @Override
    public String printDebugDump() {
        StringBuilder debugBuilder = new StringBuilder();
        debugBuilder.append("Attributes: {");
        for (Map.Entry<String, UserAttribute> entry : getAttributes().entrySet()) {
            debugBuilder.append("\n\t");
            debugBuilder.append(entry.getKey());
            debugBuilder.append(": ");
            debugBuilder.append(entry.getValue().toString());
        }
        debugBuilder.append("\n}\nTag collections: {");
        for (Map.Entry<String, Set<String>> tagCollection : getTagCollections().entrySet()) {
            debugBuilder.append("\n\t");
            debugBuilder.append(tagCollection.getKey());
            debugBuilder.append(": [");

            for (String tag : tagCollection.getValue()) {
                debugBuilder.append("\n\t\t");
                debugBuilder.append(tag);
            }

            debugBuilder.append("\n\t]");
        }
        debugBuilder.append("\n}");

        String debugString = debugBuilder.toString();

        Logger.info(TAG, "Debug User Data dump:\n" + debugString);
        return debugString;
    }

    //

    // region Exception helpers

    private void logAndThrow(String msg, Throwable t) throws UserDatabaseException {
        Logger.internal(UserModule.TAG, msg, t);
        throw new UserDatabaseException(msg);
    }

    private void throwInvalidStateException() throws UserDatabaseException {
        throw new UserDatabaseException("Invalid database state");
    }
    // endregion
}
