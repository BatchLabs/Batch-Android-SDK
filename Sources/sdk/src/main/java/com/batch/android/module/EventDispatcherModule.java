package com.batch.android.module;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchEventDispatcher;
import com.batch.android.core.DiscoveryServiceHelper;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.eventdispatcher.DispatcherDiscoveryService;
import com.batch.android.eventdispatcher.DispatcherRegistrar;
import com.batch.android.eventdispatcher.DispatcherSerializer;
import com.batch.android.json.JSONObject;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Module
@Singleton
public class EventDispatcherModule extends BatchModule {

    private static final String TAG = "EventDispatcher";
    private static final String COMPONENT_SENTINEL_VALUE = "com.batch.android.eventdispatcher.DispatcherRegistrar";
    private static final String COMPONENT_KEY_PREFIX = "com.batch.android.eventdispatcher:";

    private final Set<BatchEventDispatcher> eventDispatchers = new LinkedHashSet<>();
    private OptOutModule optOutModule;

    private boolean isContextLoaded = false;

    private EventDispatcherModule(OptOutModule optOutModule) {
        this.optOutModule = optOutModule;
    }

    @Provide
    public static EventDispatcherModule provide() {
        return new EventDispatcherModule(OptOutModuleProvider.get());
    }

    @Override
    public String getId() {
        return "eventdispatcher";
    }

    @Override
    public int getState() {
        return 1;
    }

    private void printLoadedDispatcher(@NonNull String name) {
        Logger.internal(TAG, "Adding event dispatcher: " + name);
    }

    private void printDeprecatedDispatcher(@NonNull String name) {
        Logger.warning(TAG, "The version of your event dispatcher: " + name + " is outdated, please update it.");
    }

    public void addEventDispatcher(BatchEventDispatcher dispatcher) {
        synchronized (eventDispatchers) {
            eventDispatchers.add(dispatcher);
        }
    }

    public boolean removeEventDispatcher(BatchEventDispatcher dispatcher) {
        synchronized (eventDispatchers) {
            return eventDispatchers.remove(dispatcher);
        }
    }

    public void dispatchEvent(Batch.EventDispatcher.Type type, Batch.EventDispatcher.Payload params) {
        if (Boolean.TRUE.equals(optOutModule.isOptedOut())) {
            Logger.internal(TAG, "Batch is opted out, refusing to dispatch event.");
            return;
        }

        synchronized (eventDispatchers) {
            for (BatchEventDispatcher dispatcher : eventDispatchers) {
                dispatcher.dispatchEvent(type, params);
            }
        }
    }

    public void loadDispatcherFromContext(Context context) {
        if (Boolean.TRUE.equals(optOutModule.isOptedOutSync(context))) {
            Logger.internal(TAG, "Batch is opted out, refusing to add event dispatchers.");
            return;
        }

        synchronized (eventDispatchers) {
            // Ensure once because of NotificationPresenter
            if (isContextLoaded) {
                return;
            }
            isContextLoaded = true;
        }

        List<String> registrarNames = DiscoveryServiceHelper.getComponentNames(
            context,
            DispatcherDiscoveryService.class,
            COMPONENT_SENTINEL_VALUE,
            COMPONENT_KEY_PREFIX
        );
        for (String name : registrarNames) {
            try {
                Class<?> loadedClass = Class.forName(name);
                if (!DispatcherRegistrar.class.isAssignableFrom(loadedClass)) {
                    Logger.error(
                        TAG,
                        String.format("Class %s is not an instance of %s", name, COMPONENT_SENTINEL_VALUE)
                    );
                    continue;
                }

                DispatcherRegistrar registrar = (DispatcherRegistrar) loadedClass
                    .getDeclaredConstructor()
                    .newInstance();
                BatchEventDispatcher dispatcher = registrar.getDispatcher(context);
                if (dispatcher != null) {
                    addEventDispatcher(dispatcher);
                    String dispatcherName = dispatcher.getClass().getName();
                    try {
                        if (dispatcher.getName() != null) {
                            dispatcherName = dispatcher.getName();
                        } else {
                            printDeprecatedDispatcher(dispatcherName);
                        }
                        printLoadedDispatcher(dispatcherName);
                    } catch (AbstractMethodError e) {
                        printDeprecatedDispatcher(dispatcherName);
                    }
                }
            } catch (Throwable e) {
                Logger.error(String.format("Could not instantiate %s", name), e);
            }
        }
    }

    /**
     * Get dispatchers as json object used for the analytics
     *
     * @return A JSONObject of dispatcher Name:Version
     */
    @Nullable
    public JSONObject getDispatchersAnalyticRepresentation() {
        synchronized (eventDispatchers) {
            JSONObject json = DispatcherSerializer.serialize(eventDispatchers);
            return json.length() > 0 ? json : null;
        }
    }
}
