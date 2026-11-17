package com.batch.android.di;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.batch.android.Batch;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.module.UserModule;
import com.batch.android.runtime.State;
import org.junit.After;
import org.junit.Before;

/**
 * Superclass for every test that use the dependency graph
 */
public class DITest {

    @Before
    public void setUp() {
        // Load Robolectric's native SQLite on the test thread. Otherwise the first load can happen
        // on the UserModule apply queue, which tearDown() interrupts: Robolectric never retries an
        // interrupted load, so every SQLite test after it fails with an UnsatisfiedLinkError.
        SQLiteDatabase.create(null).close();
        DI.reset();
    }

    @After
    public void tearDown() {
        UserModule.resetApplyQueueForTesting();
        DI.reset();
    }

    protected void simulateBatchStart(Context context) {
        Batch.start("FAKE_API_KEY");
        RuntimeManagerProvider.get().changeState((state, config) -> State.READY);
        RuntimeManagerProvider.get().setContext(context);
    }
}
