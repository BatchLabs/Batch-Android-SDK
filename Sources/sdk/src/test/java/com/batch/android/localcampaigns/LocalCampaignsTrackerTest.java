package com.batch.android.localcampaigns;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.DateProvider;
import com.batch.android.core.SystemDateProvider;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignsTrackerTest {

    private Context appContext;
    private Field dbHelperField;
    private Field databaseField;

    @Before
    public void setUp() throws NoSuchFieldException {
        appContext = ApplicationProvider.getApplicationContext();

        dbHelperField = LocalCampaignsSQLTracker.class.getDeclaredField("dbHelper");
        databaseField = LocalCampaignsSQLTracker.class.getDeclaredField("database");

        dbHelperField.setAccessible(true);
        databaseField.setAccessible(true);
    }

    // Check if DBHelper was created in open()
    @Test
    public void testOpen() throws IllegalAccessException {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();

        Assert.assertNull(dbHelperField.get(tracker));
        tracker.open(appContext);
        Assert.assertNotNull(dbHelperField.get(tracker));
    }

    // Check if Database was closed in close()
    @Test
    public void testClose() throws IllegalAccessException, ViewTrackerUnavailableException {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        // This is going to init the database field
        tracker.getViewEvent("fake_event");

        SQLiteDatabase database = (SQLiteDatabase) databaseField.get(tracker);
        Assert.assertNotNull(database);

        tracker.close();

        Assert.assertNull(databaseField.get(tracker));
        Assert.assertFalse(database.isOpen());
    }

    @Test
    public void testTrackEventForCampaignIDAndCount() throws ViewTrackerUnavailableException {
        // Clear database
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);

        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        final String FAKE_CAMPAIGN_ID_1 = "MyCampaign1";
        final String FAKE_CAMPAIGN_ID_2 = "MyCampaign2";

        // Never tracked
        Assert.assertEquals(0, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track one time
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        Assert.assertEquals(1, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track three times
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        Assert.assertEquals(4, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);

        // Track another campaign
        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_2);

        Assert.assertEquals(4, tracker.getViewEvent(FAKE_CAMPAIGN_ID_1).count);
        Assert.assertEquals(1, tracker.getViewEvent(FAKE_CAMPAIGN_ID_2).count);

        tracker.close();
    }

    @Test
    public void testCampaignLastOccurence() throws ViewTrackerUnavailableException {
        // Clear database
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);

        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        final String FAKE_CAMPAIGN_ID_1 = "MyCampaign1";
        final String FAKE_CAMPAIGN_ID_2 = "MyCampaign2";

        // Never tracked
        Assert.assertEquals(0, tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1));

        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);
        long firstTrackTime = tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1);

        // Tracked, time != 0
        Assert.assertNotSame(0, firstTrackTime);

        tracker.trackViewEvent(FAKE_CAMPAIGN_ID_1);

        // Tracked another time (time > to previous time)
        long secondTrackTime = tracker.campaignLastOccurrence(FAKE_CAMPAIGN_ID_1);

        System.out.println("times :: " + firstTrackTime + "    " + secondTrackTime);
        Assert.assertTrue(firstTrackTime < secondTrackTime);

        tracker.close();
    }

    @Test
    public void testGetNumberOfViewEventsSince() throws ViewTrackerUnavailableException {
        // Clear database
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);

        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        tracker.open(appContext);

        DateProvider dateProvider = new SystemDateProvider();

        //0 tracked view events
        Assert.assertEquals(
            0,
            tracker.getNumberOfViewEventsSince(dateProvider.getCurrentDate().getTime() - (60 * 1000))
        );

        // track view event
        tracker.trackViewEvent("campaign_id");

        long timestamp = dateProvider.getCurrentDate().getTime();
        // 1 tracked view event since 1 sec
        Assert.assertEquals(1, tracker.getNumberOfViewEventsSince(timestamp - (1000)));

        // Adding 1 sec
        timestamp += 1000;

        // 0 tracked view event since 1 sec
        Assert.assertEquals(0, tracker.getNumberOfViewEventsSince(timestamp - (1000)));

        tracker.close();
    }

    @Test
    // Tests that the db being not opened results in a specific exception
    public void testUnavailabilityException() {
        LocalCampaignsSQLTracker tracker = new LocalCampaignsSQLTracker();
        try {
            tracker.trackViewEvent("foo");
        } catch (ViewTrackerUnavailableException expected) {
            return;
        }
        Assert.fail("A ViewTrackerUnavailableException should have been thrown");
    }

    @Test
    public void testSessionViewsCount() throws ViewTrackerUnavailableException {
        LocalCampaignsTracker tracker = new LocalCampaignsTracker();
        tracker.open(appContext);
        Assert.assertEquals(0, tracker.getSessionViewsCount());
        tracker.trackViewEvent("campaign_id");
        Assert.assertEquals(1, tracker.getSessionViewsCount());
        tracker.close();
        tracker.resetSessionViewsCount();
        Assert.assertEquals(0, tracker.getSessionViewsCount());
    }
}
