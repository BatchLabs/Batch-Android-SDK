package com.batch.android.module;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchEventAttributes;
import com.batch.android.BatchMigration;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.SQLUserDatasourceProvider;
import com.batch.android.di.providers.TaskExecutorProvider;
import com.batch.android.di.providers.TrackerModuleProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.event.EventAttributesSerializer;
import com.batch.android.event.EventAttributesValidator;
import com.batch.android.event.InternalEvents;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.processor.Module;
import com.batch.android.processor.Provide;
import com.batch.android.processor.Singleton;
import com.batch.android.profile.ProfileDataHelper;
import com.batch.android.profile.ProfileDataSerializer;
import com.batch.android.profile.ProfileUpdateOperation;
import com.batch.android.user.AttributeType;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Batch Profile module
 *
 */
@Module
@Singleton
public final class ProfileModule extends BatchModule {

    public static final String TAG = "Profile";

    /**
     * Batch Tracker Module
     */
    @NonNull
    private final TrackerModule trackerModule;

    /**
     * ProfileModule constructor
     * @param trackerModule The Batch Tracker Module
     */
    private ProfileModule(@NonNull TrackerModule trackerModule) {
        this.trackerModule = trackerModule;
    }

    /**
     * DI access method
     * @return A new instance of the profile module
     */
    @Provide
    public static ProfileModule provide() {
        return new ProfileModule(TrackerModuleProvider.get());
    }

    // region BatchModule
    @Override
    public String getId() {
        return "profile";
    }

    @Override
    public int getState() {
        return 1;
    }

    // endregion

    /**
     * Internal implementation of the identify method
     * @param identifier The custom user identifier
     */
    public void identify(@Nullable String identifier) {
        if (ProfileDataHelper.isNotValidCustomUserID(identifier)) {
            Logger.error(TAG, "identify called with invalid identifier (must be less than 1024 chars)");
            return;
        }

        if (ProfileDataHelper.isBlocklistedCustomUserID(identifier)) {
            Logger.error(
                TAG,
                "identify called with a blocklisted identifier: `" +
                identifier +
                "`, Please ensure you have correctly implemented the API."
            );
            return;
        }

        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.error(TAG, "Batch does not have a context yet. Make sure Batch is started.");
            return;
        }

        // Saving the custom identifier locally
        UserModuleProvider.get().setCustomID(context, identifier);

