package com.batch.android.di;

import android.content.Context;
import com.batch.android.Batch;
import com.batch.android.Config;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.runtime.State;
import org.junit.After;
import org.junit.Before;

/**
 * Superclass for every test that use the dependency graph
 */
public class DITest {

    @Before
    public void setUp() {
        DI.reset();
    }

    @After
    public void tearDown() {
        DI.reset();
    }

    protected void simulateBatchStart(Context context) {
        Batch.setConfig(new Config("FAKE_API_KEY"));

        RuntimeManagerProvider.get().changeState(state -> State.READY);
        RuntimeManagerProvider.get().setContext(context);
    }
}
