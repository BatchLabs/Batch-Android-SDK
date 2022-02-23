package com.batch.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import com.batch.android.core.ExcludedActivityHelper;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.runtime.RuntimeManager;
import com.batch.android.runtime.SessionManager;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the ExcludedActivityHelper class
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ExcludedActivityHelperTest {

    // Test activities
    private Activity mainActivity;
    private Activity excludedActivity;

    // Helper to test
    private ExcludedActivityHelper helper;

    Field createCountField;

    @Rule
    public ActivityTestRule<TestExcludedActivity> excludedActivityRule = new ActivityTestRule<>(
        TestExcludedActivity.class,
        false,
        true
    );

    @Rule
    public ActivityTestRule<TestActivity> mainActivityRule = new ActivityTestRule<>(TestActivity.class, false, true);

    @Before
    public void setUp() throws NoSuchFieldException {
        helper = new ExcludedActivityHelper();

        // Setup test activities
        mainActivity = mainActivityRule.getActivity();
        excludedActivity = excludedActivityRule.getActivity();

        // Adding batch mocked payload to the activity
        final Bundle payload = new Bundle();
        payload.putString("com.batch", "{l:'batch payload'}");
        final Bundle extras = new Bundle();
        extras.putBundle(Batch.Push.PAYLOAD_KEY, payload);
        excludedActivity.getIntent().putExtras(extras);

        createCountField = SessionManager.class.getDeclaredField("createCount");
        createCountField.setAccessible(true);
    }

    /**
     * Test the helper for an activity which should be excluded from batch
     */
    @Test
    public void testActivityIsExcluded() {
        // Testing the activity ignore batch
        boolean shouldIgnore = ExcludedActivityHelper.activityIsExcludedFromManifest(excludedActivity);
        Assert.assertTrue(shouldIgnore);

        // Testing the helper has saved the intent with its push payload
        helper.saveIntentIfNeeded(excludedActivity);
        Assert.assertTrue(helper.hasIntent());

        // Popping the intent
        Intent intent = helper.popIntent();

        //Testing the intent is not null and has been removed
        Assert.assertNotNull(intent);
        Assert.assertFalse(helper.hasIntent());
    }

    /**
     * Test the helper for an activity which should not be excluded from batch
     */
    @Test
    public void testActivityIsNotExcluded() {
        // Testing the activity ignore batch
        boolean shouldIgnore = ExcludedActivityHelper.activityIsExcludedFromManifest(mainActivity);
        Assert.assertFalse(shouldIgnore);

        // Testing the helper has saved the intent with its push payload
        Assert.assertFalse(helper.hasIntent());

        // Popping the intent
        Intent intent = helper.popIntent();

        //Testing the intent is null
        Assert.assertNull(intent);
        Assert.assertFalse(helper.hasIntent());
    }

    @Test
    public void testSessionManager() throws IllegalAccessException {
        RuntimeManager runtimeManager = RuntimeManagerProvider.get();
        runtimeManager.setActivity(excludedActivity);
        runtimeManager.registerSessionManagerIfNeeded(ApplicationProvider.getApplicationContext(), true);
        SessionManager sessionManager = runtimeManager.getSessionManager();
        AtomicInteger createCount = (AtomicInteger) createCountField.get(sessionManager);

        sessionManager.onActivityCreated(excludedActivity, new Bundle());
        Assert.assertEquals(0, createCount.get());

        sessionManager.onActivityCreated(mainActivity, new Bundle());
        Assert.assertEquals(1, createCount.get());

        sessionManager.onActivityDestroyed(excludedActivity);
        Assert.assertEquals(1, createCount.get());

        sessionManager.onActivityDestroyed(mainActivity);
        Assert.assertEquals(0, createCount.get());
    }
}
