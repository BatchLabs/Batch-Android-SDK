package com.batch.android.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchEmailSubscriptionState;
import com.batch.android.BatchSMSSubscriptionState;
import com.batch.android.user.AttributeType;
import com.batch.android.user.UserAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal SDK representation of an Omnichannel Batch Profile
 */
public class ProfileUpdateOperation {

    /**
     * Profile related email
     */
    @Nullable
    private ProfileDeletableAttribute email;

    /**
     * Profile related email marketing subscription state
     */
    @Nullable
    private BatchEmailSubscriptionState emailMarketing;

    /**
     * Profile related phone number
     */
    @Nullable
    private ProfileDeletableAttribute phoneNumber;

    /**
     * Profile related SMS marketing subscription state
     */
    @Nullable
    private BatchSMSSubscriptionState smsMarketing;

    /**
     * Profile related language
     */
    @Nullable
    private ProfileDeletableAttribute language;

    /**
     * Profile related region
     */
    @Nullable
    private ProfileDeletableAttribute region;

    /**
     * Profile related custom attributes
     */
    @NonNull
    private final Map<String, UserAttribute> customAttributes = new HashMap<>();

    /**
     * Get the email address
     * @return The email address
     */
    @Nullable
    public ProfileDeletableAttribute getEmail() {
        return email;
    }

    /**
     * Set an email address
     * @param email The email address
     */
    public void setEmail(@Nullable String email) {
        this.email = new ProfileDeletableAttribute(email);
    }

    /**
     * Get the email marketing subscription state
     * @return The email marketing subscription state
     */
    @Nullable
    public BatchEmailSubscriptionState getEmailMarketing() {
        return emailMarketing;
    }

    /**
     * Set an email marketing subscription state
     * @param emailMarketing The email marketing subscription state
     */
    public void setEmailMarketing(@NonNull BatchEmailSubscriptionState emailMarketing) {
        this.emailMarketing = emailMarketing;
    }

    /**
     * Get the phone number
     * @return The phone number
     */
    @Nullable
    public ProfileDeletableAttribute getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Set a phone number
     * @param phoneNumber The phone number
     */
    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = new ProfileDeletableAttribute(phoneNumber);
    }

    /**
     * Get the SMS marketing subscription state
     * @return The SMS marketing subscription state
     */
    @Nullable
    public BatchSMSSubscriptionState getSMSMarketing() {
        return smsMarketing;
    }

    /**
     * Set an SMS marketing subscription state
     * @param smsMarketing The SMS marketing subscription state
     */
    public void setSMSMarketing(@NonNull BatchSMSSubscriptionState smsMarketing) {
        this.smsMarketing = smsMarketing;
    }

    /**
     * Get the profile language
     * @return The profile language
     */
    @Nullable
    public ProfileDeletableAttribute getLanguage() {
        return language;
    }

    /**
     * Set a profile language
     * @param language The profile language
     */
    public void setLanguage(@Nullable String language) {
        this.language = new ProfileDeletableAttribute(language);
    }

    /**
     * Get the profile region
     * @return The profile region
     */
    @Nullable
    public ProfileDeletableAttribute getRegion() {
        return region;
    }

    /**
     * Set a profile region
     * @param region The profile region
     */
    public void setRegion(@Nullable String region) {
        this.region = new ProfileDeletableAttribute(region);
    }

    /**
     * Get the profile custom attributes
     * @return The profile custom attributes
     */
    @NonNull
    public Map<String, UserAttribute> getCustomAttributes() {
        return customAttributes;
    }

    /**
     * Add a custom attributes
     * @param key The key of the custom attribute
     * @param attribute The custom attribute
     */
    public void addAttribute(@NonNull String key, @NonNull UserAttribute attribute) {
        this.customAttributes.put(key, attribute);
    }

    /**
     * Add a list of value to an array attribute (existing or not)
     * @param key The key of the array attributes
     * @param values Values to add
     */
    public void addToList(@NonNull String key, @NonNull List<String> values) {
        UserAttribute targetAttribute = this.customAttributes.get(key);

        // Case: Array attribute already exist and is a List (meaning setAttribute(string, list)
        // has already been called on this key
        if (targetAttribute != null && targetAttribute.value instanceof List) {
            ArrayList<String> targetList = (ArrayList<String>) targetAttribute.value;
            targetList.addAll(values);
        }
        // Case: Array attribute already exist and is a Partial Update object ($add/$remove)
        // (meaning addToArray(string, array) has already been called on this key
        else if (targetAttribute != null && targetAttribute.value instanceof ProfilePartialUpdateAttribute) {
            ProfilePartialUpdateAttribute targetPartialUpdate = (ProfilePartialUpdateAttribute) targetAttribute.value;
            targetPartialUpdate.putInAdded(values);
        }
        // Case: Array attribute already exist and is null (meaning removeAttribute(string, list)
        // has already been called on this key)
        else if (targetAttribute != null && targetAttribute.value == null) {
            this.customAttributes.put(key, new UserAttribute(values, AttributeType.STRING_ARRAY));
        }
        // Case: Array attribute doesn't exist
        // (meaning, this key has never been used on this editor instance)
        else {
            UserAttribute newAttribute = new UserAttribute(
                new ProfilePartialUpdateAttribute(values),
                AttributeType.STRING_ARRAY
            );
            this.customAttributes.put(key, newAttribute);
        }
    }

    /**
     * Remove a profile custom attribute
     * @param key The key of custom attribute to remove
     */
    public void removeAttribute(String key) {
        this.customAttributes.put(key, new UserAttribute(null, AttributeType.DELETED));
    }

    /**
     * Remove a list of value from an array attribute
     * @param key The key of the array attributes
     * @param values Values to remove
     */
    public void removeFromList(@NonNull String key, @NonNull List<String> values) {
        UserAttribute targetAttribute = this.customAttributes.get(key);

        // Case: Array attribute already exist and is a List (meaning setAttribute(string, list)
        // has already been called on this key
        if (targetAttribute != null && targetAttribute.value instanceof List) {
            ArrayList<String> targetList = (ArrayList<String>) targetAttribute.value;
            ArrayList<String> value = (ArrayList<String>) values;
            targetList.removeAll(value);
            if (targetList.isEmpty()) {
                this.customAttributes.remove(key);
            }
        }
        // Case: Array attribute already exist and is a Partial Update object ($add/$remove)
        // (meaning addTo/removeFromArray(string, array) has already been called on this key
        else if (targetAttribute != null && targetAttribute.value instanceof ProfilePartialUpdateAttribute) {
            ProfilePartialUpdateAttribute targetPartialUpdate = (ProfilePartialUpdateAttribute) targetAttribute.value;
            targetPartialUpdate.putInRemoved(values);
        }
        // Case: Array attribute already exist and is null (meaning removeAttribute(string, list)
        // has already been called on this key)
        else if (targetAttribute != null && targetAttribute.value == null) {
            // No need to do something here
        }
        // Case: Array attribute doesn't exist
        // (meaning, this key has never been used on this editor instance)
        else {
            UserAttribute newAttribute = new UserAttribute(
                new ProfilePartialUpdateAttribute(null, values),
                AttributeType.STRING_ARRAY
            );
            this.customAttributes.put(key, newAttribute);
        }
    }
}
