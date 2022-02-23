package com.batch.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import androidx.annotation.Nullable;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Batch.setConfig(new Config("TEST_API_KEY"));
        Batch.onCreate(this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        Batch.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Batch.onStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Batch.onDestroy(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Batch.onNewIntent(this, intent);
    }
}
