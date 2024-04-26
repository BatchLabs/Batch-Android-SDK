package com.batch.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.ProfileModuleProvider;
import com.batch.android.di.providers.RuntimeManagerProvider;
import com.batch.android.di.providers.UserModuleProvider;
import com.batch.android.profile.ProfileDataHelper;
import com.batch.android.profile.ProfileDataHelper.AttributeValidationException;
import com.batch.android.profile.ProfileUpdateOperation;
import com.batch.android.user.AttributeType;
import com.batch.android.user.InstallDataEditor;
import com.batch.android.user.UserAttribute;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Batch Profile Attribute Editor
 * <p>
 * Profiles centralize data and events from multiple sources (Apps, Websites, APIs) in a single place based on the Custom ID.
 * They also store a profile's email address and email subscriptions.
 * <p>
 * The Batch Profile Attribute Editor allows you to update profiles to:
 * <p>
 * Set attributes
 * Set email subscription status and email address
 * Set a language & region
 */
@PublicSDK
public class BatchProfileAttributeEditor extends InstallDataEditor {

    /**
     * Logger tag
     */
    private static final String TAG = "BatchProfileAttributeEditor";

    /**
     * Internal SDK model of an omnichannel profile
     */
    private final ProfileUpdateOperation profileUpdateOperation = new ProfileUpdateOperation();

    /**
     * Constructor
     */
    BatchProfileAttributeEditor() {
        super();
    }

    /**
     * Set the language of this profile.<br>
     * Overrides the detected installation language.
     *
     * @param language lowercase, ISO 639 formatted string. null to reset.
     * @return This object instance, for method chaining
     */
    @Override
    public BatchProfileAttributeEditor setLanguage(final @Nullable String language) {
        if (ProfileDataHelper.isNotValidLanguage(language)) {
            Logger.error(TAG, "setLanguage called with invalid language (must be at least 2 chars)");
            return this;
        }
        this.profileUpdateOperation.setLanguage(language);
        super.setLanguage(language);
        return this;
    }

    /**
     * Set the region of this profile.<br>
     * Overrides the detected installation region.
     *
     * @param region uppercase, ISO 3166 formatted string. null to reset.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setRegion(final @Nullable String region) {
        if (ProfileDataHelper.isNotValidRegion(region)) {
            Logger.error(TAG, "setRegion called with invalid region (must be at least 2 chars)");
            return this;
        }
        this.profileUpdateOperation.setRegion(region);
        super.setRegion(region);
        return this;
    }

    /**
     * Set the profile email address.
     * <p>
     * Note: This method requires to already have a registered identifier for the user
     * or to call {@link Batch.Profile#identify(String)} method before this one.
     * @param email Email address string
     * @return This object instance, for method chaining.
     */
    public BatchProfileAttributeEditor setEmailAddress(final @Nullable String email) {
        Context context = RuntimeManagerProvider.get().getContext();
        if (context == null) {
            Logger.error(TAG, "Batch does not have a context yet. Make sure Batch is started beforehand.");
            return this;
        }

        // Ensure profile is logged in
        String customUserID = UserModuleProvider.get().getCustomID(context);
        if (customUserID == null) {
            Logger.error(
                TAG,
                "You cannot set/reset an email to an anonymous profile. Please use the `Batch.Profile.identify` method beforehand."
            );
            return this;
        }

        // Ensure email is valid
        if (ProfileDataHelper.isNotValidEmail(email)) {
            Logger.error(
                TAG,
                "setEmail called with invalid email format. Please ensure to respect the following regex: .@.\\..* and 128 char max length. "
            );
            return this;
        }
        this.profileUpdateOperation.setEmail(email);
        return this;
    }

