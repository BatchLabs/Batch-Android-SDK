package com.batch.android.user;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import com.batch.android.core.Promise;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.module.UserModule;
import com.batch.android.profile.ProfileDataHelper;
import com.batch.android.profile.ProfileDataHelper.AttributeValidationException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Batch User data editor
 */
public class InstallDataEditor {

    public static final String TAG = "InstallDataEditor";
    private static final int LANGUAGE_INDEX = 0;
    private static final int REGION_INDEX = 1;
    private final UserOperationQueue operationQueue = new UserOperationQueue();
    private final boolean[] updatedFields = { false, false };
    private final String[] userFields = { null, null };
    private final UserModule userModule;

    protected InstallDataEditor() {
        this.userModule = UserModuleProvider.get();
    }

    //region Public API

    /**
     * Set the language of this user.<br>
     * Overrides the detected user language.
     *
     * @param language lowercase, ISO 639 formatted string. null to reset.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setLanguage(final @Nullable String language) {
        if (ProfileDataHelper.isNotValidLanguage(language)) {
            Logger.error(TAG, "setLanguage called with invalid language (must be at least 2 chars)");
            return this;
        }
        this.userFields[LANGUAGE_INDEX] = language;
        this.updatedFields[LANGUAGE_INDEX] = true;
        return this;
    }

    /**
     * Set the region of this user.<br>
     * Overrides the detected user region.
     *
     * @param region uppercase, ISO 3166 formatted string. null to reset.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setRegion(final @Nullable String region) {
        if (ProfileDataHelper.isNotValidRegion(region)) {
            Logger.error(TAG, "setRegion called with invalid region (must be at least 2 chars)");
            return this;
        }
        this.userFields[REGION_INDEX] = region;
        this.updatedFields[REGION_INDEX] = true;
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, final long value) {
        final String normalizedKey;
        try {
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, final double value) {
        final String normalizedKey;
        try {
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }

        operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, final boolean value) {
        final String normalizedKey;
        try {
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null. Note that since timezones are not supported, this will typically represent UTC dates.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, @NonNull final Date value) {
        final String normalizedKey;
        try {
            ProfileDataHelper.assertNotNull(value);
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }

        // Dates are mutable!
        final Date date = (Date) value.clone();

        synchronized (operationQueue) {
            operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, date));
        }

        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a string not longer than 64 characters. For better results, you should make them upper/lowercase and trim the whitespaces.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, final @NonNull String value) {
        try {
            ProfileDataHelper.assertNotNull(value);
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringValue(value)) {
                Logger.error(
                    TAG,
                    "String attributes can't be null or longer than " +
                    ProfileDataHelper.ATTR_STRING_MAX_LENGTH +
                    " characters. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a valid URI not longer than 2048 character.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor setAttribute(final @NonNull String key, final @NonNull URI value) {
        final String normalizedKey;
        try {
            ProfileDataHelper.assertNotNull(value);
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        if (ProfileDataHelper.isURITooLong(value)) {
            Logger.error(
                TAG,
                "URL attributes can't be null or longer than " +
                ProfileDataHelper.ATTR_URL_MAX_LENGTH +
                " characters. Ignoring attribute '" +
                key +
                "'"
            );
            return this;
        }
        if (ProfileDataHelper.isNotValidURIValue(value)) {
            Logger.error(
                TAG,
                "URL attributes must follow the format 'scheme://[authority][path][?query][#fragment]'. Ignoring attribute '" +
                key +
                "'"
            );
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        return this;
    }

    /**
     * Removes a custom attribute.<br>
     * Does nothing if it was not set.
     *
     * @param key Attribute key
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor removeAttribute(@NonNull String key) {
        final String normalizedKey;
        try {
            normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.removeAttribute(normalizedKey));
        return this;
    }

    /**
     * Add a tag in the specified collection. If empty, the collection will automatically be created.
     *
     * @param collection The collection to add the tag to. Cannot be null. Must be a string of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param tag        The tag to add. Cannot be null or empty. Must be a string no longer than 64 characters.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor addTag(final @NonNull String collection, final @NonNull String tag) {
        final String normalizedCollection;
        final String normalizedValue;
        try {
            normalizedCollection = ProfileDataHelper.normalizeAttributeKey(collection);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, collection);
            return this;
        }
        try {
            normalizedValue = ProfileDataHelper.normalizeTagValue(tag);
        } catch (AttributeValidationException e) {
            Logger.error(
                TAG,
                String.format(
                    "Invalid tag. Please make sure that the tag is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 255 characters. Ignoring tag '%s' for collection '%s'.",
                    collection,
                    tag
                )
            );
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.addTag(normalizedCollection, normalizedValue));
        return this;
    }

    /**
     * Removes a tag from a collection.
     * Does nothing if the tag does not exist.
     *
     * @param collection Collection name
     * @param tag        Tag name
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor removeTag(final @NonNull String collection, final @NonNull String tag) {
        final String normalizedCollection;
        final String normalizedValue;
        try {
            normalizedCollection = ProfileDataHelper.normalizeAttributeKey(collection);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, collection);
            return this;
        }
        try {
            normalizedValue = ProfileDataHelper.normalizeTagValue(tag);
        } catch (AttributeValidationException e) {
            Logger.error(
                TAG,
                String.format(
                    "Invalid tag. Please make sure that the tag is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 255 characters. Ignoring tag '%s' for collection '%s'.",
                    collection,
                    tag
                )
            );
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.removeTag(normalizedCollection, normalizedValue));
        return this;
    }

    /**
     * Removes all tags from a collection.
     * Does nothing if the tag collection does not exist.
     *
     * @param collection Tag collection.
     * @return This object instance, for method chaining
     */
    protected InstallDataEditor clearTagCollection(final @NonNull String collection) {
        try {
            final String normalizedCollection = ProfileDataHelper.normalizeAttributeKey(collection);
            operationQueue.addOperation(datasource -> datasource.clearTags(normalizedCollection));
        } catch (ProfileDataHelper.AttributeValidationException e) {
            Logger.error(
                TAG,
                String.format("Invalid tag collection. Ignoring tag collection clear request '%s' .", collection)
            );
            return this;
        }
        return this;
    }

