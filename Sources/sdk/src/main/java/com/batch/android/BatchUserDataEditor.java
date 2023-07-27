package com.batch.android;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.GenericHelper;
import com.batch.android.core.Logger;
import com.batch.android.core.Promise;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.module.UserModule;
import com.batch.android.user.EmailSubscription;
import com.batch.android.user.SQLUserDatasource;
import com.batch.android.user.UserOperation;
import com.batch.android.user.UserOperationQueue;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Batch User data editor
 */
@PublicSDK
public class BatchUserDataEditor {

    /**
     * @hide
     */
    public static final String TAG = "BatchUserDataEditor";
    static final Pattern ATTR_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,30}$");

    private static final int LANGUAGE_INDEX = 0;
    private static final int REGION_INDEX = 1;
    private static final int IDENTIFIER_INDEX = 2;
    private static final int ATTR_STRING_MAX_LENGTH = 64; // Also applies to tag values
    private static final int ATTR_URL_MAX_LENGTH = 2048;
    private static final int EMAIL_MAX_LENGTH = 128;

    private final UserOperationQueue operationQueue = new UserOperationQueue();

    private boolean[] updatedFields = { false, false, false };
    private String[] userFields = { null, null, null };

    private EmailSubscription emailSubscription;

    BatchUserDataEditor() {}

    //region Public API

    /**
     * Set the language of this user.<br>
     * Overrides the detected user language.
     *
     * @param language lowercase, ISO 639 formatted string. null to reset.
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor setLanguage(final @Nullable String language) {
        if (language != null && language.trim().length() < 2) {
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
    public BatchUserDataEditor setRegion(final @Nullable String region) {
        if (region != null && region.trim().length() < 2) {
            Logger.error(TAG, "setRegion called with invalid region (must be at least 2 chars)");
            return this;
        }

        this.userFields[REGION_INDEX] = region;
        this.updatedFields[REGION_INDEX] = true;
        return this;
    }

    /**
     * Set the user identifier.<br>
     * Be careful: you should make sure the identifier uniquely identifies a user. When pushing an identifier, all installations with that identifier will get the Push, which can cause some privacy issues if done wrong.
     *
     * @param identifier Identifier string
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor setIdentifier(final @Nullable String identifier) {
        if (identifier != null && identifier.trim().length() > 1024) {
            Logger.error(TAG, "setIdentifier called with invalid identifier (must be less than 1024 chars)");
            return this;
        }

        this.userFields[IDENTIFIER_INDEX] = identifier;
        this.updatedFields[IDENTIFIER_INDEX] = true;
        return this;
    }

    /**
     * Set the user email.
     *
     * Note: This method requires to already have a registered identifier for the user
     * or to call {@link BatchUserDataEditor#setIdentifier} method before this one.
     * @param email Email string
     * @return This object instance, for method chaining.
     */
    public BatchUserDataEditor setEmail(final @Nullable String email) {
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.warning(TAG, "Batch does not have a context yet. Make sure Batch is started beforehand.");
            return this;
        }

        // Ensure we already have a custom user identifier
        // or setIdentifier has been previously called in this editor instance
        if (
            Batch.User.getIdentifier(context) == null &&
            (
                !updatedFields[IDENTIFIER_INDEX] ||
                (updatedFields[IDENTIFIER_INDEX] && userFields[IDENTIFIER_INDEX] == null)
            )
        ) {
            Logger.error(
                TAG,
                "setEmail called whereas identifier is null. Please ensure to call setIdentifier before using this method."
            );
            return this;
        }

        // Deleting email case
        if (email == null) {
            if (emailSubscription == null) {
                emailSubscription = new EmailSubscription(null);
            } else {
                emailSubscription.setEmail(null);
            }
            return this;
        }

        // Ensure email is not too long
        if (email.length() > EMAIL_MAX_LENGTH) {
            Logger.error(TAG, "Email can't be longer than " + EMAIL_MAX_LENGTH + " characters. Ignoring.");
            return this;
        }

        // Ensure email has the right format
        if (!GenericHelper.isValidEmail(email)) {
            Logger.error(
                TAG,
                "setEmail called with invalid email format." +
                " Please ensure to respect the following regex: .@.\\..* "
            );
            return this;
        }

        if (emailSubscription == null) {
            emailSubscription = new EmailSubscription(email.trim());
        } else {
            emailSubscription.setEmail(email.trim());
        }
        return this;
    }

    /**
     * Set the user email marketing subscription state.
     *
     * @param state State of the subscription
     * @return This object instance, for method chaining.
     */
    public BatchUserDataEditor setEmailMarketingSubscriptionState(@NonNull BatchEmailSubscriptionState state) {
        if (emailSubscription == null) {
            emailSubscription = new EmailSubscription();
        }
        emailSubscription.addSubscription(EmailSubscription.Kind.MARKETING, state);
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor setAttribute(final @NonNull String key, final long value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
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
    public BatchUserDataEditor setAttribute(final @NonNull String key, final double value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
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
    public BatchUserDataEditor setAttribute(final @NonNull String key, final boolean value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
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
    public BatchUserDataEditor setAttribute(final @NonNull String key, @NonNull Date value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            return this;
        }

        //noinspection ConstantConditions
        if (value == null) {
            Logger.error(TAG, "setAttribute cannot be used with a null value. Ignoring attribute '" + key + "'");
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
    public BatchUserDataEditor setAttribute(final @NonNull String key, final @NonNull String value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            return this;
        }

        //noinspection ConstantConditions
        if (value == null || value.length() > ATTR_STRING_MAX_LENGTH) {
            Logger.error(
                TAG,
                "String attributes can't be null or longer than " +
                ATTR_STRING_MAX_LENGTH +
                " characters. Ignoring attribute '" +
                key +
                "'"
            );
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.setAttribute(normalizedKey, value));
        return this;
    }

    /**
     * Set a custom user attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a valid URI not longer than 2048 character.
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor setAttribute(final @NonNull String key, final @NonNull URI value) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            return this;
        }

        //noinspection ConstantConditions
        if (value == null || value.toString().length() > ATTR_URL_MAX_LENGTH) {
            Logger.error(
                TAG,
                "URL attributes can't be null or longer than " +
                ATTR_URL_MAX_LENGTH +
                " characters. Ignoring attribute '" +
                key +
                "'"
            );
            return this;
        }
        if (value.getScheme() == null || value.getAuthority() == null) {
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
    public BatchUserDataEditor removeAttribute(@NonNull String key) {
        final String normalizedKey;
        try {
            normalizedKey = normalizeAttributeKey(key);
        } catch (AttributeValidationException e) {
            return this;
        }
        operationQueue.addOperation(datasource -> datasource.removeAttribute(normalizedKey));
        return this;
    }

    /**
     * Removes all attributes.
     *
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor clearAttributes() {
        operationQueue.addOperation(SQLUserDatasource::clearAttributes);
        return this;
    }

    /**
     * Add a tag in the specified collection. If empty, the collection will automatically be created.
     *
     * @param collection The collection to add the tag to. Cannot be null. Must be a string of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param tag        The tag to add. Cannot be null or empty. Must be a string no longer than 64 characters.
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor addTag(final @NonNull String collection, final @NonNull String tag) {
        final String normalizedCollection;
        final String normalizedValue;
        try {
            normalizedCollection = normalizeTagCollection(collection);
        } catch (AttributeValidationException e) {
            Logger.error(
                TAG,
                String.format(
                    "Invalid collection. Please make sure that the collection is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters. Ignoring tag '%s' for collection '%s'.",
                    collection,
                    tag
                )
            );
            return this;
        }
        try {
            normalizedValue = normalizeTagValue(tag);
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
    public BatchUserDataEditor removeTag(final @NonNull String collection, final @NonNull String tag) {
        final String normalizedCollection;
        final String normalizedValue;
        try {
            normalizedCollection = normalizeTagCollection(collection);
        } catch (AttributeValidationException e) {
            Logger.error(
                TAG,
                String.format(
                    "Invalid collection. Please make sure that the collection is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters. Ignoring tag '%s' for collection '%s'.",
                    collection,
                    tag
                )
            );
            return this;
        }
        try {
            normalizedValue = normalizeTagValue(tag);
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
     * Removes all tags.
     *
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor clearTags() {
        operationQueue.addOperation(SQLUserDatasource::clearTags);
        return this;
    }

    /**
     * Removes all tags from a collection.
     * Does nothing if the tag collection does not exist.
     *
     * @param collection Tag collection.
     * @return This object instance, for method chaining
     */
    public BatchUserDataEditor clearTagCollection(final @NonNull String collection) {
        try {
            final String normalizedCollection = normalizeTagCollection(collection);
            operationQueue.addOperation(datasource -> datasource.clearTags(normalizedCollection));
        } catch (AttributeValidationException e) {
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
    public void save() {
        UserOperation userUpdateOperation = getUserUpdateOperation();
        if (userUpdateOperation != null) {
            operationQueue.addFirstOperation(userUpdateOperation);
        }
        UserModuleProvider
            .get()
            .addOperationQueueAndSubmit(500, new UserOperationQueue(operationQueue.popOperations()));
    }

    Promise<Void> saveSync() {
        final Promise<Void> promise = new Promise<>();
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

    private String normalizeAttributeKey(final String key) throws AttributeValidationException {
        if (TextUtils.isEmpty(key) || !ATTR_KEY_PATTERN.matcher(key).matches()) {
            Logger.error(
                TAG,
                "Invalid key. Please make sure that the key is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters. Ignoring attribute '" +
                key +
                "'."
            );
            throw new AttributeValidationException();
        }

        return key.toLowerCase(Locale.US);
    }

    private String normalizeTagCollection(final String collection) throws AttributeValidationException {
        if (TextUtils.isEmpty(collection) || !ATTR_KEY_PATTERN.matcher(collection).matches()) {
            throw new AttributeValidationException();
        }

        return collection.toLowerCase(Locale.US);
    }

    private String normalizeTagValue(final String value) throws AttributeValidationException {
        if (TextUtils.isEmpty(value) || value.length() > ATTR_STRING_MAX_LENGTH) {
            throw new AttributeValidationException();
        }

        return value.toLowerCase(Locale.US);
    }

    @Nullable
    private UserOperation getEmailUpdateOperation() {
        if (emailSubscription == null) {
            return null;
        }
        return datasource -> {
            emailSubscription.sendEmailSubscriptionEvent();
        };
    }

    @Nullable
    private UserOperation getUserUpdateOperation() {
        if (!updatedFields[LANGUAGE_INDEX] && !updatedFields[REGION_INDEX] && !updatedFields[IDENTIFIER_INDEX]) {
            // Nothing to do
            return null;
        }

        return datasource -> {
            Context context = RuntimeManagerProvider.get().getContext();
            if (context == null) {
                throw new UserModule.SaveException(
                    "Error while applying. Make sure Batch is started beforehand, and not globally opted out from.",
                    "'context' was null while saving."
                );
            }

            String[] previousUserFields = new String[] {
                Batch.User.getLanguage(context),
                Batch.User.getRegion(context),
                Batch.User.getIdentifier(context),
            };

            final User user = new User(context);
            if (updatedFields[LANGUAGE_INDEX]) {
                user.setLanguage(userFields[LANGUAGE_INDEX]);
            }

            if (updatedFields[REGION_INDEX]) {
                user.setRegion(userFields[REGION_INDEX]);
            }

            if (updatedFields[IDENTIFIER_INDEX]) {
                user.setCustomID(userFields[IDENTIFIER_INDEX]);
            }

            if (!Arrays.equals(userFields, previousUserFields)) {
                // At least one field has changed, we send new version of data
                user.sendChangeEvent();
            }
        };
    }

    private List<UserOperation> popOperationQueue() {
        final List<UserOperation> pendingOperationQueue = operationQueue.popOperations();
        UserOperation userUpdateOperation = getUserUpdateOperation();
        if (userUpdateOperation != null) {
            pendingOperationQueue.add(0, userUpdateOperation);
        }

        UserOperation emailUpdateOperation = getEmailUpdateOperation();
        if (emailUpdateOperation != null) {
            pendingOperationQueue.add(emailUpdateOperation);
        }

        return pendingOperationQueue;
    }

    private static final class AttributeValidationException extends Exception {

        public AttributeValidationException() {
            super();
        }
    }
    //endregion
}
