package com.batch.android.inbox;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchInboxFetcher;
import com.batch.android.BatchInboxNotificationContent;
import com.batch.android.PrivateNotificationContentHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.NamedThreadFactory;
import com.batch.android.di.providers.InboxDatasourceProvider;
import com.batch.android.di.providers.InboxFetcherInternalProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.TrackerModule;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.webservice.listener.InboxWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Internal implementation of {@link BatchInboxFetcher}
 */
@Module
public class InboxFetcherInternal {

    private static final String TAG = "InboxFetcher";

    private static boolean isDatabaseCleaned = false;

    private TrackerModule trackerModule;

    private Context context;

    private String cursor = null;

    private long fetcherId;

    private FetcherType fetcherType;

    private String identifier;

    private String authKey;

    private final List<InboxNotificationContentInternal> fetchedNotifications = new ArrayList<>();

    private int maxPageSize = 20;

    private int fetchLimit = 200;

    private Executor fetchExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("inbox.fetcher"));

    private boolean endReached = false;

    private InboxDatasource datasource;

    private boolean filterSilentNotifications = true;

    private InboxFetcherInternal(
        @NonNull TrackerModule trackerModule,
        @Nullable InboxDatasource datasource,
        @NonNull Context context,
        String installID
    ) {
        this.trackerModule = trackerModule;
        this.context = context;
        this.fetcherType = FetcherType.INSTALLATION;
        this.identifier = installID;
        this.datasource = datasource;
        if (datasource != null) {
            this.fetcherId = datasource.getFetcherID(fetcherType, identifier);
        } else {
            this.fetcherId = -1;
        }
    }

    @Provide
    public static InboxFetcherInternal provide(@NonNull Context context, String installID) {
        return new InboxFetcherInternal(
            TrackerModuleProvider.get(),
            InboxDatasourceProvider.get(context),
            context,
            installID
        );
    }

    /**
     * Init fetcher without using cache
     * Internal use only
     */
    @Provide
    public static InboxFetcherInternal provide(@NonNull Context context, String installID, boolean useCache) {
        if (useCache) {
            return InboxFetcherInternalProvider.get(context, installID);
        }

        return new InboxFetcherInternal(TrackerModuleProvider.get(), null, context, installID);
    }

    private InboxFetcherInternal(
        @NonNull TrackerModule trackerModule,
        @Nullable InboxDatasource datasource,
        @NonNull Context context,
        @NonNull String userIdentifier,
        @NonNull String authenticationKey
    ) {
        this.trackerModule = trackerModule;
        this.context = context;
        this.fetcherType = FetcherType.USER_IDENTIFIER;
        this.identifier = userIdentifier;
        this.authKey = authenticationKey;
        this.datasource = datasource;
        if (datasource != null) {
            this.fetcherId = datasource.getFetcherID(fetcherType, identifier);
        } else {
            this.fetcherId = -1;
        }
    }

    @Provide
    public static InboxFetcherInternal provide(
        @NonNull Context context,
        @NonNull String userIdentifier,
        @NonNull String authenticationKey
    ) {
        return new InboxFetcherInternal(
            TrackerModuleProvider.get(),
            InboxDatasourceProvider.get(context),
            context,
            userIdentifier,
            authenticationKey
        );
    }

    @Provide
    public static InboxFetcherInternal provide(
        @NonNull Context context,
        @NonNull String userIdentifier,
        @NonNull String authenticationKey,
        boolean useCache
    ) {
        if (useCache) {
            return InboxFetcherInternalProvider.get(context, userIdentifier, authenticationKey);
        }

        return new InboxFetcherInternal(TrackerModuleProvider.get(), null, context, userIdentifier, authenticationKey);
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public void setFetchLimit(int fetchLimit) {
        this.fetchLimit = fetchLimit;
    }

    public void setFilterSilentNotifications(boolean filterSilentNotifications) {
        this.filterSilentNotifications = filterSilentNotifications;
    }

    public boolean isEndReached() {
        return endReached || fetchedNotifications.size() >= fetchLimit;
    }

    public void markAsRead(BatchInboxNotificationContent notification) {
        synchronized (fetchedNotifications) {
            String notifID = PrivateNotificationContentHelper.getInternalContent(notification).identifiers.identifier;
            InboxNotificationContentInternal internalNotif = null;

            // We MAY have an earlier copy of the object, so find it by identifier to make sure we have the freshest object from a trusted source
            // Who knows what could have happened with the "hidden" internal object?
            for (InboxNotificationContentInternal fetchedNotification : fetchedNotifications) {
                if (notifID.equals(fetchedNotification.identifiers.identifier)) {
                    internalNotif = fetchedNotification;
                    break;
                }
            }

            if (internalNotif != null) {
                for (JSONObject eventData : getEventDatas(internalNotif)) {
                    trackerModule.track(InternalEvents.INBOX_MARK_AS_READ, eventData);
                    InboxDatasourceProvider.get(context).markNotificationAsRead(notifID);
                    // Potentially the same object, but they might be different
                    PrivateNotificationContentHelper.getInternalContent(notification).isUnread = false;
                    internalNotif.isUnread = false;
                }
            } else {
                Logger.internal(
                    TAG,
                    "Could not find the specified notification (" + notifID + ") to be marked as read"
                );
            }
        }
    }

    public void markAllAsRead() {
        synchronized (fetchedNotifications) {
            if (fetchedNotifications.size() > 0) {
                for (JSONObject eventData : getEventDatas(fetchedNotifications.get(0))) {
                    trackerModule.track(InternalEvents.INBOX_MARK_ALL_AS_READ, eventData);
                }
                InboxDatasourceProvider.get(context).markAllAsRead(new Date().getTime(), fetcherId);
                for (InboxNotificationContentInternal content : fetchedNotifications) {
                    content.isUnread = false;
                }
            }
        }
    }

    public void markAsDeleted(BatchInboxNotificationContent notification) {
        synchronized (fetchedNotifications) {
            String notifID = PrivateNotificationContentHelper.getInternalContent(notification).identifiers.identifier;
            InboxNotificationContentInternal internalNotif = null;

            // We MAY have an earlier copy of the object, so find it by identifier to make sure we have the freshest object from a trusted source
            // Who knows what could have happened with the "hidden" internal object?
            for (InboxNotificationContentInternal fetchedNotification : fetchedNotifications) {
                if (notifID.equals(fetchedNotification.identifiers.identifier)) {
                    internalNotif = fetchedNotification;
                    break;
                }
            }

            if (internalNotif != null) {
                for (JSONObject eventData : getEventDatas(internalNotif)) {
                    trackerModule.track(InternalEvents.INBOX_MARK_AS_DELETED, eventData);
                    InboxDatasourceProvider.get(context).markNotificationAsDeleted(notifID);
                    // Potentially the same object, but they might be different
                    PrivateNotificationContentHelper.getInternalContent(notification).isDeleted = true;
                    internalNotif.isDeleted = true;
                    fetchedNotifications.remove(internalNotif);
                }
            } else {
                Logger.internal(
                    TAG,
                    "Could not find the specified notification (" + notifID + ") to be marked as deleted"
                );
            }
        }
    }

    @NonNull
    private List<BatchInboxNotificationContent> convertInternalModelsToPublic(
        @NonNull List<InboxNotificationContentInternal> privateNotifications
    ) {
        final List<BatchInboxNotificationContent> res = new ArrayList<>();
        for (InboxNotificationContentInternal privateNotification : privateNotifications) {
            final BatchInboxNotificationContent publicContent = PrivateNotificationContentHelper.getPublicContent(
                privateNotification
            );
            if (filterSilentNotifications && publicContent.isSilent()) {
                Logger.verbose(TAG, "Filtering silent notification");
                continue;
            }
            res.add(publicContent);
        }
        return res;
    }

    public void fetchNewNotifications(BatchInboxFetcher.OnNewNotificationsFetchedListener l) {
        if (l == null) {
            l =
                new BatchInboxFetcher.OnNewNotificationsFetchedListener() {
                    @Override
                    public void onFetchSuccess(
                        @NonNull List<BatchInboxNotificationContent> notifications,
                        boolean foundNewNotifications,
                        boolean endReached
                    ) {}

                    @Override
                    public void onFetchFailure(@NonNull String error) {}
                };
        }

        final BatchInboxFetcher.OnNewNotificationsFetchedListener userListener = l;

        InboxWebserviceListener wsClientListener = new InboxWebserviceListener() {
            @Override
            public void onSuccess(InboxWebserviceResponse response) {
                Logger.internal(TAG, "Inbox fetch success (new notifications) ----\n" + response.toString());
                try {
                    List<InboxNotificationContentInternal> addedNotifications = handleFetchSuccess(response, true);
                    userListener.onFetchSuccess(
                        convertInternalModelsToPublic(addedNotifications),
                        response.notifications.size() > 0,
                        !response.hasMore
                    );
                } catch (ResultHandlingError e) {
                    Logger.internal(TAG, "Failed to handle inbox fetch response", e);
                    userListener.onFetchFailure(e.getPublicMessage());
                }
            }

            @Override
            public void onFailure(@NonNull String error) {
                //TODO: Check if we shouldn't rather make it a generic public safe error
                userListener.onFetchFailure(error);
            }
        };
        fetch(null, wsClientListener);
    }

    public void fetchNextPage(BatchInboxFetcher.OnNextPageFetchedListener listener) {
        if (isEndReached()) {
            if (listener != null) {
                listener.onFetchFailure(
                    "The end of the inbox feed has been reached, either because you've reached the fetch limit, or because the server doesn't have anything left for you."
                );
            }
            return;
        }

        if (listener == null) {
            listener =
                new BatchInboxFetcher.OnNextPageFetchedListener() {
                    @Override
                    public void onFetchSuccess(
                        @NonNull List<BatchInboxNotificationContent> notifications,
                        boolean endReached
                    ) {}

                    @Override
                    public void onFetchFailure(@NonNull String error) {}
                };
        }

        final BatchInboxFetcher.OnNextPageFetchedListener finalListener = listener;
        InboxWebserviceListener wsClientListener = new InboxWebserviceListener() {
            @Override
            public void onSuccess(InboxWebserviceResponse response) {
                Logger.internal(TAG, "Inbox fetch success (next page) ----\n" + response.toString());
                try {
                    // If the cursor is null, we actually did something equivalent to "fetchNewNotifications",
                    // so handle the response appropriately
                    List<InboxNotificationContentInternal> addedNotifications = handleFetchSuccess(
                        response,
                        cursor == null
                    );
                    finalListener.onFetchSuccess(convertInternalModelsToPublic(addedNotifications), !response.hasMore);
                } catch (ResultHandlingError e) {
                    Logger.internal(TAG, "Failed to handle inbox fetch response", e);
                    finalListener.onFetchFailure(e.getPublicMessage());
                }
            }

            @Override
            public void onFailure(@NonNull String error) {
                //TODO: Check if we shouldn't rather make it a generic public safe error
                finalListener.onFetchFailure(error);
            }
        };
        fetch(cursor, wsClientListener);
    }

    private void fetch(@Nullable final String cursor, @NonNull final InboxWebserviceListener wsClientListener) {
        if (sync(cursor, wsClientListener)) {
            return;
        }

        if (this.fetcherType == FetcherType.USER_IDENTIFIER) {
            if (TextUtils.isEmpty(this.identifier)) {
                wsClientListener.onFailure("Inbox API Error: User identifier can't be null or empty");
                return;
            }

            if (TextUtils.isEmpty(this.authKey)) {
                wsClientListener.onFailure("Inbox API Error: Authentication Key can't be null or empty in user mode");
                return;
            }
        }

        fetchExecutor.execute(() -> {
            Context c = context != null ? context : RuntimeManagerProvider.get().getContext();
            if (c == null) {
                Logger.internal(TAG, "No context available");
                wsClientListener.onFailure(
                    "Internal error: No context available. If you are getting a Batch Inbox Fetcher in 'user identifier' mode, you can improve this by using the Batch.Inbox.getFetcher(Context,String,String) variant."
                );
                return;
            }

            if (!isDatabaseCleaned && datasource != null) {
                // Before the first fetch or sync, we clean old notifications for the DB
                isDatabaseCleaned = true;
                datasource.cleanDatabase();
            }

            try {
                // No need for the TaskExecutor, run the WS directly on this thread since it has to work serially
                new InboxFetchWebserviceClient(
                    c,
                    fetcherType,
                    identifier,
                    authKey,
                    maxPageSize,
                    cursor,
                    fetcherId,
                    wsClientListener
                )
                    .run();
            } catch (MalformedURLException e) {
                Logger.internal(TAG, "Could not start inbox fetcher ws: ", e);
                wsClientListener.onFailure("Internal network call error");
            }
        });
    }

    private boolean sync(@Nullable final String cursor, final InboxWebserviceListener wsClientListener) {
        if (datasource != null && fetcherId != -1) {
            List<InboxCandidateNotificationInternal> candidates = datasource.getCandidateNotifications(
                cursor,
                maxPageSize,
                fetcherId
            );

            if (!candidates.isEmpty()) {
                fetchExecutor.execute(() -> {
                    Context c = context != null ? context : RuntimeManagerProvider.get().getContext();
                    if (c == null) {
                        Logger.internal(TAG, "No context available");
                        wsClientListener.onFailure(
                            "Internal error: No context available. If you are getting a Batch Inbox Fetcher in 'user identifier' mode, you can improve this by using the Batch.Inbox.getFetcher(Context,String,String) variant."
                        );
                        return;
                    }

                    if (!isDatabaseCleaned && datasource != null) {
                        // Before the first fetch or sync, we clean old notifications for the DB
                        isDatabaseCleaned = true;
                        datasource.cleanDatabase();
                    }

                    try {
                        // No need for the TaskExecutor, run the WS directly on this thread since it has to work serially
                        new InboxSyncWebserviceClient(
                            c,
                            fetcherType,
                            identifier,
                            authKey,
                            maxPageSize,
                            cursor,
                            fetcherId,
                            candidates,
                            wsClientListener
                        )
                            .run();
                    } catch (MalformedURLException e) {
                        Logger.internal(TAG, "Could not start inbox fetcher ws: ", e);
                        wsClientListener.onFailure("Internal network call error");
                    }
                });

                return true;
            }
        }
        return false;
    }

    @NonNull
    private List<JSONObject> getEventDatas(InboxNotificationContentInternal notificationContent) {
        List<JSONObject> datas = new ArrayList<>();

        if (notificationContent == null) {
            return datas;
        }

        List<NotificationIdentifiers> contentIdentifiers = new ArrayList<>();
        contentIdentifiers.add(notificationContent.identifiers);
        if (notificationContent.duplicateIdentifiers != null) {
            contentIdentifiers.addAll(notificationContent.duplicateIdentifiers);
        }

        for (NotificationIdentifiers i : contentIdentifiers) {
            String customID = i.customID;
            if (TextUtils.isEmpty(customID) && fetcherType == FetcherType.USER_IDENTIFIER) {
                customID = identifier;
            }

            try {
                JSONObject data = new JSONObject();
                data.put("notificationId", i.identifier);

                if (i.installID != null) {
                    data.put("notificationInstallId", i.installID);
                }

                if (customID != null) {
                    data.put("notificationCustomId", customID);
                }

                if (i.additionalData != null) {
                    data.put("additionalData", i.additionalData);
                }

                datas.add(data);
            } catch (JSONException e) {
                Logger.internal(TAG, "Could not make inbox event data", e);
            }
        }

        return datas;
    }

    @NonNull
    public List<BatchInboxNotificationContent> getPublicFetchedNotifications() {
        synchronized (fetchedNotifications) {
            return convertInternalModelsToPublic(fetchedNotifications);
        }
    }

    private List<InboxNotificationContentInternal> handleFetchSuccess(
        InboxWebserviceResponse response,
        boolean askedForNewNotifications
    ) throws ResultHandlingError {
        if (response.notifications.size() == 0) {
            if (response.didTimeout) {
                throw new ResultHandlingError(
                    "Server did timeout, but returned no notifications at all.",
                    "The server could not complete your request in time. Please try again later."
                );
            } else if (response.hasMore) {
                throw new ResultHandlingError(
                    "Server didn't timeout, returned no notifications but told us there were more.",
                    "The server could not complete your request in time. Please try again later."
                );
            }
        }

        synchronized (fetchedNotifications) {
            if (askedForNewNotifications) {
                //TODO Improve on this later (v2)
                fetchedNotifications.clear();
            }

            // Also deduplicate the response notifications, so that the developer gets somewhat consistent data
            List<InboxNotificationContentInternal> addedNotifications = new ArrayList<>();

            // Deduplicate based on the sendID
            for (InboxNotificationContentInternal respNotif : response.notifications) {
                final String sendID = respNotif.identifiers.sendID;
                if (sendID == null) {
                    continue;
                }
                InboxNotificationContentInternal duplicateNotif = null;
                for (InboxNotificationContentInternal fetchedNotif : fetchedNotifications) {
                    if (sendID.equals(fetchedNotif.identifiers.sendID)) {
                        duplicateNotif = fetchedNotif;
                        break;
                    }
                }

                if (duplicateNotif != null) {
                    if (duplicateNotif.isDeleted) {
                        Logger.internal(
                            TAG,
                            "Receiving notification that has been deleted locally. " + respNotif.identifiers.identifier
                        );
                    }

                    if (respNotif.identifiers.identifier.equals(duplicateNotif.identifiers.identifier)) {
                        Logger.internal(
                            TAG,
                            "InboxFetcher: Got the exact same notification twice, skipping. " +
                            respNotif.identifiers.identifier
                        );
                    } else {
                        Logger.internal(
                            TAG,
                            "Merging notifications for sendID " +
                            sendID +
                            " (identifiers: " +
                            respNotif.identifiers.identifier +
                            ", " +
                            duplicateNotif.identifiers.identifier +
                            ")"
                        );

                        duplicateNotif.addDuplicateIdentifiers(respNotif.identifiers);

                        // If the duplicate is read, mark the other notification as read too
                        if (!respNotif.isUnread) {
                            duplicateNotif.isUnread = false;
                        }
                    }
                } else {
                    fetchedNotifications.add(respNotif);
                    addedNotifications.add(respNotif);
                }
            }

            cursor = response.cursor;
            endReached = !response.hasMore;
            return addedNotifications;
        }
    }

    private class ResultHandlingError extends Exception {

        private String publicMesssage;

        public ResultHandlingError(String debugMessage, String publicMesssage) {
            super(debugMessage);
            this.publicMesssage = publicMesssage;
        }

        public String getPublicMessage() {
            return publicMesssage;
        }
    }
}
