package com.batch.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DITest;
import com.batch.android.di.DITestUtils;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.TrackerModule;
import com.batch.android.user.EmailSubscription;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class EmailSubscriptionTest extends DITest {

    private EmailSubscription emailSubscription;

    private TrackerModule trackerModule;

    @Before
    public void setUp() {
        super.setUp();
        simulateBatchStart(ApplicationProvider.getApplicationContext());
        Batch.User.editor().setIdentifier("id_test").saveSync();
        this.emailSubscription = new EmailSubscription();
        this.trackerModule = DITestUtils.mockSingletonDependency(TrackerModule.class, null);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testSetEmail() {
        assertNull(Whitebox.getInternalState(emailSubscription, "email"));
        emailSubscription.setEmail("test@batch.com");
        assertEquals("test@batch.com", Whitebox.getInternalState(emailSubscription, "email"));
        emailSubscription.setEmail(null);
        assertNull(Whitebox.getInternalState(emailSubscription, "email"));
        assertTrue(Whitebox.getInternalState(emailSubscription, "deleteEmail"));
    }

    @Test
    public void testAddSubscription() {
        emailSubscription.addSubscription(EmailSubscription.Kind.MARKETING, BatchEmailSubscriptionState.SUBSCRIBED);
        HashMap<EmailSubscription.Kind, BatchEmailSubscriptionState> currentSubscription = Whitebox.getInternalState(
            emailSubscription,
            "subscriptions"
        );
        assertEquals(1, currentSubscription.size());
        assertEquals(EmailSubscription.Kind.MARKETING, currentSubscription.keySet().toArray()[0]);
        assertEquals(BatchEmailSubscriptionState.SUBSCRIBED, currentSubscription.values().toArray()[0]);
    }

    @Test
    public void testSendEmailOnly() throws JSONException {
        JSONObject expected = new JSONObject();
        expected.put("custom_id", "id_test");
        expected.put("email", "test@batch.com");

        emailSubscription.setEmail("test@batch.com");
        emailSubscription.sendEmailSubscriptionEvent();

        Mockito.verify(trackerModule).track(eq(InternalEvents.EMAIL_CHANGED), JSONObjectMockitoMatcher.eq(expected));
    }

    @Test
    public void testSendEmailNullOnly() throws JSONException {
        JSONObject expected = new JSONObject();
        expected.put("custom_id", "id_test");
        expected.put("email", JSONObject.NULL);

        emailSubscription.setEmail(null);
        emailSubscription.sendEmailSubscriptionEvent();

        Mockito.verify(trackerModule).track(eq(InternalEvents.EMAIL_CHANGED), JSONObjectMockitoMatcher.eq(expected));
    }

    @Test
    public void testSendEmailSubscriptionOnly() throws JSONException {
        JSONObject expected = new JSONObject();
        expected.put("custom_id", "id_test");
        JSONObject expectedSub = new JSONObject();
        expectedSub.put("marketing", "subscribed");
        expected.put("subscriptions", expectedSub);

        emailSubscription.addSubscription(EmailSubscription.Kind.MARKETING, BatchEmailSubscriptionState.SUBSCRIBED);
        emailSubscription.sendEmailSubscriptionEvent();

        Mockito.verify(trackerModule).track(eq(InternalEvents.EMAIL_CHANGED), JSONObjectMockitoMatcher.eq(expected));
    }

    @Test
    public void testSendEmailSubscriptionFull() throws JSONException {
        JSONObject expected = new JSONObject();
        expected.put("custom_id", "id_test");
        expected.put("email", "test@batch.com");
        JSONObject expectedSub = new JSONObject();
        expectedSub.put("marketing", "subscribed");
        expected.put("subscriptions", expectedSub);

        emailSubscription.setEmail("test@batch.com");
        emailSubscription.addSubscription(EmailSubscription.Kind.MARKETING, BatchEmailSubscriptionState.SUBSCRIBED);
        emailSubscription.sendEmailSubscriptionEvent();

        Mockito.verify(trackerModule).track(eq(InternalEvents.EMAIL_CHANGED), JSONObjectMockitoMatcher.eq(expected));
    }
}
