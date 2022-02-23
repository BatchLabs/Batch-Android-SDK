package com.batch.android.core.stores;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.PackageUtils;

public class StoreApplicationFactory {

    private StoreApplicationFactory() {}

    // Get the main app store. Google Play is the preferred one,
    // then comes huawei AppGallery
    @Nullable
    public static StoreApplication getMainStore(@NonNull Context context) {
        if (isPlayStoreInstalled(context)) {
            return new GooglePlayStoreApplication();
        }
        if (isHuaweiAppGalleryInstalled(context)) {
            return new HuaweiAppGalleryApplication();
        }
        return null;
    }

    private static boolean isPlayStoreInstalled(@NonNull Context context) {
        return PackageUtils.isPackageInstalled(context.getPackageManager(), "com.android.vending");
    }

    private static boolean isHuaweiAppGalleryInstalled(@NonNull Context context) {
        return PackageUtils.isPackageInstalled(context.getPackageManager(), "com.huawei.appmarket");
    }
}
