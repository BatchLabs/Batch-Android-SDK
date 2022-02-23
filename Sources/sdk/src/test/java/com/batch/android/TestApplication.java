package com.batch.android;

import android.app.Application;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Batch.setConfig(new com.batch.android.Config("FAKE_API_KEY"));
    }
}
