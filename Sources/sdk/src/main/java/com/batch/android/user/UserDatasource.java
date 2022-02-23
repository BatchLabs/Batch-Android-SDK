package com.batch.android.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface UserDatasource {
    void close();

    void acquireTransactionLock(long changeset) throws UserDatabaseException;

    void commitTransaction() throws UserDatabaseException;

    void rollbackTransaction() throws UserDatabaseException;

    void setAttribute(@NonNull String key, long attribute) throws UserDatabaseException;

    void setAttribute(@NonNull String key, double attribute) throws UserDatabaseException;

    void setAttribute(@NonNull String key, boolean attribute) throws UserDatabaseException;

    void setAttribute(@NonNull String key, @NonNull String attribute) throws UserDatabaseException;

    void setAttribute(@NonNull String key, @NonNull Date attribute) throws UserDatabaseException;

    void setAttribute(@NonNull String key, @NonNull URI attribute) throws UserDatabaseException;

    void removeAttribute(@NonNull String key) throws UserDatabaseException;

    void addTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException;

    void removeTag(@NonNull String collection, @NonNull String tag) throws UserDatabaseException;

    void clear();

    void clearTags();

    void clearTags(String collection);

    void clearAttributes();

    @Nullable
    Map<String, Set<String>> getTagCollections();

    @Nullable
    HashMap<String, UserAttribute> getAttributes();

    String printDebugDump();
}
