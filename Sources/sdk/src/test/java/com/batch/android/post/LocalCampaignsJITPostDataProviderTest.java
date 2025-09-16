package com.batch.android.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.json.JSONUtils;
import com.batch.android.localcampaigns.LocalCampaignTrackDbHelper;
import com.batch.android.localcampaigns.LocalCampaignsSQLTracker;
import com.batch.android.localcampaigns.LocalCampaignsTracker;
import com.batch.android.localcampaigns.ViewTrackerUnavailableException;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.query.response.LocalCampaignsResponse;
import com.batch.android.user.SQLUserDatasource;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for MetricPostDataProvider
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocalCampaignsJITPostDataProviderTest extends DITest {

    private Context appContext;

    @Override
    public void setUp() {
        super.setUp();
        appContext = ApplicationProvider.getApplicationContext();
        simulateBatchStart(appContext);
    }

    @Test
    public void testGetRawDataMEP() throws JSONException, ViewTrackerUnavailableException {
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);
        LocalCampaignsSQLTracker tracker = (LocalCampaignsSQLTracker) CampaignManagerProvider.get().getViewTracker();
        tracker.open(appContext);

        List<LocalCampaign> campaigns = new ArrayList<>();
        LocalCampaign campaign1 = new LocalCampaign();
        campaign1.id = "c1";
        LocalCampaign campaign2 = new LocalCampaign();
        campaign2.id = "c2";
        LocalCampaign campaign3 = new LocalCampaign();
        campaign3.id = "c3";
        campaigns.add(campaign1);
        campaigns.add(campaign2);
        campaigns.add(campaign3);

        tracker.trackViewEvent("c1", null);
        tracker.trackViewEvent("c2", null);
        tracker.trackViewEvent("c3", null);

        tracker.trackViewEvent("c1", "test-user");

        UserModuleProvider.get().setCustomID(appContext, "test-user");

        JSONObject expectedData = new JSONObject(
            "{\"campaigns\":[\"c1\",\"c2\",\"c3\"],\"ids\":{},\"views\":{\"c3\":{\"count\":1},\"c1\":{\"count\":2},\"c2\":{\"count\":1}}}"
        );
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(
            campaigns,
            LocalCampaignsResponse.Version.MEP
        );
        assertEquals("application/json", provider.getContentType());
        assertTrue(JSONUtils.jsonObjectContainsValuesFrom(expectedData, provider.getRawData()));

        tracker.close();
    }

    @Test
    public void testDeserializeResponseMEP() throws JSONException {
        JSONObject fakeResponse = new JSONObject("{\"eligibleCampaigns\":[\"c2\",\"c3\"]}");
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(
            new ArrayList<>(),
            LocalCampaignsResponse.Version.MEP
        );
        List<String> eligibleCampaigns = provider.deserializeResponse(fakeResponse);
        Assert.assertEquals("c2", eligibleCampaigns.get(0));
        Assert.assertEquals("c3", eligibleCampaigns.get(1));
    }

    @Test
    public void testGetRawDataCEP() throws JSONException, ViewTrackerUnavailableException {
        appContext.deleteDatabase(LocalCampaignTrackDbHelper.DATABASE_NAME);
        LocalCampaignsSQLTracker tracker = (LocalCampaignsSQLTracker) CampaignManagerProvider.get().getViewTracker();
        tracker.open(appContext);

        List<LocalCampaign> campaigns = new ArrayList<>();
        LocalCampaign campaign1 = new LocalCampaign();
        campaign1.id = "c1";
        LocalCampaign campaign2 = new LocalCampaign();
        campaign2.id = "c2";
        LocalCampaign campaign3 = new LocalCampaign();
        campaign3.id = "c3";
        campaigns.add(campaign1);
        campaigns.add(campaign2);
        campaigns.add(campaign3);

        tracker.trackViewEvent("c1", null);
        tracker.trackViewEvent("c2", null);
        tracker.trackViewEvent("c3", null);

        tracker.trackViewEvent("c1", "test-user");

        UserModuleProvider.get().setCustomID(appContext, "test-user");

        JSONObject expectedData = new JSONObject(
            "{\"campaigns\":[\"c1\",\"c2\",\"c3\"],\"ids\":{},\"views\":{\"c3\":{\"count\":0},\"c1\":{\"count\":1},\"c2\":{\"count\":0}}}"
        );
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(
            campaigns,
            LocalCampaignsResponse.Version.CEP
        );
        assertEquals("application/json", provider.getContentType());
        assertTrue(JSONUtils.jsonObjectContainsValuesFrom(expectedData, provider.getRawData()));

        tracker.close();
    }

    @Test
    public void testDeserializeResponseCEP() throws JSONException {
        JSONObject fakeResponse = new JSONObject("{\"eligibleCampaigns\":[\"c2\",\"c3\"]}");
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(
            new ArrayList<>(),
            LocalCampaignsResponse.Version.CEP
        );
        List<String> eligibleCampaigns = provider.deserializeResponse(fakeResponse);
        Assert.assertEquals("c2", eligibleCampaigns.get(0));
        Assert.assertEquals("c3", eligibleCampaigns.get(1));
    }
}
