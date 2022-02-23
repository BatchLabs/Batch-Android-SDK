package com.batch.android;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipboardManager;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.debug.FindMyInstallationHelper;
import com.batch.android.di.DITest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FindMyInstallationHelperTest extends DITest {

    private Context context;

    private FindMyInstallationHelper helper;

    @Before
    public void setUp() {
        super.setUp();
        context = ApplicationProvider.getApplicationContext();
        simulateBatchStart(context);
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testInstallationIdInClipboard() throws NoSuchFieldException, IllegalAccessException {
        Install install = new Install(context);
        Whitebox.setInternalState(Batch.class, "install", install);
        helper = new FindMyInstallationHelper();
        Field fieldTimestamps = FindMyInstallationHelper.class.getDeclaredField("timestamps");
        fieldTimestamps.setAccessible(true);
        List<Long> timestamps = (ArrayList<Long>) fieldTimestamps.get(helper);
        long now = new Date().getTime();
        assert timestamps != null;
        timestamps.add(now - 21_000);
        helper.notifyForeground();
        helper.notifyForeground();
        helper.notifyForeground();
        helper.notifyForeground();
        Assert.assertEquals(0, timestamps.size());
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        Assert.assertEquals("Batch Installation ID", clipboard.getPrimaryClip().getDescription().getLabel());
        Assert.assertEquals(
            "Batch Installation ID: ".concat(install.getInstallID()),
            clipboard.getPrimaryClip().getItemAt(0).getText()
        );
    }

    @Test
    public void testInstallationIdNotInClipboard() throws NoSuchFieldException, IllegalAccessException {
        helper = new FindMyInstallationHelper();
        Field fieldTimestamps = FindMyInstallationHelper.class.getDeclaredField("timestamps");
        fieldTimestamps.setAccessible(true);
        List<Long> timestamps = (ArrayList<Long>) fieldTimestamps.get(helper);
        long now = new Date().getTime();
        assert timestamps != null;
        timestamps.add(now - 21_000);
        helper.notifyForeground();
        helper.notifyForeground();
        helper.notifyForeground();
        Assert.assertEquals(3, timestamps.size());
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        Assert.assertNull(clipboard.getPrimaryClip());
    }

    @Test
    public void testSetFindMyInstallationEnabled() throws NoSuchFieldException, IllegalAccessException {
        helper = new FindMyInstallationHelper();
        Field fieldTimestamps = FindMyInstallationHelper.class.getDeclaredField("timestamps");
        fieldTimestamps.setAccessible(true);
        List<Long> timestamps = (ArrayList<Long>) fieldTimestamps.get(helper);
        assert timestamps != null;

        Assert.assertEquals(0, timestamps.size());

        helper.notifyForeground();
        //Checking default is true
        Assert.assertEquals(1, timestamps.size());

        Batch.setFindMyInstallationEnabled(false);

        helper.notifyForeground();
        Assert.assertEquals(1, timestamps.size());

        Batch.setFindMyInstallationEnabled(true);

        helper.notifyForeground();
        Assert.assertEquals(2, timestamps.size());
    }
}
