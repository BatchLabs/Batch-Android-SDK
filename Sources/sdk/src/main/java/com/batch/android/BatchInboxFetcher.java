package com.batch.android;

import android.os.Handler;
import androidx.annotation.NonNull;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.inbox.InboxFetcherInternal;
import java.util.List;

/**
 * BatchInboxFetcher allows you to fetch notifications that have been sent to a user (or installation, more on that later) in their raw form,
 * allowing you to display them in a list, for example. This is also useful to display messages to users that disabled notifications.<br/>
 * <p>
 * Once you get your BatchInboxFetcher instance, you should call {@link #fetchNewNotifications(OnNewNotificationsFetchedListener)} to fetch the initial page of messages: nothing is done automatically.
 * This method is also useful to refresh the list.<br/>
 * <p>
 * In an effort to minimize network and memory usage, messages are fetched by page (batches of messages):
 * this allows you to easily create an infinite list, loading more messages on demand.<br/>
 * While you can configure the maximum number of messages you want in a page, the actual number of returned messages can differ, as the SDK may filter some of the messages returned by the server (such as duplicate notifications, etc...).<br/>
 * <p>
 * As BatchInboxFetcher caches answers from the server, instances of this class should be tied to the lifecycle of the UI consuming it (if applicable).<br/>
 * For example, you should keep a reference to this object during your Activity's entire life.<br/>
 * Another reason to keep the object around, is that you cannot mark a message as read with another BatchInbox instance that the one
 * that gave you the message in the first place.<br/>
 * <p>
 * A BatchInboxFetcher instance will hold to all fetched messages: be careful of how long you're keeping the instances around.<br/>
 * You can also set a upper messages limit, after which BatchInbox will stop fetching new messages, even if you call fetchNextPage.<br/>
 * <p>
 * Note: You will always be called back on the thread that you instantiated BatchInboxFetcher on. use {@link #setHandlerOverride(Handler)} if you want to change that behaviour.
 */

@PublicSDK
public class BatchInboxFetcher {

    private InboxFetcherInternal impl;

    private Handler handler = new Handler();

    BatchInboxFetcher(InboxFetcherInternal internal) {
        this.impl = internal;
    }

    /**
     * Number of notifications to fetch on each call, up to 100 messages per page.
     * Note that the actual count of fetched messages might differ from the value you've set here.
     */
    public void setMaxPageSize(int maxPageSize) {
        impl.setMaxPageSize(maxPageSize);
    }

    /**
     * Maximum number of notifications to fetch. This allows you to let Batch manage the upper limit itself, so you can be sure not to use a crazy amount of memory.
     * If you want to fetch unlimited messages, set this property to 0.
     *
     * @param fetchLimit Limit of notifications to fetch. Default: 200
     */
    public void setFetchLimit(int fetchLimit) {
        impl.setFetchLimit(fetchLimit);
    }

    /**
     * Sets whether the SDK should filter silent notifications (pushes that don't result in a message
     * being shown to the user).
     *
     * Default: true
     */
    public void setFilterSilentNotifications(boolean filterSilentNotifications) {
        impl.setFilterSilentNotifications(filterSilentNotifications);
    }

    /**
     * Returns whether all of the user or installation's notifications have been fetched.
     * If this method returns true, calling fetchNextPage will always return an error, as there is nothing left to fetch.
     * Also artificially returns true if the maximum number of fetched messages has been reached.
     */
    public boolean hasMore() {
        return !impl.isEndReached();
    }

    /**
     * Mark a specific notification as read.
     *
     * @param notification The notification to be marked as read.
     */
    public void markAsRead(BatchInboxNotificationContent notification) {
        impl.markAsRead(notification);
    }

    /**
     * Marks all notifications as read.
     */
    public void markAllAsRead() {
        impl.markAllAsRead();
    }

    /**
     * Mark a specific notification as deleted.
     *
     * @param notification The notification to be marked as deleted.
     */
    public void markAsDeleted(BatchInboxNotificationContent notification) {
        impl.markAsDeleted(notification);
    }

