package com.batch.android.inbox;

import static com.batch.android.inbox.FetcherType.USER_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.BatchNotificationSource;
import com.batch.android.di.providers.InboxDatasourceProvider;
import com.batch.android.json.JSONObject;
import com.batch.android.webservice.listener.InboxWebserviceListener;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

@RunWith(AndroidJUnit4.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*" })
@SmallTest
@PrepareForTest({ InboxSyncWebserviceClient.class })
public class InboxSyncWebserviceClientTest {

    private String payloadJson =
        "{\"t\":\"c\",\"l\":\"https://batch.com\",\"i\":\"test-send-id\",\"od\":{\"n\":\"02521fa3e-70e4-11ea-b5ac-bf057dd56464\",\"ct\":\"2918f6a1d46ff48c9e74bfded5d5d9c9e\"}}";

    private Context appContext;
    private InboxDatasource datasource;

    @Before
    public void setUp() {
        appContext = ApplicationProvider.getApplicationContext();
        datasource = InboxDatasourceProvider.get(appContext);
    }

    @After
    public void tearDown() throws Exception {
        datasource.wipeData();
        datasource.close();
    }

    @Test
    public void testMissingNotificationInCache() throws Exception {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long timeOffset = 0;
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            timeOffset += 3600000;
            Date time = new Date(timeOffset);

            NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id-" + i, "test-send-id-" + i);
            identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
            InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
                BatchNotificationSource.CAMPAIGN,
                time,
                payload,
                identifiers
            );

            notification.title = "test title";
            notification.body = "test body";
            notification.isUnread = false;
            notification.isDeleted = false;

            assertTrue(datasource.insert(notification, fetcherId));
        }

        // We now have 4 notifications in cache: test-id-0, test-id-1, test-id-2, test-id-3
        // Notifications timestamps: 3600000, 7200000, 10800000, 14400000
        // We'll sync 2 notifications from cursor test-id-3
        String cursor = "test-id-3";
        int limit = 2;

        List<InboxCandidateNotificationInternal> candidates = datasource.getCandidateNotifications(
            cursor,
            limit,
            fetcherId
        );
        assertEquals(limit, candidates.size());
        assertEquals("test-id-2", candidates.get(0).identifier);
        assertEquals("test-id-1", candidates.get(1).identifier);

        InboxSyncWebserviceClient client = PowerMockito.spy(
            new InboxSyncWebserviceClient(
                appContext,
                USER_IDENTIFIER,
                "test-id",
                "test-auth",
                limit,
                cursor,
                fetcherId,
                candidates,
                new InboxWebserviceListener() {
                    @Override
                    public void onSuccess(InboxWebserviceResponse result) {
                        assertEquals("test-id-2", result.cursor);
                        assertTrue(result.hasMore);
                        assertFalse(result.didTimeout);
                        assertEquals(2, result.notifications.size());

                        assertEquals("test-id-2.5", result.notifications.get(0).identifiers.identifier);
                        assertEquals("test-id-2", result.notifications.get(1).identifiers.identifier);

                        Cursor cursor = datasource
                            .getDatabase()
                            .query(
                                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                                null,
                                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                                new String[] { "test-id-2.5" },
                                null,
                                null,
                                null
                            );

                        assertTrue(cursor.moveToFirst());
                        assertEquals(1, cursor.getCount());
                        assertEquals(
                            "test-id-2.5",
                            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID))
                        );
                        assertEquals(
                            "test-send-id-2.5",
                            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_SEND_ID))
                        );
                        assertEquals(
                            "test title",
                            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_TITLE))
                        );
                        assertEquals(
                            "test body",
                            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_BODY))
                        );
                        assertEquals(1, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
                        assertEquals(10900000, cursor.getLong(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));

                        Cursor cursor2 = datasource
                            .getDatabase()
                            .query(
                                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                                null,
                                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                                new String[] { "test-id-2.5" },
                                null,
                                null,
                                null
                            );

                        assertTrue(cursor2.moveToFirst());
                        assertEquals(1, cursor2.getCount());
                        assertEquals(
                            fetcherId,
                            cursor2.getLong(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_ID))
                        );
                        assertEquals(
                            "test-id-2.5",
                            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID))
                        );
                        assertEquals(
                            "b5baf3e0-a01f-11ea-111a-17c13e111be2",
                            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_INSTALL_ID))
                        );
                        assertNull(cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_CUSTOM_ID)));
                    }

                    @Override
                    public void onFailure(@NonNull String error) {
                        fail();
                    }
                }
            )
        );

        String fakeServerResponse =
            "{\"notifications\":[{\"installId\":\"b5baf3e0-a01f-11ea-111a-17c13e111be2\",\"notificationId\":\"test-id-2.5\",\"notificationTime\":10900000,\"sendId\":\"test-send-id-2.5\",\"payload\":{\"com.batch\":{\"t\":\"t\",\"l\":\"https://google.com\",\"i\":\"6y4g8guj-u1585829353322_68f3\",\"od\":{\"n\":\"c44d6340-74da-11ea-b3b3-8dc99181b65a\"}},\"msg\":\"test body\",\"title\":\"test title\"}},{\"notificationId\":\"test-id-2\"}],\"cache\":{\"lastMarkAllAsRead\":1585233902218},\"hasMore\":true,\"timeout\":false,\"cursor\":\"test-id-2\"}";
        PowerMockito.doReturn(new JSONObject(fakeServerResponse)).when(client, "getBasicJsonResponseBody");
        client.run();
    }

    @Test
    public void testUpdatingNotificationInCache() throws Exception {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long timeOffset = 0;
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            timeOffset += 3600000;
            Date time = new Date(timeOffset);

            NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id-" + i, "test-send-id-" + i);
            identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
            InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
                BatchNotificationSource.CAMPAIGN,
                time,
                payload,
                identifiers
            );

            notification.title = "test title";
            notification.body = "test body";
            notification.isUnread = true;
            notification.isDeleted = false;

            assertTrue(datasource.insert(notification, fetcherId));
        }

        // We now have 4 notifications in cache: test-id-0, test-id-1, test-id-2, test-id-3
        // Notifications timestamps: 3600000, 7200000, 10800000, 14400000
        // We'll sync 2 notifications from cursor test-id-3
        String cursor = "test-id-3";
        int limit = 2;

        List<InboxCandidateNotificationInternal> candidates = datasource.getCandidateNotifications(
            cursor,
            limit,
            fetcherId
        );
        assertEquals(limit, candidates.size());
        assertEquals("test-id-2", candidates.get(0).identifier);
        assertTrue(candidates.get(0).isUnread);
        assertEquals("test-id-1", candidates.get(1).identifier);

        InboxSyncWebserviceClient client = PowerMockito.spy(
            new InboxSyncWebserviceClient(
                appContext,
                USER_IDENTIFIER,
                "test-id",
                "test-auth",
                limit,
                cursor,
                fetcherId,
                candidates,
                new InboxWebserviceListener() {
                    @Override
                    public void onSuccess(InboxWebserviceResponse result) {
                        assertEquals("test-id-1", result.cursor);
                        assertTrue(result.hasMore);
                        assertFalse(result.didTimeout);
                        assertEquals(2, result.notifications.size());

                        assertEquals("test-id-2", result.notifications.get(0).identifiers.identifier);
                        assertFalse(result.notifications.get(0).isUnread);

                        assertEquals("test-id-1", result.notifications.get(1).identifiers.identifier);
                        assertEquals("test-install-id-1-updated", result.notifications.get(1).identifiers.installID);
                        assertEquals("test-custom-id-1-updated", result.notifications.get(1).identifiers.customID);
                        assertEquals("test-send-id-1-updated", result.notifications.get(1).identifiers.sendID);
                        assertEquals(7300000, result.notifications.get(1).date.getTime());

                        Cursor cursor = datasource
                            .getDatabase()
                            .query(
                                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                                null,
                                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                                new String[] { "test-id-2" },
                                null,
                                null,
                                null
                            );

                        assertTrue(cursor.moveToFirst());
                        assertEquals(1, cursor.getCount());
                        assertEquals(
                            "test-id-2",
                            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID))
                        );
                        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));

                        Cursor cursor2 = datasource
                            .getDatabase()
                            .query(
                                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                                null,
                                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                                new String[] { "test-id-1" },
                                null,
                                null,
                                null
                            );

                        assertTrue(cursor2.moveToFirst());
                        assertEquals(1, cursor2.getCount());
                        assertEquals(
                            "test-id-1",
                            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID))
                        );
                        assertEquals(1, cursor2.getInt(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
                        assertEquals(7300000, cursor2.getInt(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));
                    }

                    @Override
                    public void onFailure(@NonNull String error) {
                        fail();
                    }
                }
            )
        );

        String fakeServerResponse =
            "{\"notifications\":[{\"read\":true,\"notificationId\":\"test-id-2\"},{\"installId\":\"test-install-id-1-updated\",\"customId\":\"test-custom-id-1-updated\",\"notificationId\":\"test-id-1\",\"notificationTime\":7300000,\"sendId\":\"test-send-id-1-updated\"}],\"hasMore\":true,\"timeout\":false,\"cursor\":\"test-id-1\"}";
        PowerMockito.doReturn(new JSONObject(fakeServerResponse)).when(client, "getBasicJsonResponseBody");
        client.run();
    }
}