    /**
     * Set the profile email marketing subscription state.
     * <p>
     * Note that profile's subscription status is automatically set to unsubscribed when they click
     * an unsubscribe link.
     * @param state State of the subscription
     * @return This object instance, for method chaining.
     */
    public BatchProfileAttributeEditor setEmailMarketingSubscription(@NonNull BatchEmailSubscriptionState state) {
        this.profileUpdateOperation.setEmailMarketing(state);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final long value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            this.profileUpdateOperation.addAttribute(normalizedKey, new UserAttribute(value, AttributeType.LONG));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final double value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            this.profileUpdateOperation.addAttribute(normalizedKey, new UserAttribute(value, AttributeType.DOUBLE));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final boolean value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            this.profileUpdateOperation.addAttribute(normalizedKey, new UserAttribute(value, AttributeType.BOOL));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null. Note that since timezones are not supported, this will typically represent UTC dates.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, @NonNull final Date value) {
        try {
            ProfileDataHelper.assertNotNull(value);
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            this.profileUpdateOperation.addAttribute(normalizedKey, new UserAttribute(value, AttributeType.DATE));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a string not longer than 64 characters. For better results, you should make them upper/lowercase and trim the whitespaces.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final @NonNull String value) {
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
            this.profileUpdateOperation.addAttribute(normalizedKey, new UserAttribute(value, AttributeType.STRING));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a valid URI not longer than 2048 character.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final @NonNull URI value) {
        try {
            ProfileDataHelper.assertNotNull(value);
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
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
            this.profileUpdateOperation.addAttribute(
                    normalizedKey,
                    new UserAttribute(value.toString(), AttributeType.URL)
                );
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.setAttribute(key, value);
        return this;
    }

    /**
     * Set a custom profile attribute for a key.
     *
     * @param key   Attribute key, can't be null. It should be made of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value Attribute value, can't be null or empty. Must be a valid List not longer than X items.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor setAttribute(final @NonNull String key, final @NonNull List<String> value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringArray(value)) {
                Logger.error(
                    TAG,
                    "Array of string attributes must not be longer than 25 items, only values of type String and must respect the string attribute limitations. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            this.profileUpdateOperation.addAttribute(
                    normalizedKey,
                    new UserAttribute(new ArrayList<>(value), AttributeType.STRING_ARRAY)
                );
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.clearTagCollection(key);
        for (String item : value) {
            super.addTag(key, item);
        }
        return this;
    }

    /**
     * Removes a custom attribute.<br>
     * Does nothing if it was not set.
     *
     * @param key Attribute key
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor removeAttribute(@NonNull String key) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            this.profileUpdateOperation.removeAttribute(normalizedKey);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        // We do both: super.removeAttribute and clearTagCollection since we don't know
        // the type for the install-based compat, so we don't know if we should remove
        // a tag collection or an attribute. This will not if attribute or collection doesn't exist.
        super.removeAttribute(key);
        super.clearTagCollection(key);
        return this;
    }

    /**
     * Add a string value in the specified array attribute. If empty, the collection will automatically be created.
     *
     * @param key The array attribute to add the value to. Cannot be null. Must be a string of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param value The value to add. Cannot be null or empty. Must be a string no longer than 64 characters.
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor addToArray(final @NonNull String key, final @NonNull String value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringValue(value)) {
                Logger.error(
                    TAG,
                    "Strings in Array attributes can't be null or longer than " +
                    ProfileDataHelper.ATTR_STRING_MAX_LENGTH +
                    " characters. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            this.profileUpdateOperation.addToList(normalizedKey, Arrays.asList(value));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.addTag(key, value);
        return this;
    }

    /**
     * Add a list of strings in the specified array attribute. If empty, the collection will automatically be created.
     *
     * @param key The array attribute to add the value to. Cannot be null. Must be a string of letters, numbers or underscores ([a-z0-9_]) and can't be longer than 30 characters.
     * @param values The strings to add. Cannot be null or empty. Must be strings no longer than 64 characters and max 25 items
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor addToArray(final @NonNull String key, final @NonNull List<String> values) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringArray(values)) {
                Logger.error(
                    TAG,
                    "Array of string attributes must not be longer than 25 items, only values of type String and must respect the string attribute limitations. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            this.profileUpdateOperation.addToList(normalizedKey, new ArrayList<>(values));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        for (String value : values) {
            super.addTag(key, value);
        }
        return this;
    }

    /**
     * Removes a string from an array attribute.
     * Does nothing if the tag does not exist.
     *
     * @param key Array attribute name
     * @param value The value to remove
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor removeFromArray(final @NonNull String key, final @NonNull String value) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringValue(value)) {
                Logger.error(
                    TAG,
                    "Strings in Array attributes can't be null or longer than " +
                    ProfileDataHelper.ATTR_STRING_MAX_LENGTH +
                    " characters. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            this.profileUpdateOperation.removeFromList(normalizedKey, new ArrayList<>(Arrays.asList(value)));
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        super.removeTag(key, value);
        return this;
    }

    /**
     * Removes a list of strings from an array attribute.
     * Does nothing if the tag does not exist.
     *
     * @param key Array attribute name
     * @param values The values to remove
     * @return This object instance, for method chaining
     */
    public BatchProfileAttributeEditor removeFromArray(final @NonNull String key, final @NonNull List<String> values) {
        try {
            String normalizedKey = ProfileDataHelper.normalizeAttributeKey(key);
            if (ProfileDataHelper.isNotValidStringArray(values)) {
                Logger.error(
                    TAG,
                    "Array of string attributes must not be longer than 25 items, only values of type String and must respect the string attribute limitations. Ignoring attribute '" +
                    key +
                    "'"
                );
                return this;
            }
            this.profileUpdateOperation.removeFromList(normalizedKey, values);
        } catch (AttributeValidationException e) {
            e.printErrorMessage(TAG, key);
            return this;
        }
        for (String value : values) {
            super.removeTag(key, value);
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
        super.save();
        ProfileModuleProvider.get().handleProfileDataChanged(this.profileUpdateOperation);
    }
}
