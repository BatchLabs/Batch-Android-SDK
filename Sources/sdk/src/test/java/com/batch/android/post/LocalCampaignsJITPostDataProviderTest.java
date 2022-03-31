package com.batch.android.post;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.core.ByteArrayHelper;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.localcampaigns.LocalCampaignsTracker;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.metrics.model.Counter;
import com.batch.android.metrics.model.Metric;
import com.batch.android.metrics.model.Observation;
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
public class LocalCampaignsJITPostDataProviderTest {

    @Test
    public void testPack() {
        Context context = ApplicationProvider.getApplicationContext();

        LocalCampaignsTracker tracker = (LocalCampaignsTracker) CampaignManagerProvider.get().getViewTracker();
        tracker.open(context);

        SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);

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

        byte[] expectedData = ByteArrayHelper.hexToBytes(
            "84A963616D706169676E7393A26331A26332A26333A369647380AA6174747269627574657380A5766965777383A2633381A5636F756E7400A2633181A5636F756E7400A2633281A5636F756E7400"
        );

        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(campaigns);

        assertEquals("application/msgpack", provider.getContentType());
        assertArrayEquals(campaigns.toArray(), provider.getRawData().toArray());
        assertArrayEquals(expectedData, provider.getData());

        tracker.close();
        datasource.close();
    }

    @Test
    public void testUnpack() {
        String fakeHexResponse = "81b1656c696769626c6543616d706169676e7392a26332a26333";
        byte[] fakeResponse = ByteArrayHelper.hexToBytes(fakeHexResponse);
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(new ArrayList<>());
        List<String> eligibleCampaigns = provider.unpack(fakeResponse);
        Assert.assertEquals("c2", eligibleCampaigns.get(0));
        Assert.assertEquals("c3", eligibleCampaigns.get(1));
    }

    @Test
    public void testIsEmpty() {
        List<LocalCampaign> campaigns = new ArrayList<>();
        LocalCampaignsJITPostDataProvider provider = new LocalCampaignsJITPostDataProvider(campaigns);
        assertTrue(provider.isEmpty());

        LocalCampaign campaign = new LocalCampaign();
        campaigns.add(campaign);
        provider = new LocalCampaignsJITPostDataProvider(campaigns);
        assertFalse(provider.isEmpty());
    }
}
