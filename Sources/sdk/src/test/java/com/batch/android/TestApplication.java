package com.batch.android;

import android.app.Application;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Batch.start("FAKE_API_KEY");
    }
}
