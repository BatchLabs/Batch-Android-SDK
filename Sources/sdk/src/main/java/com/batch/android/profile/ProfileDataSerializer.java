package com.batch.android.profile;

import androidx.annotation.NonNull;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.user.UserAttribute;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileDataSerializer {

    private static final String EMAIL_KEY = "email";
    private static final String EMAIL_MARKETING_KEY = "email_marketing";
    private static final String PHONE_NUMBER_KEY = "phone_number";
    private static final String SMS_MARKETING_KEY = "sms_marketing";
    private static final String LANGUAGE_KEY = "language";
    private static final String REGION_KEY = "region";
    private static final String TOPIC_PREFERENCES_KEY = "topic_preferences";
    private static final String CUSTOM_ATTRIBUTES_KEY = "custom_attributes";

    /**
     * Serialize a ProfileDataModel object.
     *
     * @param profileUpdateOperation The object to serialize
     * @return The serialized json object
     * @throws JSONException A potential parsing exception
     */
    @NonNull
    public static JSONObject serialize(@NonNull ProfileUpdateOperation profileUpdateOperation) throws JSONException {
        JSONObject serializedProfile = new JSONObject();

        ProfileDeletableAttribute<String> email = profileUpdateOperation.getEmail();
        if (email != null) {
            serializedProfile.put(EMAIL_KEY, email.getSerializedValue());
        }

        ProfileDeletableAttribute<String> phoneNumber = profileUpdateOperation.getPhoneNumber();
        if (phoneNumber != null) {
            serializedProfile.put(PHONE_NUMBER_KEY, phoneNumber.getSerializedValue());
        }

        ProfileDeletableAttribute<String> language = profileUpdateOperation.getLanguage();
        if (language != null) {
            serializedProfile.put(LANGUAGE_KEY, language.getSerializedValue());
        }

        ProfileDeletableAttribute<String> region = profileUpdateOperation.getRegion();
        if (region != null) {
            serializedProfile.put(REGION_KEY, region.getSerializedValue());
        }

        if (profileUpdateOperation.getEmailMarketing() != null) {
            serializedProfile.put(
                EMAIL_MARKETING_KEY,
                profileUpdateOperation.getEmailMarketing().name().toLowerCase(Locale.US)
            );
        }

        if (profileUpdateOperation.getSMSMarketing() != null) {
            serializedProfile.put(
                SMS_MARKETING_KEY,
                profileUpdateOperation.getSMSMarketing().name().toLowerCase(Locale.US)
            );
        }

        serializedProfile.put(TOPIC_PREFERENCES_KEY, profileUpdateOperation.getTopicPreferences().toJSON());

        Map<String, UserAttribute> customAttributes = profileUpdateOperation.getCustomAttributes();
        if (!customAttributes.isEmpty()) {
            JSONObject serializedCustomAttributes = new JSONObject();
            Map<String, Object> serverAttributesRepresentation = UserAttribute.getServerMapRepresentation(
                customAttributes,
                false
            );
            for (Map.Entry<String, Object> attribute : serverAttributesRepresentation.entrySet()) {
                if (attribute.getValue() instanceof ProfileStringArrayAttribute) {
                    serializedCustomAttributes.put(
                        attribute.getKey(),
                        ((ProfileStringArrayAttribute) attribute.getValue()).toJSON()
                    );
                } else {
                    serializedCustomAttributes.put(
                        attribute.getKey(),
                        attribute.getValue() == null ? JSONObject.NULL : attribute.getValue()
                    );
                }
            }
            serializedProfile.put(CUSTOM_ATTRIBUTES_KEY, serializedCustomAttributes);
        }
        return serializedProfile;
    }

    /**
     * Serialize a ProfilePartialUpdateAttribute
     *
     * @param attribute The object to serialize
     * @return The serialized json object
     * @throws JSONException A potential parsing exception
     */
    @NonNull
    public static JSONObject serializePartialUpdateAttribute(@NonNull ProfilePartialUpdateAttribute attribute)
        throws JSONException {
        JSONObject json = new JSONObject();
        List<String> added = attribute.getAdded();
        List<String> removed = attribute.getRemoved();
        if (added != null && !added.isEmpty()) {
            json.put("$add", new JSONArray(attribute.getAdded()));
        }
        if (removed != null && !removed.isEmpty()) {
            json.put("$remove", new JSONArray(attribute.getRemoved()));
        }
        return json;
    }
}
