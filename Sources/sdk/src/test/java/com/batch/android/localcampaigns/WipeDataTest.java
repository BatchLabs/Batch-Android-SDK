package com.batch.android.localcampaigns;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DI;
import com.batch.android.di.DITestUtils;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.module.LocalCampaignsModule;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WipeDataTest {

    @Before
    public void setup() {
        DI.reset();
    }

    @After
    public void teardown() {
        DI.reset();
    }

    @Test
    public void testWipeData() throws JSONException {
        final Context c = ApplicationProvider.getApplicationContext();

        final JSONObject j = new JSONObject();
        j.put("foo", "bar");

        final CampaignManager campaignManager = DITestUtils.mockSingletonDependency(CampaignManager.class, null);
        //campaignManager.saveCampaignsResponse(c, j);

        final LocalCampaign fakeCampaign = new LocalCampaign();
        final List<LocalCampaign> fakeCampaignList = new ArrayList<>();
        fakeCampaignList.add(fakeCampaign);
        // Stub out the method that checks the validity, as it will remove our fake campaign
        Mockito.when(campaignManager.cleanCampaignList(Mockito.anyList())).thenReturn(fakeCampaignList);
        campaignManager.updateCampaignList(fakeCampaignList);

        LocalCampaignsModuleProvider.get().wipeData(c);

        Assert.assertFalse(campaignManager.hasSavedCampaigns(c));
        Assert.assertFalse(campaignManager.areCampaignsLoaded());
        Assert.assertEquals(0, campaignManager.getCampaignList().size());
    }

    @Test
    public void testOptOutCausesWipe() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();
        LocalCampaignsModule lcInstance = DITestUtils.mockSingletonDependency(LocalCampaignsModule.class, null);

        OptOutModuleProvider.get().wipeData(context);
        Mockito.verify(lcInstance).wipeData(context);
    }
}
