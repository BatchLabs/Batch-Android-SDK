package com.batch.android.localcampaigns;

import static org.mockito.ArgumentMatchers.argThat;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import com.batch.android.Batch;
import com.batch.android.BatchMessagingException;
import com.batch.android.TestActivity;
import com.batch.android.date.UTCDate;
import com.batch.android.di.DITest;
import com.batch.android.di.DITestUtils;
import com.batch.android.di.providers.CampaignManagerProvider;
import com.batch.android.di.providers.LandingOutputProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.localcampaigns.model.LocalCampaign;
import com.batch.android.localcampaigns.signal.EventTrackedSignal;
import com.batch.android.localcampaigns.trigger.EventLocalCampaignTrigger;
import com.batch.android.module.LocalCampaignsModule;
import java.util.Collections;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EventTriggerTest extends DITest {

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class, false, true);

    @Before
    public void setUp() {
        super.setUp();

        LocalCampaignsModule module = DITestUtils.mockSingletonDependency(LocalCampaignsModule.class, null);
        simulateBatchStart(activityRule.getActivity());
        module.batchContextBecameAvailable(ApplicationProvider.getApplicationContext());
    }

    @After
    public void tearDown() {
        super.tearDown();
        Mockito.validateMockitoUsage();
    }

    @Test
    public void testSignalSentAfterEventTracked()
        throws BatchMessagingException, InterruptedException, NoSuchFieldException, IllegalAccessException, JSONException {
        final String EVENT_NAME_TEST = "TEST_EVENT";

        LocalCampaign campaign = new LocalCampaign();
        campaign.id = UUID.randomUUID().toString();
        campaign.eventData = new JSONObject();
        campaign.output = LandingOutputProvider.get(new JSONObject());
        campaign.output.payload.put("testRandomMessage", UUID.randomUUID().toString());
        campaign.startDate = new UTCDate(0);
        campaign.triggers.add(new EventLocalCampaignTrigger("E." + EVENT_NAME_TEST, null));

        CampaignManagerProvider.get().updateCampaignList(Collections.singletonList(campaign));

        // Track the event which is linked to the Local Campaign
        Batch.User.trackEvent(EVENT_NAME_TEST);

        Mockito
            .verify(LocalCampaignsModuleProvider.get())
            .sendSignal(eventTrackedSignalEq(new EventTrackedSignal("E." + EVENT_NAME_TEST, null)));
    }

    @Test
    public void testCampaignDisplayedAfterEventTracked()
        throws BatchMessagingException, InterruptedException, NoSuchFieldException, IllegalAccessException, JSONException {
        final String EVENT_NAME_TEST = "TEST_EVENT";

        LocalCampaign campaign = Mockito.spy(new LocalCampaign());
        campaign.id = UUID.randomUUID().toString();
        campaign.eventData = new JSONObject();
        campaign.output = LandingOutputProvider.get(new JSONObject());
        campaign.output.payload.put("testRandomMessage", UUID.randomUUID().toString());
        campaign.startDate = new UTCDate(0);
        campaign.triggers.add(new EventLocalCampaignTrigger("E." + EVENT_NAME_TEST, null));

        CampaignManagerProvider.get().updateCampaignList(Collections.singletonList(campaign));

        // Simulate synchro is finished
        LocalCampaignsModuleProvider.get().onLocalCampaignsWebserviceFinished();

        // Track the event which is linked to the Local Campaign
        Batch.User.trackEvent(EVENT_NAME_TEST);

        Thread.sleep(2000);

        Mockito.verify(campaign).displayMessage();
    }

    static class EventTrackedSignalMatcher implements ArgumentMatcher<EventTrackedSignal> {

        private final EventTrackedSignal expected;

        public EventTrackedSignalMatcher(EventTrackedSignal expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(EventTrackedSignal argument) {
            return (
                expected == argument ||
                expected != null &&
                argument != null &&
                expected.name.equals(argument.name) &&
                (expected.parameters == null || expected.parameters.equals(argument.parameters))
            );
        }
    }

    static EventTrackedSignal eventTrackedSignalEq(EventTrackedSignal expected) {
        return argThat(new EventTrackedSignalMatcher(expected));
    }
}
