package com.batch.android.profile;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.core.Logger;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ProfileDataHelper {

    /**
     * The custom user identifier max length authorized
     */
    private static final int CUSTOM_USER_ID_MAX_LENGTH = 1024;

    /**
     * The email max length authorized
     */
    private static final int EMAIL_MAX_LENGTH = 256;

    /**
     * Valid email pattern
     */
    private static final Pattern EMAIL_KEY_PATTERN = Pattern.compile("^[^@\\r\\n\\t]+@[A-z0-9\\-\\.]+\\.[A-z0-9]+$");

    /**
     * Attribute key pattern
     */
    public static final Pattern ATTR_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,30}$");

    /**
     * The string attribute and tag values max length authorized
     */
    public static final int ATTR_STRING_MAX_LENGTH = 64;

    /**
     * Max size of an array of string
     */
    private static final int ATTR_STRING_ARRAY_MAX_SIZE = 25;
    /**
     * The URL attribute max length authorized
     */
    public static final int ATTR_URL_MAX_LENGTH = 2048;

    /**
     * Whether the identifier is NOT a valid custom user identifier
     *
     * @param identifier The custom user identifier.
     *                   Null is considered as a valid value since it can be sent to delete
     * @return True if NOT valid, false otherwise
     */
    public static boolean isNotValidCustomUserID(@Nullable String identifier) {
        return identifier != null && identifier.trim().length() > CUSTOM_USER_ID_MAX_LENGTH;
    }

    /**
     * Whether given email is valid
     *
     * @param email the profile's email
     * @return true if valid, false otherwise
     */
    public static boolean isNotValidEmail(String email) {
        return email != null && (!EMAIL_KEY_PATTERN.matcher(email).matches() || email.length() > EMAIL_MAX_LENGTH);
    }

    /**
     * Whether the given language is NOT valid.
     *
     * @param language the profile's language (null value allowed)
     * @return true if NOT valid, false otherwise
     */
    public static boolean isNotValidLanguage(@Nullable String language) {
        return language != null && language.trim().length() < 2;
    }

    /**
     * Whether the given region is NOT valid.
     *
     * @param region the profile's language (null value allowed)
     * @return true if NOT valid, false otherwise
     */
    public static boolean isNotValidRegion(@Nullable String region) {
        return region != null && region.trim().length() < 2;
    }

    /**
     * Whether the given string attribute is NOT valid.
     *
     * @param value The value to check
     * @return true if NOT valid, false otherwise
     */
    public static boolean isNotValidStringValue(@Nullable String value) {
        return value == null || value.length() > ATTR_STRING_MAX_LENGTH;
    }

    /**
     * Whether the given URI attribute is too long..
     *
     * @param value The value to check
     * @return true if too long, false otherwise
     */
    public static boolean isURITooLong(@Nullable URI value) {
        return value == null || value.toString().length() > ATTR_URL_MAX_LENGTH;
    }

    /**
     * Whether the given URI attribute is NOT valid.
     *
     * @param value The value to check
     * @return true if NOT valid, false otherwise
     */
    public static boolean isNotValidURIValue(@Nullable URI value) {
        return value == null || value.getScheme() == null || value.getAuthority() == null;
    }

    /**
     * Whether the given List of string attribute is NOT valid.
     *
     * @param values The value to check
     * @return true if NOT valid, false otherwise
     */
    public static boolean isNotValidStringArray(@NonNull List<String> values) {
        if (values.size() > ATTR_STRING_ARRAY_MAX_SIZE) {
            return true;
        }
        for (String value : values) {
            if (value.trim().isEmpty() || value.length() > ATTR_STRING_MAX_LENGTH) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensure the key has the right format and return it lowercase
     *
     * @param key The attribute's key
     * @return The key in lowercase
     * @throws AttributeValidationException Validation exception
     */
    public static String normalizeAttributeKey(final String key) throws AttributeValidationException {
        if (TextUtils.isEmpty(key) || !ATTR_KEY_PATTERN.matcher(key).matches()) {
            throw new AttributeValidationException(AttributeValidationException.Type.INVALID_KEY);
        }
        return key.toLowerCase(Locale.US);
    }

    /**
     * Ensure tag value has the right format and return it in lowercase (install mode only)
     * @param value The tag
     * @return The tag in lowercase
     * @throws AttributeValidationException Validation exception
     */
    public static String normalizeTagValue(final String value) throws AttributeValidationException {
        if (TextUtils.isEmpty(value) || value.length() > ProfileDataHelper.ATTR_STRING_MAX_LENGTH) {
            throw new AttributeValidationException(AttributeValidationException.Type.INVALID_STRING_ITEM);
        }
        return value.toLowerCase(Locale.US);
    }

    /**
     * Assert an attribute value is not null
     *
     * @param value the value to check
     * @throws AttributeValidationException Exception thrown when value is null
     */
    public static void assertNotNull(final Object value) throws AttributeValidationException {
        if (value == null) {
            throw new AttributeValidationException(AttributeValidationException.Type.NULL_VALUE);
        }
    }

    /**
     * Exception thrown when an attribute validation has failed
     */
    public static final class AttributeValidationException extends Exception {

        public enum Type {
            INVALID_KEY,
            INVALID_STRING_ITEM,
            NULL_VALUE,
        }

        private final Type errorType;

        public AttributeValidationException(Type errorType) {
            this.errorType = errorType;
        }

        public void printErrorMessage(String tag, String key) {
            switch (this.errorType) {
                case NULL_VALUE:
                    Logger.error(
                        tag,
                        "setAttribute cannot be used with a null value. Ignoring attribute '" + key + "'"
                    );
                    break;
                case INVALID_KEY:
                    Logger.error(
                        tag,
                        "Invalid key. Please make sure that the key is made of letters, underscores and numbers only (a-zA-Z0-9_). It also can't be longer than 30 characters. Ignoring attribute '" +
                        key +
                        "'."
                    );
                    break;
            }
        }
    }
}