    /**
     * Returns a copy of all notifications that have been fetched until now, ordered by reverse chronological order (meaning that the first message is the newest one, and the last one the oldest).
     * Note that this array will be empty until you call {@link #fetchNewNotifications(OnNewNotificationsFetchedListener)}, and will only grow on subsequent fetches.
     * This operation is quite extensive: you should cache this result until you call fetch*.
     */
    @NonNull
    public List<BatchInboxNotificationContent> getFetchedNotifications() {
        return impl.getPublicFetchedNotifications();
    }

    /**
     * Fetch new notifications.<br/>
     * While {@link #fetchNextPage(OnNextPageFetchedListener)} is used to fetch older notifications than the ones currently loaded, this method checks for new notifications. For example, this is the method you would call on initial load, or on a "pull to refresh".
     * If new notifications are found, the previously loaded ones will be kept if possible, but might be cleared to ensure consistency. For example, if a gap were to happen because of a refresh, old notifications would be removed from the cache.
     *
     * @param listener An optional listener can be executed on success or failure with either the fetched notifications or the detailed error.
     */
    public void fetchNewNotifications(@NonNull OnNewNotificationsFetchedListener listener) {
        // Change the listener to make sure we call the developer back on the thread that they called us on
        if (listener != null && handler != null) {
            final OnNewNotificationsFetchedListener originalListener = listener;
            listener =
                new OnNewNotificationsFetchedListener() {
                    @Override
                    public void onFetchSuccess(
                        @NonNull final List<BatchInboxNotificationContent> notifications,
                        final boolean foundNewNotifications,
                        final boolean endReached
                    ) {
                        handler.post(() ->
                            originalListener.onFetchSuccess(notifications, foundNewNotifications, endReached)
                        );
                    }

                    @Override
                    public void onFetchFailure(@NonNull final String error) {
                        handler.post(() -> originalListener.onFetchFailure(error));
                    }
                };
        }
        impl.fetchNewNotifications(listener);
    }

    /**
     * Fetch a page of notifications.<br/>
     * Calling this method when no messages have been loaded will be equivalent to calling {@link #fetchNewNotifications(OnNewNotificationsFetchedListener)}<br/>
     * <b>Warning: callbacks might not be called on the thread you're expecting. See {@link #setHandlerOverride(Handler)}</b>
     *
     * @param listener An optional listener can be executed on success or failure with either the fetched notifications or the detailed error.
     */
    public void fetchNextPage(OnNextPageFetchedListener listener) {
        if (listener != null && handler != null) {
            final OnNextPageFetchedListener originalListener = listener;
            listener =
                new OnNextPageFetchedListener() {
                    @Override
                    public void onFetchSuccess(
                        @NonNull final List<BatchInboxNotificationContent> notifications,
                        final boolean endReached
                    ) {
                        handler.post(() -> originalListener.onFetchSuccess(notifications, endReached));
                    }

                    @Override
                    public void onFetchFailure(@NonNull final String error) {
                        handler.post(() -> originalListener.onFetchFailure(error));
                    }
                };
        }
        impl.fetchNextPage(listener);
    }

    /**
     * Specify a handler to post the callbacks on. By default, this is the thread that you've created
     * the inbox fetcher on.
     *
     * @param handler Handler to post the callbacks on
     */
    public void setHandlerOverride(@NonNull Handler handler) {
        if (handler != null) {
            this.handler = handler;
        }
    }

    @PublicSDK
    public interface OnNewNotificationsFetchedListener {
        void onFetchSuccess(
            @NonNull List<BatchInboxNotificationContent> notifications,
            boolean foundNewNotifications,
            boolean endReached
        );

        //TODO maybe add a typed failure reason
        void onFetchFailure(@NonNull String error);
    }

    @PublicSDK
    public interface OnNextPageFetchedListener {
        void onFetchSuccess(@NonNull List<BatchInboxNotificationContent> notifications, boolean endReached);

        //TODO maybe add a typed failure reason
        void onFetchFailure(@NonNull String error);
    }
}
