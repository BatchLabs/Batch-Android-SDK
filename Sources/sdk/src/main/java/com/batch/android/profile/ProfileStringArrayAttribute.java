package com.batch.android.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class ProfileStringArrayAttribute {

    @NonNull
    private ProfileDeletableAttribute<List<String>> attribute = new ProfileDeletableAttribute<>();

    @Nullable
    private ProfilePartialUpdateAttribute partialUpdates;

    public ProfileStringArrayAttribute() {}

    public ProfileStringArrayAttribute(@Nullable List<String> value) {
        attribute.setValue(value != null ? ProfileDataHelper.deduplicateKeepLast(value) : null);
    }

    public ProfileStringArrayAttribute(@NonNull ProfileStringArrayAttribute profileStringArrayAttribute) {
        this.attribute = profileStringArrayAttribute.getAttribute();
        this.partialUpdates = profileStringArrayAttribute.getPartialUpdates();
    }

    @NonNull
    public ProfileDeletableAttribute<List<String>> getAttribute() {
        return attribute;
    }

    @Nullable
    public ProfilePartialUpdateAttribute getPartialUpdates() {
        return partialUpdates;
    }

    /**
     * Set the attribute value
     *
     * @param value The new value
     */
    public void setAttribute(@Nullable List<String> value) {
        attribute.setValue(value != null ? ProfileDataHelper.deduplicateKeepLast(value) : null);
    }

    /**
     * Add values to the attribute
     *
     * @param values List of values to add
     * @throws ProfileDataHelper.AttributeValidationException If the validation fails
     */
    public void addToArray(@NonNull List<String> values) throws ProfileDataHelper.AttributeValidationException {
        // Case: we already have value (setAttribute has been called beforehand)
        if (attribute.getValue() != null) {
            List<String> updatedList = new ArrayList<>(attribute.getValue());
            updatedList.addAll(values);
            List<String> deduped = ProfileDataHelper.deduplicateKeepLast(updatedList);
            ProfileDataHelper.validateStringArray(deduped);
            attribute.setValue(deduped);
            return;
        }

        // Case: we have null value (removeAttribute has been called beforehand)
        if (attribute.shouldDelete()) {
            List<String> deduped = ProfileDataHelper.deduplicateKeepLast(values);
            ProfileDataHelper.validateStringArray(deduped);
            attribute.setValue(deduped);
            return;
        }

        // Case: we already have partials updates (addTo or removeFrom has been called beforehand)
        if (this.partialUpdates != null) {
            ProfilePartialUpdateAttribute updatedPartialUpdates = new ProfilePartialUpdateAttribute(partialUpdates);
            updatedPartialUpdates.putInAdded(values);
            ProfileDataHelper.assertNotNull(updatedPartialUpdates.getAdded()); // Should never be null since putInAdded is called beforehand but remove warning on getAdded
            ProfileDataHelper.validateStringArray(updatedPartialUpdates.getAdded());
            this.partialUpdates = updatedPartialUpdates;
            return;
        }

        // Case: attribute doesn't exist
        List<String> deduped = ProfileDataHelper.deduplicateKeepLast(values);
        ProfileDataHelper.validateStringArray(deduped);
        this.partialUpdates = new ProfilePartialUpdateAttribute(deduped);
    }

    /**
     * Remove values from the attribute
     *
     * @param values List of values to remove
     * @throws ProfileDataHelper.AttributeValidationException If the validation fails
     */
    public void removeFromArray(@NonNull List<String> values) throws ProfileDataHelper.AttributeValidationException {
        // Case: we already have value (set has been called before)
        if (attribute.getValue() != null) {
            List<String> updatedList = new ArrayList<>(attribute.getValue());
            updatedList.removeAll(values);
            attribute.setValue(updatedList);
            return;
        }

        // Case: we already have partials updates (addTo or removeFrom has been called before)
        if (this.partialUpdates != null) {
            ProfilePartialUpdateAttribute updatedPartialUpdates = new ProfilePartialUpdateAttribute(partialUpdates);
            updatedPartialUpdates.putInRemoved(values);
            ProfileDataHelper.assertNotNull(updatedPartialUpdates.getRemoved()); // Should never be null since putInRemoved is called beforehand but remove warning on getRemoved
            ProfileDataHelper.validateStringArray(updatedPartialUpdates.getRemoved());
            this.partialUpdates = updatedPartialUpdates;
            return;
        }

        // Case: attribute doesn't exist
        List<String> deduped = ProfileDataHelper.deduplicateKeepLast(values);
        ProfileDataHelper.validateStringArray(deduped);
        this.partialUpdates = new ProfilePartialUpdateAttribute(null, deduped);
    }

    /**
     * Check if the attribute has an empty values
     */
    public boolean isEmpty() {
        return (
            (attribute.getValue() == null || attribute.getValue().isEmpty()) &&
            (partialUpdates == null || partialUpdates.isEmpty())
        );
    }

    /**
     * Validate the attribute and partial updates values
     * @throws ProfileDataHelper.AttributeValidationException If the validation fails
     */
    public void validate() throws ProfileDataHelper.AttributeValidationException {
        validateAttribute();
        validatePartialUpdates();
    }

    /**
     * Validate the attribute values
     * @throws ProfileDataHelper.AttributeValidationException If the validation fails
     */
    public void validateAttribute() throws ProfileDataHelper.AttributeValidationException {
        if (attribute.getValue() != null) {
            ProfileDataHelper.validateStringArray((List<String>) attribute.getValue());
        }
    }

    /**
     * Validate the partial updates values
     * @throws ProfileDataHelper.AttributeValidationException If the validation fails
     */
    public void validatePartialUpdates() throws ProfileDataHelper.AttributeValidationException {
        if (partialUpdates != null) {
            if (partialUpdates.getAdded() != null) {
                ProfileDataHelper.validateStringArray((List<String>) partialUpdates.getAdded());
            }
            if (partialUpdates.getRemoved() != null) {
                ProfileDataHelper.validateStringArray((List<String>) partialUpdates.getRemoved());
            }
        }
    }

    /**
     * Serialize the attribute to JSON
     * @return The serialized attribute
     * @throws JSONException If the serialization fails
     */
    @Nullable
    public Object toJSON() throws JSONException {
        Object serializedValue = attribute.getSerializedValue();
        if (serializedValue != null) {
            return serializedValue;
        } else if (partialUpdates != null) {
            return partialUpdates.getSerializedValue();
        }
        return null;
    }
}