    /**
     * Save all of the pending changes made in that editor.
     * Note if Batch is not started, your changes will be enqueue until it start.
     * Once you called "save", you need to get a new editor in order to make further changes.
     * <p>
     * This action cannot be undone.
     */
    protected void save() {
        try {
            executeUserUpdateOperation();
        } catch (UserModule.SaveException e) {
            Logger.error(TAG, "Failed saving custom user operation for install compatibility.");
        }
        final List<UserOperation> pendingOperationQueue = popOperationQueue();
        UserModuleProvider.get().addOperationQueueAndSubmit(500, new UserOperationQueue(pendingOperationQueue));
    }

    protected Promise<Void> saveSync() {
        // Making temporary the method public to not break the tests.
        // This class will be fully internal in the next development.
        final Promise<Void> promise = new Promise<>();
        try {
            executeUserUpdateOperation();
        } catch (UserModule.SaveException e) {
            Logger.error(TAG, "Failed saving custom user operation for install compatibility.");
        }
        final List<UserOperation> pendingOperationQueue = popOperationQueue();
        Runnable runnable = () -> {
            try {
                UserModule.applyUserOperationsSync(pendingOperationQueue);
                promise.resolve(null);
            } catch (UserModule.SaveException e) {
                Logger.error(TAG, e.getMessage());
                promise.reject(e);
            }
        };
        runnable.run();
        return promise;
    }

    //endregion

    //region Private helpers
    private void executeUserUpdateOperation() throws UserModule.SaveException {
        if (!updatedFields[LANGUAGE_INDEX] && !updatedFields[REGION_INDEX]) {
            // Nothing to do
            return;
        }
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            throw new UserModule.SaveException(
                "Error while applying. Make sure Batch is started beforehand, and not globally opted out from.",
                "'context' was null while saving."
            );
        }
        String[] previousUserFields = new String[] { userModule.getLanguage(context), userModule.getRegion(context) };

        if (updatedFields[LANGUAGE_INDEX]) {
            userModule.setLanguage(context, userFields[LANGUAGE_INDEX]);
        }

        if (updatedFields[REGION_INDEX]) {
            userModule.setRegion(context, userFields[REGION_INDEX]);
        }

        if (!Arrays.equals(userFields, previousUserFields)) {
            // At least one field has changed, we increment the user profile version
            userModule.incrementVersion(context);
        }
    }

    private List<UserOperation> popOperationQueue() {
        return operationQueue.popOperations();
    }
    //endregion
}
