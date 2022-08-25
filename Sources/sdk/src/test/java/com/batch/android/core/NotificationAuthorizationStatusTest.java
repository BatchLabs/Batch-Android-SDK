package com.batch.android.core;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DI;
import com.batch.android.di.providers.ParametersProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NotificationAuthorizationStatusTest {

    @Before
    public void setUp() {
        DI.reset();
    }

    @After
    public void tearDown() {
        DI.reset();
    }

    @Test
    public void testShouldTrackNotificationStatusChangeEvent() {
        Context context = ApplicationProvider.getApplicationContext();

        // First time is always tracked
        Assert.assertTrue(NotificationAuthorizationStatus.shouldTrackNotificationStatusChangeEvent(context, false));

        // Persist authorization status
        ParametersProvider
            .get(context)
            .set(ParameterKeys.PUSH_NOTIF_LAST_AUTH_STATUS_SENT, String.valueOf(false), true);

        // Should not be tracked since authorization did not change
        Assert.assertFalse(NotificationAuthorizationStatus.shouldTrackNotificationStatusChangeEvent(context, false));

        // Should be tracked since authorization did change
        Assert.assertTrue(NotificationAuthorizationStatus.shouldTrackNotificationStatusChangeEvent(context, true));
    }
}
