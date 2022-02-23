package com.batch.android.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.BatchNotificationSource;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the SQLite DAO
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class DatasourceTest {

    private String payloadJson =
        "{\"t\":\"c\",\"l\":\"https://batch.com\",\"i\":\"79946434-e5c3-4b22-1a1a-68e8f556740a\",\"od\":{\"n\":\"02521fa3e-70e4-11ea-b5ac-bf057dd56464\",\"ct\":\"2918f6a1d46ff48c9e74bfded5d5d9c9e\"}}";

    private InboxDatasource datasource;
    private Context appContext;

    @Before
    public void setUp() throws Exception {
        appContext = ApplicationProvider.getApplicationContext();
        datasource = new InboxDatasource(appContext);
        datasource.wipeData();
    }

    @After
    public void tearDown() throws Exception {
        datasource.close();
    }

    // -------------------------------------------------->

    /**
     * Test getting/creating fetcher ids
     */
    @Test
    public void testInsertFetcherIds() {
        assertEquals(-1, datasource.getFetcherID(FetcherType.USER_IDENTIFIER, null));
        assertEquals(-1, datasource.getFetcherID(FetcherType.USER_IDENTIFIER, ""));

        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_FETCHERS,
                null,
                InboxDatabaseHelper.COLUMN_DB_ID + " =?",
                new String[] { Long.toString(fetcherId) },
                null,
                null,
                null
            );

        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals(1, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_TYPE)));
        assertEquals(
            "test-custom-id",
            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_IDENTIFIER))
        );

        long fetcherId2 = datasource.getFetcherID(FetcherType.INSTALLATION, "b5baf3e0-a01f-11ea-111a-17c13e111be2");

        assertEquals(fetcherId2, fetcherId + 1);
        Cursor cursor2 = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_FETCHERS,
                null,
                InboxDatabaseHelper.COLUMN_DB_ID + " =?",
                new String[] { Long.toString(fetcherId2) },
                null,
                null,
                null
            );

        assertTrue(cursor2.moveToFirst());
        assertEquals(1, cursor2.getCount());
        assertEquals(0, cursor2.getInt(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_TYPE)));
        assertEquals(
            "b5baf3e0-a01f-11ea-111a-17c13e111be2",
            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_IDENTIFIER))
        );

        long fetcherId3 = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertEquals(fetcherId, fetcherId3);
    }

    @Test
    public void testGetNotificationTime() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = false;
        notification.isDeleted = false;

        assertTrue(datasource.insert(notification, fetcherId));

        long time = datasource.getNotificationTime("test-id");
        assertEquals(now.getTime(), time);

        long time2 = datasource.getNotificationTime("test-non-existing-id");
        assertEquals(-1, time2);
    }

    @Test
    public void testGetNotifications() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = false;
        notification.isDeleted = false;

        assertTrue(datasource.insert(notification, fetcherId));

        NotificationIdentifiers identifiers2 = new NotificationIdentifiers("test-id-2", "test-send-id-2");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2-2";
        InboxNotificationContentInternal notification2 = new InboxNotificationContentInternal(
            BatchNotificationSource.TRIGGER,
            now,
            payload,
            identifiers2
        );

        notification2.title = "test title";
        notification2.body = "test body";
        notification2.isUnread = false;
        notification2.isDeleted = false;

        assertTrue(datasource.insert(notification2, fetcherId));

        List<InboxNotificationContentInternal> notifications = datasource.getNotifications(
            Arrays.asList("test-id", "test-id-2"),
            fetcherId
        );
        assertEquals(2, notifications.size());
        assertEquals("test-id", notifications.get(0).identifiers.identifier);
        assertEquals("test-id-2", notifications.get(1).identifiers.identifier);
    }

    /**
     * Test a simple insert
     */
    @Test
    public void testSimpleInsert() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = false;
        notification.isDeleted = false;

        assertTrue(datasource.insert(notification, fetcherId));

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );

        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals("test-id", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
        assertEquals("test-send-id", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_SEND_ID)));
        assertEquals("test title", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_TITLE)));
        assertEquals("test body", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_BODY)));
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DELETED)));
        assertEquals(now.getTime(), cursor.getLong(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));
        assertEquals(
            new JSONObject(payload).toString(),
            cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_PAYLOAD))
        );

        Cursor cursor2 = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );

        assertTrue(cursor2.moveToFirst());
        assertEquals(1, cursor2.getCount());
        assertEquals(fetcherId, cursor2.getLong(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_ID)));
        assertEquals("test-id", cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
        assertEquals(
            "b5baf3e0-a01f-11ea-111a-17c13e111be2",
            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_INSTALL_ID))
        );
        assertNull(cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_CUSTOM_ID)));
    }

    @Test
    public void testUpdate() throws JSONException {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = true;
        notification.isDeleted = false;

        assertTrue(datasource.insert(notification, fetcherId));

        JSONObject updatePayload = new JSONObject("{\"notificationId\":\"test-id\"}");
        assertEquals("test-id", datasource.updateNotification(updatePayload, fetcherId));

        updatePayload = new JSONObject("{\"notificationId\":\"test-id\",\"read\":true}");
        assertEquals("test-id", datasource.updateNotification(updatePayload, fetcherId));

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );

        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));

        updatePayload =
            new JSONObject(
                "{\"notificationId\":\"test-id\",\"notificationTime\":123456000,\"sendId\":\"updated-send-id\",\"installId\":\"updated-install-id\",\"customId\":\"updated-custom-id\"}"
            );
        assertEquals("test-id", datasource.updateNotification(updatePayload, fetcherId));

        cursor =
            datasource
                .getDatabase()
                .query(
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                    null,
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                    new String[] { "test-id" },
                    null,
                    null,
                    null
                );

        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
        assertEquals(123456000, cursor.getLong(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));
        assertEquals("test-id", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
        assertEquals("updated-send-id", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_SEND_ID)));

        Cursor cursor2 = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );

        assertTrue(cursor2.moveToFirst());
        assertEquals(1, cursor2.getCount());
        assertEquals(fetcherId, cursor2.getLong(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_FETCHER_ID)));
        assertEquals("test-id", cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
        assertEquals(
            "updated-install-id",
            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_INSTALL_ID))
        );
        assertEquals(
            "updated-custom-id",
            cursor2.getString(cursor2.getColumnIndex(InboxDatabaseHelper.COLUMN_CUSTOM_ID))
        );
    }

    @Test
    public void testMarkNotificationAsDeleted() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);

        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            new Date(),
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = false;
        assertTrue(datasource.insert(notification, fetcherId));

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );
        cursor.moveToFirst();
        boolean deleted = cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DELETED)) != 0;
        assertFalse(deleted);

        datasource.markNotificationAsDeleted("test-id");

        cursor =
            datasource
                .getDatabase()
                .query(
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                    null,
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                    new String[] { "test-id" },
                    null,
                    null,
                    null
                );

        cursor.moveToFirst();
        deleted = cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DELETED)) != 0;
        assertTrue(deleted);
    }

    @Test
    public void testMarkNotificationAsRead() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);

        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            new Date(),
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = true;
        assertTrue(datasource.insert(notification, fetcherId));

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                new String[] { "test-id" },
                null,
                null,
                null
            );
        cursor.moveToFirst();
        boolean unread = cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)) != 0;
        assertTrue(unread);

        datasource.markNotificationAsRead("test-id");

        cursor =
            datasource
                .getDatabase()
                .query(
                    InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                    null,
                    InboxDatabaseHelper.COLUMN_NOTIFICATION_ID + " =?",
                    new String[] { "test-id" },
                    null,
                    null,
                    null
                );

        cursor.moveToFirst();
        unread = cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)) != 0;
        assertFalse(unread);
    }

    @Test
    public void testMarkAllAsRead() {
        long fetcherIdInstall = datasource.getFetcherID(FetcherType.INSTALLATION, "test-install-id");
        assertTrue(fetcherIdInstall > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long now = System.currentTimeMillis();
        long timeOffset = 0;
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            timeOffset -= 10000;
            Date time = new Date(now + timeOffset);

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

            assertTrue(datasource.insert(notification, fetcherIdInstall));
        }

        assertEquals(2, datasource.markAllAsRead(now - 25000, fetcherIdInstall));

        Cursor cursor = datasource
            .getDatabase()
            .query(
                InboxDatabaseHelper.TABLE_NOTIFICATIONS,
                null,
                InboxDatabaseHelper.COLUMN_UNREAD + " = ?",
                new String[] { "0" },
                null,
                null,
                null
            );

        assertEquals(2, cursor.getCount());

        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
        assertEquals("test-id-2", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));

        assertTrue(cursor.moveToNext());
        assertEquals(0, cursor.getInt(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_UNREAD)));
        assertEquals("test-id-3", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
    }

    @Test
    public void testDelete() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        Date now = new Date();
        NotificationIdentifiers identifiers = new NotificationIdentifiers("test-id", "test-send-id");
        identifiers.installID = "b5baf3e0-a01f-11ea-111a-17c13e111be2";
        InboxNotificationContentInternal notification = new InboxNotificationContentInternal(
            BatchNotificationSource.CAMPAIGN,
            now,
            payload,
            identifiers
        );

        notification.title = "test title";
        notification.body = "test body";
        notification.isUnread = false;
        notification.isDeleted = false;

        assertTrue(datasource.insert(notification, fetcherId));
        assertTrue(datasource.deleteNotifications(Collections.singletonList("test-id")));

        Cursor cursor = datasource
            .getDatabase()
            .query(InboxDatabaseHelper.TABLE_NOTIFICATIONS, null, null, null, null, null, null);
        assertEquals(0, cursor.getCount());

        Cursor cursor2 = datasource
            .getDatabase()
            .query(InboxDatabaseHelper.TABLE_FETCHERS_NOTIFICATIONS, null, null, null, null, null, null);
        assertEquals(0, cursor2.getCount());
    }

    @Test
    public void testGetCandidate() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long now = System.currentTimeMillis();
        long timeOffset = 0;
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            timeOffset -= 2500000;
            Date time = new Date(now + timeOffset);

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

        List<InboxCandidateNotificationInternal> candidates = datasource.getCandidateNotifications(
            "test-id-0",
            3,
            fetcherId
        );
        assertEquals(3, candidates.size());
        assertEquals("test-id-1", candidates.get(0).identifier);
        assertEquals("test-id-2", candidates.get(1).identifier);
        assertEquals("test-id-3", candidates.get(2).identifier);

        candidates = datasource.getCandidateNotifications("test-id-1", 2, fetcherId);
        assertEquals(2, candidates.size());
        assertEquals("test-id-2", candidates.get(0).identifier);
        assertEquals("test-id-3", candidates.get(1).identifier);

        candidates = datasource.getCandidateNotifications("test-id-3", 2, fetcherId);
        assertEquals(0, candidates.size());

        candidates = datasource.getCandidateNotifications(null, 3, fetcherId);
        assertEquals(3, candidates.size());
        assertEquals("test-id-0", candidates.get(0).identifier);
        assertEquals("test-id-1", candidates.get(1).identifier);
        assertEquals("test-id-2", candidates.get(2).identifier);
    }

    @Test
    public void testGetCandidateMultipleFetcher() {
        long fetcherIdCustom = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherIdCustom > 0);

        long fetcherIdInstall = datasource.getFetcherID(FetcherType.INSTALLATION, "test-install-id");
        assertTrue(fetcherIdInstall > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long now = System.currentTimeMillis();
        long timeOffset = 0;
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            timeOffset -= 2500000;
            Date time = new Date(now + timeOffset);

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

            assertTrue(datasource.insert(notification, fetcherIdCustom));
            assertTrue(datasource.insert(notification, fetcherIdInstall));
        }

        List<InboxCandidateNotificationInternal> candidates = datasource.getCandidateNotifications(
            "test-id-0",
            10,
            fetcherIdCustom
        );
        assertEquals(3, candidates.size());
        assertEquals("test-id-1", candidates.get(0).identifier);
        assertEquals("test-id-2", candidates.get(1).identifier);
        assertEquals("test-id-3", candidates.get(2).identifier);

        candidates = datasource.getCandidateNotifications("test-id-1", 10, fetcherIdInstall);
        assertEquals(2, candidates.size());
        assertEquals("test-id-2", candidates.get(0).identifier);
        assertEquals("test-id-3", candidates.get(1).identifier);

        candidates = datasource.getCandidateNotifications("test-id-3", 10, fetcherIdInstall);
        assertEquals(0, candidates.size());

        candidates = datasource.getCandidateNotifications(null, 3, fetcherIdCustom);
        assertEquals(3, candidates.size());
        assertEquals("test-id-0", candidates.get(0).identifier);
        assertEquals("test-id-1", candidates.get(1).identifier);
        assertEquals("test-id-2", candidates.get(2).identifier);
    }

    @Test
    public void testCleanDatabase() {
        long fetcherId = datasource.getFetcherID(FetcherType.USER_IDENTIFIER, "test-custom-id");
        assertTrue(fetcherId > 0);

        Map<String, String> payload = new HashMap<>();
        payload.put("com.batch", payloadJson);
        payload.put("hip", "hop");
        payload.put("test", "test");

        long now = System.currentTimeMillis();

        long[] times = { now - 7776000000L, now, now - 7779004900L, now + 123456789 };
        // Inserting 4 notifications
        for (int i = 0; i < 4; ++i) {
            Date time = new Date(times[i]);

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

        assertTrue(datasource.cleanDatabase());

        Cursor cursor = datasource
            .getDatabase()
            .query(InboxDatabaseHelper.TABLE_NOTIFICATIONS, null, null, null, null, null, null);

        assertEquals(2, cursor.getCount());

        assertTrue(cursor.moveToFirst());
        assertEquals(times[1], cursor.getLong(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));
        assertEquals("test-id-1", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));

        assertTrue(cursor.moveToNext());
        assertEquals(times[3], cursor.getLong(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_DATE)));
        assertEquals("test-id-3", cursor.getString(cursor.getColumnIndex(InboxDatabaseHelper.COLUMN_NOTIFICATION_ID)));
    }
}
