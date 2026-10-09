package com.batch.android.push;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.batch.android.PushRegistrationProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PushRegistrationProviderFactoryTest {

    private static final String FID_FLAG = "firebase_messaging_installation_id_enabled";

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testTokenProviderWhenFlagIsMissing() {
        setManifestMetaData(null);
        PushRegistrationProvider provider = new PushRegistrationProviderFactory(context).getRegistrationProvider();
        Assert.assertTrue(provider instanceof FCMTokenRegistrationProvider);
        Assert.assertEquals("FCM-Token", provider.getShortname());
    }

    @Test
    public void testTokenProviderWhenFlagIsFalse() {
        Bundle metaData = new Bundle();
        metaData.putBoolean(FID_FLAG, false);
        setManifestMetaData(metaData);
        PushRegistrationProvider provider = new PushRegistrationProviderFactory(context).getRegistrationProvider();
        Assert.assertTrue(provider instanceof FCMTokenRegistrationProvider);
    }

    @Test
    public void testFidProviderWhenFlagIsTrue() {
        Bundle metaData = new Bundle();
        metaData.putBoolean(FID_FLAG, true);
        setManifestMetaData(metaData);
        PushRegistrationProvider provider = new PushRegistrationProviderFactory(context).getRegistrationProvider();
        Assert.assertTrue(provider instanceof FCMFidRegistrationProvider);
        Assert.assertEquals("FCM-FID", provider.getShortname());
    }

    private void setManifestMetaData(Bundle metaData) {
        ApplicationInfo appInfo = shadowOf(context.getPackageManager())
            .getInternalMutablePackageInfo(context.getPackageName())
            .applicationInfo;
        appInfo.metaData = metaData;
    }
}
