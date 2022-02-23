package com.batch.android;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.di.DITest;
import com.batch.android.di.providers.PushModuleProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test statuses of Modules
 *
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ModuleStatusTest extends DITest {

    @Test
    public void testPushModuleStatus() throws Exception {
        assertEquals(2, PushModuleProvider.get().getState());

        Batch.Push.setGCMSenderId("test");

        assertEquals(1, PushModuleProvider.get().getState());

        Batch.Push.setGCMSenderId(null);

        assertEquals(2, PushModuleProvider.get().getState());
    }
}
