package com.batch.android;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.module.MessagingModule;
import com.batch.android.runtime.SessionManager;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test life cycle of Batch and entries check
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LifeCycleTest extends DITest {

    private Activity activity;

    @Rule
    public ActivityTestRule<TestActivity> activityRule = new ActivityTestRule<>(TestActivity.class, false, true);

    @Before
    public void setUp() {
        super.setUp();
        activity = activityRule.getActivity();
    }

    @Override
    public void tearDown() {
        super.tearDown();
    }

    /**
     * Test that Batch start with all parameters set
     */
    @Test
    public void testCompletestart() {
        Batch.onStart(activity);
    }

    /**
     * Test batch start & stop
     *
     * @throws Exception
     */
    @Test
    public void testBatchStartStop() throws Exception {
        int total = 100;
        int i = 0;
        while (i < total) {
            Batch.onStart(activity);

            Thread.sleep(50);

            Batch.onStop(activity);

            Thread.sleep(25);

            Batch.onDestroy(activity);

            Thread.sleep(25);

            i++;
        }

        Batch.onStart(activity);
    }

    /**
     * Test sessionID
     *
     * @throws Exception
     */
    @Test
    public void testSessionID() throws Exception {
        Field createCountField = SessionManager.class.getDeclaredField("createCount");
        createCountField.setAccessible(true);

        RuntimeManagerProvider.get().setActivity(activityRule.getActivity());
        RuntimeManagerProvider.get().registerSessionManagerIfNeeded(ApplicationProvider.getApplicationContext(), true);

        String sessionID = RuntimeManagerProvider.get().getSessionIdentifier();
        assertNotNull(sessionID);

        SessionManager sessionManager = RuntimeManagerProvider.get().getSessionManager();
        AtomicInteger createCount = (AtomicInteger) createCountField.get(sessionManager);

        LocalBroadcastManager
            .getInstance(activity)
            .sendBroadcast(new Intent(MessagingModule.ACTION_DISMISS_INTERSTITIAL));
        Thread.sleep(2000);

        Assert.assertEquals(1, createCount.get());

        // Simulate activity destroy in sessionManager
        sessionManager.onActivityPaused(activity);
        sessionManager.onActivityDestroyed(activity);

        Assert.assertEquals(0, createCount.get());

        // Simulate activity start in sessionManager
        sessionManager.onActivityCreated(activity, new Bundle());
        sessionManager.onActivityResumed(activity);

        Assert.assertEquals(1, createCount.get());

        String newSession = RuntimeManagerProvider.get().getSessionIdentifier();
        assertNotNull(newSession);
        assertNotEquals(newSession, sessionID);

        RuntimeManagerProvider.get().setActivity(null);
    }
}
