package com.batch.android.localcampaigns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import com.batch.android.core.DateProvider;
import com.batch.android.date.BatchDate;
import com.batch.android.date.UTCDate;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class CampaignManagerTest {

    private Context context;
    private CampaignManager campaignManager;
    private JSONObject jsonCampaigns;
    private LocalCampaignsSQLTracker tracker;

    @Before
    public void setUp() throws IOException, JSONException {
        context = ApplicationProvider.getApplicationContext();
        RuntimeManagerProvider.get().setContext(context);

        tracker = new LocalCampaignsSQLTracker();
        campaignManager = new CampaignManager(tracker);

        // Read fake campaigns file
        ClassLoader classLoader = CampaignManagerTest.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("fake_geo_campaigns.json");
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
    public void testSaveCampaigns() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response.getCampaigns());
        assertTrue(campaignManager.hasSavedCampaigns(context));

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    /**
     * Test that the campaigns are correctly written to the cache, and can be reloaded.
     *
     * This isn't really a "unit" test, but is a test that allows us to fully test the persistence
     * code until we rewrite it. The other test (testLoadCampaigns) is too narrow to catch some errors:
     * the persisting code is tested elsewhere, but persisting + reloading isn't tested as a unit
     * and can (actually did, twice) break even though both tests are correct.
     *
     * Non regression test for ch24046.
     */
    @Test
    public void testSaveAndLoadCampaigns() throws NoSuchFieldException, IllegalAccessException, JSONException {
        // Remove all campaigns, saved or loaded
        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
        campaignManager.updateCampaignList(new ArrayList<>());

        // Save using the code that will actually save the response in production, rather
        // than reimplementing it in the tests.
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response.getCampaigns());
        assertTrue(campaignManager.hasSavedCampaigns(context));
        assertEquals(0, campaignManager.getCampaignList().size());

        campaignManager.loadSavedCampaignResponse(context);
        assertEquals(1, campaignManager.getCampaignList().size());

        removeExistingSave();
        assertFalse(campaignManager.hasSavedCampaigns(context));
    }

    @Test
    public void testLoadCampaigns() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response.getCampaigns());
        assertTrue(campaignManager.getCampaignList().isEmpty());

        campaignManager.loadSavedCampaignResponse(context);
        assertFalse(campaignManager.getCampaignList().isEmpty());
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
        campaignManager.getViewTracker().trackViewEvent(campaign.id);
        campaignManager.getViewTracker().trackViewEvent(campaign.id);

        // 3 >= 2
        assertFalse(campaignManager.isCampaignOverCapping(campaign, true));

        // Track a third time
        campaignManager.getViewTracker().trackViewEvent(campaign.id);

        // We can't track again, campaign is over capping
        assertTrue(campaignManager.isCampaignOverCapping(campaign, true));
    }

    @Test
    public void testGracePeriod()
        throws NoSuchFieldException, IllegalAccessException, ViewTrackerUnavailableException, JSONException {
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

        campaignManager.getViewTracker().trackViewEvent(campaign.id);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(100100L);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(1001001L);
        assertFalse(campaignManager.isCampaignOverCapping(campaign, false));

        // Test that grace periods over 119,3 hours work
        // We had a bug were an int overflow broke them
        fakeCurrentDate.setTime(100L);
        campaign.minimumDisplayInterval = 2592000;
        campaignManager.getViewTracker().trackViewEvent(campaign.id);

        fakeCurrentDate.setTime(2592000100L);
        assertTrue(campaignManager.isCampaignOverCapping(campaign, false));

        fakeCurrentDate.setTime(2592000101L);
        assertFalse(campaignManager.isCampaignOverCapping(campaign, false));

        tracker.setDateProvider(oldDateProvider);
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

        campaignManager.updateCampaignList(fakeCampaigns);

        LocalCampaign campainToDisplay = campaignManager.getCampaignToDisplay(trigger -> true);

        assertSame(highPriorityCampaign, campainToDisplay);
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

        campaignManager.updateCampaignList(new ArrayList<>(0));
    }

    private void reloadCampaigns() throws NoSuchFieldException, IllegalAccessException, JSONException {
        removeExistingSave();
        LocalCampaignsResponse response = new LocalCampaignsResponseDeserializer(jsonCampaigns).deserialize();
        campaignManager.saveCampaigns(context, response.getCampaigns());
        campaignManager.loadSavedCampaignResponse(context);
    }

    private LocalCampaign createFakeCampaignWithPriority(int priority) {
        LocalCampaign campaign = new LocalCampaign();
        campaign.id = UUID.randomUUID().toString();
        campaign.output = LandingOutputProvider.get(new JSONObject());
        campaign.priority = priority;
        campaign.startDate = new UTCDate(0);
        campaign.triggers.add(
            new LocalCampaign.Trigger() {
                @Override
                public String getType() {
                    return "";
                }
            }
        );
        return campaign;
    }
}
