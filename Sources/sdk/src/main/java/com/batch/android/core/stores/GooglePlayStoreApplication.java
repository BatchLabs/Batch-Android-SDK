package com.batch.android.core.stores;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.batch.android.core.Logger;

public class GooglePlayStoreApplication implements StoreApplication {

    @Override
    public void open(@NonNull Context context) {
        try {
            final Intent i = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + context.getPackageName())
            );
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Logger.error("Core", "Could not open Play Store");
        }
    }
}
