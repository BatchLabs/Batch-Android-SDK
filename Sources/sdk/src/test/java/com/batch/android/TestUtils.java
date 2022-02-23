package com.batch.android;

import android.os.Build;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Assume;

public class TestUtils {

    public static boolean isAppRunningInEmulator() {
        return (
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk".equals(Build.PRODUCT)
        );
    }

    public static void assertAppRunningInRealDevice() {
        Assert.assertFalse("This test is supposed to run in a real device.", isAppRunningInEmulator());
    }

    public static void assumeAppRunningInRealDevice() {
        Assume.assumeFalse("This test is supposed to run in a real device.", isAppRunningInEmulator());
    }

    public static InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
}
