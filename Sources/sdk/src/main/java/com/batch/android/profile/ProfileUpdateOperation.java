package com.batch.android.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchEmailSubscriptionState;
import com.batch.android.BatchSMSSubscriptionState;
import com.batch.android.user.AttributeType;
import com.batch.android.user.UserAttribute;
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
    private ProfileDeletableAttribute<String> email;

    /**
     * Profile related email marketing subscription state
     */
    @Nullable
    private BatchEmailSubscriptionState emailMarketing;

    /**
     * Profile related phone number
     */
    @Nullable
    private ProfileDeletableAttribute<String> phoneNumber;

    /**
     * Profile related SMS marketing subscription state
     */
    @Nullable
    private BatchSMSSubscriptionState smsMarketing;

    /**
     * Profile related language
     */
    @Nullable
    private ProfileDeletableAttribute<String> language;

    /**
     * Profile related region
     */
    @Nullable
    private ProfileDeletableAttribute<String> region;

    /**
     * Profile related custom attributes
     */
    @NonNull
    private final Map<String, UserAttribute> customAttributes = new HashMap<>();

    /**
     * Profile related topic preferences
     */
    @NonNull
    private final ProfileStringArrayAttribute topicPreferences = new ProfileStringArrayAttribute();

    /**
     * Get the email address
     * @return The email address
     */
    @Nullable
    public ProfileDeletableAttribute<String> getEmail() {
        return email;
    }

    /**
     * Set an email address
     * @param email The email address
     */
    public void setEmail(@Nullable String email) {
        this.email = new ProfileDeletableAttribute<>(email);
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
    public ProfileDeletableAttribute<String> getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Set a phone number
     * @param phoneNumber The phone number
     */
    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = new ProfileDeletableAttribute<>(phoneNumber);
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
    public ProfileDeletableAttribute<String> getLanguage() {
        return language;
    }

    /**
     * Set a profile language
     * @param language The profile language
     */
    public void setLanguage(@Nullable String language) {
        this.language = new ProfileDeletableAttribute<>(language);
    }

    /**
     * Get the profile region
     * @return The profile region
     */
    @Nullable
    public ProfileDeletableAttribute<String> getRegion() {
        return region;
    }

    /**
     * Set a profile region
     * @param region The profile region
     */
    public void setRegion(@Nullable String region) {
        this.region = new ProfileDeletableAttribute<>(region);
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
     * Get the topic preferences
     * @return The topic preferences
     */
    @NonNull
    public ProfileStringArrayAttribute getTopicPreferences() {
        return topicPreferences;
    }

    /**
     * Set topic preferences
     * @param topics The topic preferences
     */
    public void setTopicPreferences(@Nullable List<String> topics) {
        this.topicPreferences.setAttribute(topics);
    }

    /**
     * Add to topic preferences
     * @param topics The topic preferences
     */
    public void addToTopicPreferences(@NonNull List<String> topics)
        throws ProfileDataHelper.AttributeValidationException {
        this.topicPreferences.addToArray(topics);
    }

    /**
     * Remove from topic preferences
     * @param topics The topic preferences
     */
    public void removeFromTopicPreferences(@NonNull List<String> topics)
        throws ProfileDataHelper.AttributeValidationException {
        this.topicPreferences.removeFromArray(topics);
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
     * Add a list of value to a custom array attribute (existing or not)
     * @param key The key of the array attributes
     * @param values Values to add
     */
    public void addToCustomArrayAttribute(@NonNull String key, @NonNull List<String> values)
        throws ProfileDataHelper.AttributeValidationException {
        UserAttribute targetAttribute = this.customAttributes.get(key);

        if (targetAttribute != null && targetAttribute.value instanceof ProfileStringArrayAttribute) {
            ProfileStringArrayAttribute targetArrayAttribute = (ProfileStringArrayAttribute) targetAttribute.value;
            targetArrayAttribute.addToArray(values);
        } else if (targetAttribute != null && targetAttribute.value == null) {
            ProfileStringArrayAttribute arrayAttribute = new ProfileStringArrayAttribute((List<String>) null);
            arrayAttribute.addToArray(values);
            UserAttribute newAttribute = new UserAttribute(arrayAttribute, AttributeType.STRING_ARRAY);
            this.customAttributes.put(key, newAttribute);
        } else {
            ProfileStringArrayAttribute arrayAttribute = new ProfileStringArrayAttribute();
            arrayAttribute.addToArray(values);
            UserAttribute newAttribute = new UserAttribute(arrayAttribute, AttributeType.STRING_ARRAY);
            this.customAttributes.put(key, newAttribute);
        }
    }

    /**
     * Remove a list of value from a custom array attribute
     * @param key The key of the array attributes
     * @param values Values to remove
     */
    public void removeFromCustomArrayAttribute(@NonNull String key, @NonNull List<String> values)
        throws ProfileDataHelper.AttributeValidationException {
        UserAttribute targetAttribute = this.customAttributes.get(key);

        if (targetAttribute != null && targetAttribute.value instanceof ProfileStringArrayAttribute) {
            ProfileStringArrayAttribute targetArrayAttribute = (ProfileStringArrayAttribute) targetAttribute.value;
            ProfileStringArrayAttribute updatedArrayAttribute = new ProfileStringArrayAttribute(targetArrayAttribute);
            updatedArrayAttribute.removeFromArray(values);
            if (updatedArrayAttribute.isEmpty()) {
                this.customAttributes.remove(key);
            } else {
                updatedArrayAttribute.validate();
                this.customAttributes.put(key, new UserAttribute(updatedArrayAttribute, AttributeType.STRING_ARRAY));
            }
        } else if (targetAttribute == null || targetAttribute.value != null) {
            ProfileDataHelper.validateStringArray(values);
            ProfileStringArrayAttribute arrayAttribute = new ProfileStringArrayAttribute();
            arrayAttribute.removeFromArray(values);
            UserAttribute newAttribute = new UserAttribute(arrayAttribute, AttributeType.STRING_ARRAY);
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
}