        // Send an identify event to login/logout the profile with the installation
        this.sendIdentifyEvent(identifier);
    }

    /**
     * Handle profile data changed
     * @param data The profile data model to handle
     */
    public void handleProfileDataChanged(@NonNull ProfileUpdateOperation data) {
        try {
            JSONObject params = ProfileDataSerializer.serialize(data);
            if (params.length() == 0) {
                Logger.internal(TAG, "Trying to send an empty profile data changed event, aborting.");
                return;
            }
            trackerModule.track(InternalEvents.PROFILE_DATA_CHANGED, params);
        } catch (JSONException e) {
            Logger.error(TAG, "Sending profile data changed event failed.", e);
        }
    }

    /**
     * Method called when we the project key has changed.
     *
     * @param oldProjectKey The old project key bound to the App
     * @param newProjectKey The new project key bound to the App
     */
    public void onProjectChanged(@Nullable String oldProjectKey, @NonNull String newProjectKey) {
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.error(TAG, "Batch does not have a context yet. Aborting profile data migrations.");
            return;
        }
        // Migrate only the first time
        if (oldProjectKey == null) {
            // Migration related configurations
            RuntimeManagerProvider
                .get()
                .readConfig(config -> {
                    Integer migrations = config.getMigrations();
                    // Custom ID Migration
                    if (BatchMigration.isCustomIDMigrationDisabled(migrations)) {
                        Logger.internal(TAG, "Custom ID migration has been explicitly disabled.");
                    } else {
                        String customUserID = UserModuleProvider.get().getCustomID(context);
                        if (customUserID != null) {
                            Logger.internal(TAG, "Automatic custom id migration.");
                            sendIdentifyEvent(customUserID);
                        }
                    }

                    // Custom Data Migration
                    if (BatchMigration.isCustomDataMigrationDisabled(migrations)) {
                        Logger.internal(TAG, "Custom Data migration has been explicitly disabled.");
                    } else {
                        Logger.internal(TAG, "Automatic custom data migration.");
                        TaskExecutorProvider.get(context).submit(() -> this.migrateCustomData(context));
                    }
                });
        }
    }

    /**
     * Migrate the installation related data to the profile
     *
     * @param context Android's context
     */
    private void migrateCustomData(Context context) {
        ProfileUpdateOperation profileUpdateOperation = new ProfileUpdateOperation();

        // Get custom language and region
        profileUpdateOperation.setLanguage(UserModuleProvider.get().getLanguage(context));
        profileUpdateOperation.setRegion(UserModuleProvider.get().getRegion(context));

        // Get custom attributes
        final SQLUserDatasource datasource = SQLUserDatasourceProvider.get(context);
        Map<String, UserAttribute> customAttributes = datasource.getAttributes();
        for (Map.Entry<String, UserAttribute> entry : customAttributes.entrySet()) {
            profileUpdateOperation.addAttribute(entry.getKey().substring(2), entry.getValue()); // substring 2 to remove datasource "c." prefix
        }
        // Get custom tags
        Map<String, Set<String>> customTagCollections = datasource.getTagCollections();
        for (Map.Entry<String, Set<String>> entry : customTagCollections.entrySet()) {
            profileUpdateOperation.addAttribute(
                entry.getKey(),
                new UserAttribute(new ArrayList<>(entry.getValue()), AttributeType.STRING_ARRAY)
            );
        }
        // Send profile data changed
        this.handleProfileDataChanged(profileUpdateOperation);
    }

    /**
     * Send an internal event for profile identify
     *
     * @param identifier The custom user identifier
     */
    private void sendIdentifyEvent(@Nullable String identifier) {
        String installationID = Batch.User.getInstallationID();
        if (installationID == null) {
            Logger.error(TAG, "Cannot send identify event since Installation ID is null.");
            return;
        }
        JSONObject params = new JSONObject();
        JSONObject identifiers = new JSONObject();
        try {
            identifiers.put("custom_id", identifier != null ? identifier : JSONObject.NULL);
            identifiers.put("install_id", installationID);
            params.put("identifiers", identifiers);
            trackerModule.track(InternalEvents.PROFILE_IDENTIFY, params);
        } catch (JSONException e) {
            Logger.error(TAG, "Sending identify event failed", e);
        }
    }

    //region Event Tracking

    /**
     * Track a public event
     *
     * @param event Event name
     * @param eventAttributes  Event attributes
     */
    public void trackPublicEvent(@NonNull String event, @Nullable BatchEventAttributes eventAttributes) {
        // Event name validation
        boolean nameValidated = !TextUtils.isEmpty(event) && EventAttributesValidator.isEventNameValid(event);
        if (!nameValidated) {
            Logger.error(TAG, "Invalid event name ('" + event + "'). Not tracking event.");
            return;
        }
        JSONObject eventParameters = new JSONObject();
        if (eventAttributes != null) {
            // Event data validation
            List<String> errors = eventAttributes.validateEventAttributes();
            if (!errors.isEmpty()) {
                Logger.error(
                    TAG,
                    "Failed to validate event attributes:\n\n" + String.join("\n", errors) + "\n\nNot tracking event."
                );
                return;
            }
            // Event data serialization
            try {
                eventParameters = EventAttributesSerializer.serialize(eventAttributes);
            } catch (JSONException | ClassCastException e) {
                Logger.error(
                    TAG,
                    "Could not process BatchEventAttributes, refusing to track event. This is an internal error: please contact us."
                );
                Logger.internal(TAG, "BatchEventAttributes serialization did failed - Not tracking event.", e);
                return;
            }
        }
        trackerModule.track("E." + event.toUpperCase(Locale.US), eventParameters);
    }
    //endregion
}
