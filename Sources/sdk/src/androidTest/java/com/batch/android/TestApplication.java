package com.batch.android;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        Batch.setConfig(new com.batch.android.Config("FAKE_API_KEY"));
    }
}
