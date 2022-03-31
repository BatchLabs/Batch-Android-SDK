package com.batch.android.module;

import android.content.Context;
import androidx.annotation.NonNull;
import com.batch.android.di.providers.ActionModuleProvider;
import com.batch.android.di.providers.DisplayReceiptModuleProvider;
import com.batch.android.di.providers.EventDispatcherModuleProvider;
import com.batch.android.di.providers.LocalCampaignsModuleProvider;
import com.batch.android.di.providers.MessagingModuleProvider;
import com.batch.android.di.providers.PushModuleProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Module master that dispatch to subcribed modules
 *
 */
@Module
@Singleton
public class BatchModuleMaster extends BatchModule {

    /**
     * Subscribed modules
     */
    private List<BatchModule> modules;

    private BatchModuleMaster(List<BatchModule> modules) {
        this.modules = modules;
    }

    @Provide
    public static BatchModuleMaster provide() {
        List<BatchModule> modules = new ArrayList<>(8);

        modules.add(ActionModuleProvider.get());
        modules.add(DisplayReceiptModuleProvider.get());
        modules.add(EventDispatcherModuleProvider.get());
        modules.add(LocalCampaignsModuleProvider.get());
        modules.add(MessagingModuleProvider.get());
        modules.add(PushModuleProvider.get());
        modules.add(TrackerModuleProvider.get());
        modules.add(UserModuleProvider.get());
        return new BatchModuleMaster(modules);
    }

    // ------------------------------------------->

    @Override
    public String getId() {
        return "master";
    }

    @Override
    public int getState() {
        return 1;
    }

    @Override
    public void batchContextBecameAvailable(@NonNull Context applicationContext) {
        for (BatchModule module : modules) {
            module.batchContextBecameAvailable(applicationContext);
        }
    }

    @Override
    public void batchWillStart() {
        for (BatchModule module : modules) {
            module.batchWillStart();
        }
    }

    @Override
    public void batchDidStart() {
        for (BatchModule module : modules) {
            module.batchDidStart();
        }
    }

    @Override
    public void batchIsFinishing() {
        for (BatchModule module : modules) {
            module.batchIsFinishing();
        }
    }

    @Override
    public void batchWillStop() {
        for (BatchModule module : modules) {
            module.batchWillStop();
        }
    }

    @Override
    public void batchDidStop() {
        for (BatchModule module : modules) {
            module.batchDidStop();
        }
    }
}
