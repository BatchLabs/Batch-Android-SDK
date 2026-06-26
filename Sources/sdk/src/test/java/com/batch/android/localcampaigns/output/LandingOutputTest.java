package com.batch.android.localcampaigns.output;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DITest;
import com.batch.android.di.DITestUtils;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.CampaignManager;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.module.MessagingModule;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LandingOutputTest extends DITest {

    private LocalCampaign newCampaign() throws JSONException {
        LocalCampaign campaign = new LocalCampaign();
        campaign.id = UUID.randomUUID().toString();
        campaign.eventData = new JSONObject();
        return campaign;
    }

    @Test
    public void testPendingDisplayClearedWhenInAppNotDisplayed() throws JSONException {
        MessagingModule messagingModule = DITestUtils.mockSingletonDependency(MessagingModule.class, null);
        CampaignManager campaignManager = DITestUtils.mockSingletonDependency(CampaignManager.class, null);

        // Simulate the messaging module refusing to display the in-app (interceptor blocked it
        // or no presented activity is available).
        Mockito.doReturn(false).when(messagingModule).displayInAppMessage(Mockito.any());

        LocalCampaign campaign = newCampaign();
        LandingOutput output = LandingOutputProvider.get(new JSONObject());

        // The module marks the campaign as pending before dispatching the display.
        campaignManager.markCampaignAsPendingDisplay(campaign.id);

        boolean displayed = output.displayMessage(campaign);

        Assert.assertFalse(displayed);
        // Since nothing was shown, onViewShown (and thus trackCampaignView) will never fire, so the
        // pending flag must be cleared here to avoid suppressing the campaign for the rest of the process.
        Mockito.verify(campaignManager).unmarkCampaignAsPendingDisplay(campaign.id);
    }

    @Test
    public void testPendingDisplayKeptWhenInAppDisplayed() throws JSONException {
        MessagingModule messagingModule = DITestUtils.mockSingletonDependency(MessagingModule.class, null);
        CampaignManager campaignManager = DITestUtils.mockSingletonDependency(CampaignManager.class, null);

        Mockito.doReturn(true).when(messagingModule).displayInAppMessage(Mockito.any());

        LocalCampaign campaign = newCampaign();
        LandingOutput output = LandingOutputProvider.get(new JSONObject());

        campaignManager.markCampaignAsPendingDisplay(campaign.id);

        boolean displayed = output.displayMessage(campaign);

        Assert.assertTrue(displayed);
        // The display is in flight: the pending flag must stay set and only be cleared later by
        // TrackerModule#trackCampaignView once the view has actually been tracked.
        Mockito.verify(campaignManager, Mockito.never()).unmarkCampaignAsPendingDisplay(campaign.id);
    }
}
