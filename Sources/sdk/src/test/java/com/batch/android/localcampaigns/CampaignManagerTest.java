package com.batch.android.localcampaigns;

import static com.batch.android.localcampaigns.model.LocalCampaign.SyncedJITResult;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.Batch;
import com.batch.android.core.DateProvider;
import com.batch.android.date.BatchDate;
import com.batch.android.date.UTCDate;
import com.batch.android.di.DI;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.DayOfWeek;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.model.QuietHours;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CampaignManagerTest {

    private Context context;
    private CampaignManager campaignManager;
    private JSONObject jsonCampaigns;
    private LocalCampaignsTracker tracker;

    @Before
    public void setUp() throws JSONException, IOException {
        context = ApplicationProvider.getApplicationContext();
        RuntimeManagerProvider.get().setContext(context);
        tracker = new LocalCampaignsTracker();
        campaignManager = new CampaignManager(tracker);
        readJsonFileForVersion(LocalCampaignsResponse.Version.MEP);
    }

    public void readJsonFileForVersion(LocalCampaignsResponse.Version version) throws IOException, JSONException {
        // Read fake campaigns file
        ClassLoader classLoader = CampaignManagerTest.class.getClassLoader();
        assert classLoader != null;
        InputStream inputStream = classLoader.getResourceAsStream(
            version == LocalCampaignsResponse.Version.MEP ? "fake_geo_campaigns.json" : "fake_cep_campaigns.json"
        );
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonCampaignsStringBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            jsonCampaignsStringBuilder.append(line);
        }
        jsonCampaigns = new JSONObject(jsonCampaignsStringBuilder.toString()).getJSONArray("queries").getJSONObject(0);
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        removeExistingSave();
    }

    @Test
    public void testSaveCampaignsMEP() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.hasSavedCampaigns(context));

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    @Test
    public void testSaveCampaignsCEP() throws NoSuchFieldException, IllegalAccessException, JSONException, IOException {
        readJsonFileForVersion(LocalCampaignsResponse.Version.CEP);
        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.hasSavedCampaigns(context));

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    /**
     * Test that the campaigns are correctly written to the cache, and can be reloaded.
     * <p>
     * This isn't really a "unit" test, but is a test that allows us to fully test the persistence
     * code until we rewrite it. The other test (testLoadCampaigns) is too narrow to catch some errors:
     * the persisting code is tested elsewhere, but persisting + reloading isn't tested as a unit
     * and can (actually did, twice) break even though both tests are correct.
     * <p>
     * Non regression test for ch24046.
     */
    @Test
    public void testSaveAndLoadCampaignsMEP() throws NoSuchFieldException, IllegalAccessException, JSONException {
        // Remove all campaigns, saved or loaded
        removeExistingSave();
        assertNull(campaignManager.getCampaignsVersion());
        assertFalse(campaignManager.hasSavedCampaigns(context));
        campaignManager.updateCampaignList(new ArrayList<>(), true);

        // Save using the code that will actually save the response in production, rather
        // than reimplementing it in the tests.
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.hasSavedCampaigns(context));
        assertEquals(0, campaignManager.getCampaignList().size());
        assertNull(campaignManager.getCappings());

        campaignManager.loadSavedCampaignResponse(context);
        assertEquals(1, campaignManager.getCampaignList().size());
        assertNotNull(campaignManager.getCappings());

        assertNotNull(campaignManager.getCampaignsVersion());
        assertEquals(LocalCampaignsResponse.Version.MEP, campaignManager.getCampaignsVersion());

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    @Test
    public void testSaveAndLoadCampaignsForCEP()
        throws NoSuchFieldException, IllegalAccessException, JSONException, IOException {
        // Remove all campaigns, saved or loaded
        readJsonFileForVersion(LocalCampaignsResponse.Version.CEP);
        removeExistingSave();
        assertNull(campaignManager.getCampaignsVersion());
        assertFalse(campaignManager.hasSavedCampaigns(context));
        campaignManager.updateCampaignList(new ArrayList<>(), true);

        // Save using the code that will actually save the response in production, rather
        // than reimplementing it in the tests.
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.hasSavedCampaigns(context));
        assertEquals(0, campaignManager.getCampaignList().size());
        assertNull(campaignManager.getCappings());

        campaignManager.loadSavedCampaignResponse(context);
        assertEquals(1, campaignManager.getCampaignList().size());
        assertNull(campaignManager.getCappings());

        assertNotNull(campaignManager.getCampaignsVersion());
        assertEquals(LocalCampaignsResponse.Version.CEP, campaignManager.getCampaignsVersion());

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    @Test
    public void testLoadCampaignsMEP() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.getCampaignList().isEmpty());
        assertNull(campaignManager.getCappings());
        assertNull(campaignManager.getCampaignsVersion());
        campaignManager.loadSavedCampaignResponse(context);
        assertFalse(campaignManager.getCampaignList().isEmpty());
        assertNotNull(campaignManager.getCappings());
        assertNotNull(campaignManager.getCampaignsVersion());
        assertEquals(LocalCampaignsResponse.Version.MEP, campaignManager.getCampaignsVersion());
    }

    @Test
    public void testLoadCampaignsCEP() throws NoSuchFieldException, IllegalAccessException, JSONException, IOException {
        readJsonFileForVersion(LocalCampaignsResponse.Version.CEP);
        removeExistingSave();
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        assertTrue(campaignManager.getCampaignList().isEmpty());
        assertNull(campaignManager.getCappings());
        assertNull(campaignManager.getCampaignsVersion());
        campaignManager.loadSavedCampaignResponse(context);
        assertFalse(campaignManager.getCampaignList().isEmpty());
        assertNull(campaignManager.getCappings());
        assertNotNull(campaignManager.getCampaignsVersion());
        assertEquals(LocalCampaignsResponse.Version.CEP, campaignManager.getCampaignsVersion());
    }

    @Test
    public void testLoadExpiredCampaigns() throws JSONException, NoSuchFieldException, IllegalAccessException {
        removeExistingSave();

        final BatchDate fakeCurrentDate = new UTCDate(0);
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);

        fakeCurrentDate.setTime(TimeUnit.DAYS.toMillis(15));

        campaignManager.loadSavedCampaignResponse(context);
        assertTrue(campaignManager.getCampaignList().isEmpty());
    }

    @Test
    public void testOpenCloseViewTracker() {
        campaignManager.closeViewTracker();

        assertFalse(tracker.isOpen());
        campaignManager.openViewTracker();
        assertTrue(tracker.isOpen());
        campaignManager.closeViewTracker();
        assertFalse(tracker.isOpen());
    }

    @Test
    public void testCapping()
        throws NoSuchFieldException, IllegalAccessException, ViewTrackerUnavailableException, JSONException {
        reloadCampaigns();

        // This campaign should have a capping equals to 3
        final String campaignName = "next_session_triggered_campaign";
        LocalCampaign campaign = null;
        final List<LocalCampaign> allCampaigns = campaignManager.getCampaignList();

        for (LocalCampaign currCampaign : allCampaigns) {
            if (campaignName.equals(currCampaign.id)) {
                campaign = currCampaign;
                break;
            }
        }

        assertNotNull(campaign);
        assertNotNull(campaign.capping);
        assertEquals(3, (int) campaign.capping);

        assertTrue(campaignManager.isCampaignDisplayable(campaign));
        assertFalse(campaignManager.isCampaignOverCapping(campaign, true));

        // Track 2 times
        campaignManager.getViewTracker().trackViewEvent(campaign.id, "test_user_id");
        campaignManager.getViewTracker().trackViewEvent(campaign.id, null);

        // 3 >= 2
        assertFalse(campaignManager.isCampaignOverCapping(campaign, true));

        // Track a third time
        campaignManager.getViewTracker().trackViewEvent(campaign.id, null);

        // We can't track again, campaign is over capping
        assertTrue(campaignManager.isCampaignOverCapping(campaign, true));
    }

    @Test
    public void testGlobalCapping()
        throws JSONException, NoSuchFieldException, IllegalAccessException, ViewTrackerUnavailableException {
        String customUserId = "test_user_id";

        // Load cappings from the json local campaign response (2/session & 1/h)
        reloadCampaigns();

        // Setting fake date provider
        final BatchDate fakeCurrentDate = new UTCDate(0);
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        DateProvider oldDateProvider = tracker.getDateProvider();
        tracker.setDateProvider(() -> fakeCurrentDate);

        assertFalse(campaignManager.isOverGlobalCappings());

        campaignManager.getViewTracker().trackViewEvent("campaign_id", customUserId);

        // We have reached time-based capping
        assertTrue(campaignManager.isOverGlobalCappings());

        // Adding 1h
        fakeCurrentDate.setTime(3600 * 1000);

        // time-based capping released
        assertFalse(campaignManager.isOverGlobalCappings());

        campaignManager.getViewTracker().trackViewEvent("campaign_id", null);

        // Adding 1h
        fakeCurrentDate.setTime(3600 * 1000);

        // We have reached the session capping
        assertTrue(campaignManager.isOverGlobalCappings());

        tracker.setDateProvider(oldDateProvider);
    }

    @Test
    public void testGracePeriod()
        throws NoSuchFieldException, IllegalAccessException, ViewTrackerUnavailableException, JSONException {
        reloadCampaigns();
        final String customUserId = "test_user_id";
        final String campaignName = "next_session_triggered_campaign";
        LocalCampaign campaign = null;
        final List<LocalCampaign> allCampaigns = campaignManager.getCampaignList();

        for (LocalCampaign currCampaign : allCampaigns) {
            if (campaignName.equals(currCampaign.id)) {
                campaign = currCampaign;
                break;
            }
        }

        assertNotNull(campaign);

        campaign.capping = 0;
        campaign.minimumDisplayInterval = 100;

        assertTrue(campaignManager.isCampaignDisplayable(campaign));
        assertFalse(campaignManager.isCampaignOverCapping(campaign, false));

        // Replace the SecureDateProvider with a fake DateProvider to test start/end times
        final BatchDate fakeCurrentDate = new UTCDate();
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        DateProvider oldDateProvider = tracker.getDateProvider();
        tracker.setDateProvider(() -> fakeCurrentDate);

        fakeCurrentDate.setTime(100L);

        // Test that the grace period works

        campaignManager.getViewTracker().trackViewEvent(campaign.id, customUserId);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(100100L);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(1001001L);
        assertFalse(campaignManager.isCampaignOverCapping(campaign, false));

        // Test that grace periods over 119,3 hours work
        // We had a bug were an int overflow broke them
        fakeCurrentDate.setTime(100L);
        campaign.minimumDisplayInterval = 2592000;
        campaignManager.getViewTracker().trackViewEvent(campaign.id, null);

        fakeCurrentDate.setTime(2592000100L);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(2592000101L);
        assertFalse(campaignManager.isCampaignOverCapping(campaign, false));

        tracker.setDateProvider(oldDateProvider);
    }

    @Test
    public void testQuietHours() throws NoSuchFieldException, IllegalAccessException {
        // Mock the DateProvider
        final BatchDate fakeCurrentDate = new UTCDate();
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        LocalCampaign campaign = new LocalCampaign();
        campaign.quietHours = new QuietHours(7, 15, 9, 0, Arrays.asList(DayOfWeek.valueOf(6), DayOfWeek.valueOf(0)));

        // Init calendar
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());

        // Ensure campaign is within quiet day (sunday)
        Assert.assertTrue(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is not within quiet day (wednesday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is within quiet hours (8am) between 7:15am and 9am
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertTrue(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is Not within quiet hours (7:14am) between 7:15am and 9am
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 14);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is within quiet hours (7:15am) between 7:15am and 9am
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 15);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertTrue(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is Not within quiet hours (9:00:15am) between 7:15am and 9am
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is Not within quiet hours (9:01am) between 7:15am and 9am
        calendar.set(Calendar.MINUTE, 1);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));

        // Update campaign quiet hours to be over night
        campaign.quietHours = new QuietHours(20, 0, 7, 0, Arrays.asList(DayOfWeek.valueOf(6), DayOfWeek.valueOf(0)));

        // Ensure campaign is within quiet day (6:00am) between 20am and 7am
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        calendar.set(Calendar.MINUTE, 0);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertTrue(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is not within quiet day (8:00am) between 20am and 7am
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));

        // Ensure campaign is within quiet day (22:00am) between 20am and 7am
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 0);
        fakeCurrentDate.setTime(calendar.getTimeInMillis());
        Assert.assertTrue(campaignManager.isCampaignWithinQuietHours(campaign));

        // Update campaign quiet hours to be null
        campaign.quietHours = null;
        Assert.assertFalse(campaignManager.isCampaignWithinQuietHours(campaign));
    }

    @Test
    public void testStartEndDate() throws NoSuchFieldException, IllegalAccessException, JSONException {
        reloadCampaigns();

        final String campaignName = "next_session_triggered_campaign";
        LocalCampaign campaign = null;
        final List<LocalCampaign> allCampaigns = campaignManager.getCampaignList();

        for (LocalCampaign currCampaign : allCampaigns) {
            if (campaignName.equals(currCampaign.id)) {
                campaign = currCampaign;
                break;
            }
        }

        assertNotNull(campaign);
        assertNotNull(campaign.startDate);
        assertNotNull(campaign.endDate);

        // Start Date : Mon Jan 23 10:14:04 GMT+01:00 2017
        assertEquals(1499960145L, campaign.startDate.getTime());
        // End Date : Sat Jan 23 14:38:15 GMT+01:00 2038
        assertEquals(2147866695000L, campaign.endDate.getTime());

        // Replace the SecureDateProvider with a fake DateProvider to test start/end times
        final BatchDate fakeCurrentDate = new UTCDate();
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        // Set a date before start date : Not displayable
        fakeCurrentDate.setTime(100L);
        assertFalse(campaignManager.isCampaignDisplayable(campaign));

        // Set a date between start date and end date : Displayable
        fakeCurrentDate.setTime(2000000000000L);
        assertTrue(campaignManager.isCampaignDisplayable(campaign));

        // Set a date before start date : Not displayable
        fakeCurrentDate.setTime(3000000000000L);
        assertFalse(campaignManager.isCampaignDisplayable(campaign));
    }

    @Test
    public void testPriority() throws NoSuchFieldException, IllegalAccessException {
        removeExistingSave();

        LocalCampaign highPriorityCampaign = createFakeCampaignWithPriority(50);
        List<LocalCampaign> fakeCampaigns = new ArrayList<>(3);
        fakeCampaigns.add(createFakeCampaignWithPriority(0));
        fakeCampaigns.add(createFakeCampaignWithPriority(10));
        fakeCampaigns.add(highPriorityCampaign);
        fakeCampaigns.add(createFakeCampaignWithPriority(20));
        fakeCampaigns.add(createFakeCampaignWithPriority(30));

        campaignManager.updateCampaignList(fakeCampaigns, true);

        List<LocalCampaign> eligibleCampaigns = campaignManager.getEligibleCampaignsSortedByPriority(trigger -> true);

        assertFalse(eligibleCampaigns.isEmpty());
        assertSame(highPriorityCampaign, eligibleCampaigns.get(0));
    }

    @Test
    public void testGetEligibleCampaignsRequiringSync() {
        List<LocalCampaign> fakeCampaigns = new ArrayList<>();
        fakeCampaigns.add(createFakeCampaignWithPriority(0));
        fakeCampaigns.add(createFakeCampaignWithPriority(10));
        Assert.assertEquals(0, campaignManager.getFirstEligibleCampaignsRequiringSync(fakeCampaigns).size());
        LocalCampaign jitCampaign = createFakeCampaignWithPriority(0);
        jitCampaign.requiresJustInTimeSync = true;
        fakeCampaigns.add(0, jitCampaign);
        Assert.assertEquals(jitCampaign, campaignManager.getFirstEligibleCampaignsRequiringSync(fakeCampaigns).get(0));
    }

    @Test
    public void testGetFirstCampaignNotRequiringJITSync() {
        List<LocalCampaign> fakeCampaigns = new ArrayList<>();
        LocalCampaign jitCampaign = createFakeCampaignWithPriority(0);
        jitCampaign.requiresJustInTimeSync = true;
        fakeCampaigns.add(jitCampaign);
        LocalCampaign expectedCampaign = createFakeCampaignWithPriority(0);
        fakeCampaigns.add(expectedCampaign);
        Assert.assertEquals(expectedCampaign, campaignManager.getFirstCampaignNotRequiringJITSync(fakeCampaigns));
    }

    @Test
    public void testIsJITServiceAvailable() {
        Assert.assertTrue(campaignManager.isJITServiceAvailable());
        Whitebox.setInternalState(campaignManager, "nextAvailableJITTimestamp", new Date().getTime() + 1000L);
        Assert.assertFalse(campaignManager.isJITServiceAvailable());
    }

    @Test
    public void testGetSyncedJITCampaignState() throws NoSuchFieldException, IllegalAccessException {
        // Get synced jit campaign from manager
        Map<String, SyncedJITResult> syncedCampaignsCached = campaignManager.getSyncedJITCampaignsCached();

        // Replace the SecureDateProvider with a fake DateProvider
        final BatchDate fakeCurrentDate = new UTCDate(0);
        Field dateProviderField = CampaignManager.class.getDeclaredField("dateProvider");
        dateProviderField.setAccessible(true);
        dateProviderField.set(campaignManager, (DateProvider) () -> fakeCurrentDate);

        // Ensure non-jit campaign is return as eligible
        LocalCampaign campaign = createFakeCampaignWithPriority(0);
        Assert.assertEquals(SyncedJITResult.State.ELIGIBLE, campaignManager.getSyncedJITCampaignState(campaign));

        // Ensure non-cached jit campaign requires a sync
        campaign.requiresJustInTimeSync = true;
        Assert.assertEquals(SyncedJITResult.State.REQUIRES_SYNC, campaignManager.getSyncedJITCampaignState(campaign));

        // Adding fake synced jit result in cache
        SyncedJITResult result = new SyncedJITResult(fakeCurrentDate.getTime());
        result.eligible = false;
        syncedCampaignsCached.put(campaign.id, result);

        // Ensure cached jit campaign is not eligible
        Assert.assertEquals(SyncedJITResult.State.NOT_ELIGIBLE, campaignManager.getSyncedJITCampaignState(campaign));

        // Ensure cached jit campaign is eligible
        result.eligible = true;
        Assert.assertEquals(SyncedJITResult.State.ELIGIBLE, campaignManager.getSyncedJITCampaignState(campaign));

        // Ensure cached jit campaign requires a new sync
        fakeCurrentDate.setTime(30_000);
        Assert.assertEquals(SyncedJITResult.State.REQUIRES_SYNC, campaignManager.getSyncedJITCampaignState(campaign));
    }

    @Test
    public void testUpdateSyncedJITCampaignsCached() {
        Map<String, SyncedJITResult> syncedCampaignsCached = campaignManager.getSyncedJITCampaignsCached();

        LocalCampaign campaign1 = createFakeCampaignWithPriority(1);
        campaign1.requiresJustInTimeSync = true;
        LocalCampaign campaign2 = createFakeCampaignWithPriority(2);
        campaign2.requiresJustInTimeSync = true;
        LocalCampaign campaign3 = createFakeCampaignWithPriority(3);
        campaign3.requiresJustInTimeSync = false;

        List<LocalCampaign> syncedCampaigns = new LinkedList<>();
        syncedCampaigns.add(campaign1);
        syncedCampaigns.add(campaign2);
        syncedCampaigns.add(campaign3);

        List<String> eligibleCampaignIds = Arrays.asList(campaign1.id, campaign3.id);
        campaignManager.updateSyncedJITCampaignsCached(syncedCampaigns, eligibleCampaignIds, false);

        // ensure only campaign1 and campaign2 are in the cache
        assertNotNull(syncedCampaignsCached);
        assertEquals(2, syncedCampaignsCached.size());
        assertTrue(syncedCampaignsCached.containsKey(campaign1.id));
        assertTrue(syncedCampaignsCached.containsKey(campaign2.id));
        assertFalse(syncedCampaignsCached.containsKey(campaign3.id));

        // ensure just campaign1 is eligible
        assertTrue(Objects.requireNonNull(syncedCampaignsCached.get(campaign1.id)).eligible);
        assertFalse(Objects.requireNonNull(syncedCampaignsCached.get(campaign2.id)).eligible);

        // ensure we did not remove any campaigns
        assertEquals(3, syncedCampaigns.size());

        campaignManager.updateSyncedJITCampaignsCached(syncedCampaigns, eligibleCampaignIds, true);

        // ensure only campaign1 and campaign2 are in the cache
        assertNotNull(syncedCampaignsCached);
        assertEquals(2, syncedCampaignsCached.size());
        assertTrue(syncedCampaignsCached.containsKey(campaign1.id));
        assertTrue(syncedCampaignsCached.containsKey(campaign2.id));
        assertFalse(syncedCampaignsCached.containsKey(campaign3.id));

        // ensure just campaign1 is eligible
        assertTrue(Objects.requireNonNull(syncedCampaignsCached.get(campaign1.id)).eligible);
        assertFalse(Objects.requireNonNull(syncedCampaignsCached.get(campaign2.id)).eligible);

        // ensure we removed non eligible campaigns from original list
        assertEquals(2, syncedCampaigns.size());
    }

    @Test
    public void testClearSyncedJITCampaignsCached() {
        DI.getInstance().addSingletonInstance(CampaignManager.class, campaignManager);
        UserModuleProvider.get().setCustomID(context, "test_user_id");
        assertEquals(0, campaignManager.getSyncedJITCampaignsCached().size());

        LocalCampaign campaign1 = createFakeCampaignWithPriority(1);
        campaign1.requiresJustInTimeSync = true;
        LocalCampaign campaign2 = createFakeCampaignWithPriority(2);
        campaign2.requiresJustInTimeSync = true;

        List<LocalCampaign> syncedCampaigns = new LinkedList<>();
        syncedCampaigns.add(campaign1);
        syncedCampaigns.add(campaign2);

        List<String> eligibleCampaignIds = Arrays.asList(campaign1.id, campaign2.id);
        campaignManager.updateSyncedJITCampaignsCached(syncedCampaigns, eligibleCampaignIds, false);

        assertNotNull(campaignManager.getSyncedJITCampaignsCached());
        assertEquals(2, campaignManager.getSyncedJITCampaignsCached().size());

        Batch.Profile.identify("test_user_id");

        // Ensure we don't clean cache when user doesn't change
        assertEquals(2, campaignManager.getSyncedJITCampaignsCached().size());

        Batch.Profile.identify("new_test_user_id");

        // Ensure we don't clean cache when user doesn't change
        assertEquals(0, campaignManager.getSyncedJITCampaignsCached().size());
    }

    private void removeExistingSave() throws NoSuchFieldException, IllegalAccessException {
        Field inappCampaignsFileNameField =
            CampaignManager.class.getDeclaredField("PERSISTENCE_LOCAL_CAMPAIGNS_FILE_NAME");
        inappCampaignsFileNameField.setAccessible(true);
        final String INAPP_CAMPAIGNS_FILE_NAME = (String) inappCampaignsFileNameField.get(null);

        campaignManager.closeViewTracker();
        context.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);
        campaignManager.openViewTracker();

        File tmpFile = new File(context.getCacheDir(), INAPP_CAMPAIGNS_FILE_NAME);
        if (tmpFile.exists()) {
            tmpFile.delete();
        }

        campaignManager.updateCampaignList(new ArrayList<>(0), true);
    }

    private void reloadCampaigns() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response);
        campaignManager.loadSavedCampaignResponse(context);
    }

    private LocalCampaign createFakeCampaignWithPriority(int priority) {
        LocalCampaign campaign = new LocalCampaign();
        campaign.id = UUID.randomUUID().toString();
        campaign.output = LandingOutputProvider.get(new JSONObject());
        campaign.priority = priority;
        campaign.startDate = new UTCDate(0);
        campaign.triggers.add(() -> "");
        return campaign;
    }
}
