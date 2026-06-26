package com.batch.android.profile;

import static com.batch.android.profile.ProfileDataHelper.TOPIC_PREFERENCE_MAX_SIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ProfileDataHelperTest {

    @Test
    public void testIsNotValidEmail() {
        // Valid use case
        Assert.assertFalse(ProfileDataHelper.isNotValidEmail("foo@batch.com"));
        Assert.assertFalse(ProfileDataHelper.isNotValidEmail("bar@foo.batch.com"));
        Assert.assertFalse(ProfileDataHelper.isNotValidEmail("bar+foo@batch.com"));
        Assert.assertFalse(ProfileDataHelper.isNotValidEmail("FOObar@Test.Batch.COM"));

        // Invalid use case
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail("@gmail.com"));
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail("invalid@gmail"));
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail("inva\nlid@gmail.com"));
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail("invalid@gmail .com"));
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail("invalid@inva lid.gmail.com"));
        Assert.assertTrue(ProfileDataHelper.isNotValidEmail(buildStringOfLength(248).concat("@gmail.com")));
    }

    @Test
    public void testIsNotValidCustomUserID() {
        Assert.assertFalse(ProfileDataHelper.isNotValidCustomUserID("customId"));
        Assert.assertFalse(ProfileDataHelper.isNotValidCustomUserID(null));
        Assert.assertTrue(ProfileDataHelper.isNotValidCustomUserID(buildStringOfLength(1025)));
    }

    @Test
    public void testIsBlocklistedCustomUserID() {
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("null"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("(null)"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("nil"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("[object Object]"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("undefined"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("Infinity"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("-Infinity"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("NaN"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("true"));
        Assert.assertTrue(ProfileDataHelper.isBlocklistedCustomUserID("false"));
        Assert.assertFalse(ProfileDataHelper.isBlocklistedCustomUserID(null));
    }

    @Test
    public void testIsNotValidLanguage() {
        Assert.assertFalse(ProfileDataHelper.isNotValidLanguage("fr"));
        Assert.assertTrue(ProfileDataHelper.isNotValidLanguage("F"));
    }

    @Test
    public void testIsNotValidRegion() {
        Assert.assertFalse(ProfileDataHelper.isNotValidLanguage("FR"));
        Assert.assertTrue(ProfileDataHelper.isNotValidLanguage("F"));
    }

    @Test
    public void testNormalizeAttributeKey() throws ProfileDataHelper.AttributeValidationException {
        Assert.assertEquals("normalized_attribute", ProfileDataHelper.normalizeAttributeKey("Normalized_Attribute"));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeAttributeKey("wrong-key")
        );
    }

    @Test
    public void testNormalizeTagValue() throws ProfileDataHelper.AttributeValidationException {
        Assert.assertEquals("normalized_tag", ProfileDataHelper.normalizeTagValue("Normalized_Tag"));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTagValue(buildStringOfLength(65))
        );
    }

    @Test
    public void testNormalizeTopicPreference() throws ProfileDataHelper.AttributeValidationException {
        Assert.assertEquals("news_updates", ProfileDataHelper.normalizeTopicPreference("News_Updates"));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreference("invalid topic")
        );
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreference(null)
        );
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreference(buildStringOfLength(301))
        );
    }

    @Test
    public void testNormalizeTopicPreferences() throws ProfileDataHelper.AttributeValidationException {
        List<String> normalized = ProfileDataHelper.normalizeTopicPreferences(Arrays.asList("News_Updates", "topic2"));
        Assert.assertEquals(Arrays.asList("news_updates", "topic2"), normalized);
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreferences(Collections.emptyList())
        );
        // 26 distinct topics — still too large after dedup
        List<String> tooManyDistinct = new ArrayList<>();
        for (int i = 0; i < (TOPIC_PREFERENCE_MAX_SIZE + 1); i++) {
            tooManyDistinct.add("topic_" + i);
        }
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreferences(tooManyDistinct)
        );
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.normalizeTopicPreferences(Arrays.asList("valid_topic", "invalid topic"))
        );
    }

    @Test
    public void testNormalizeTopicPreferencesDeduplicates() throws ProfileDataHelper.AttributeValidationException {
        // Case-folding duplicates are merged, last occurrence wins
        List<String> result = ProfileDataHelper.normalizeTopicPreferences(Arrays.asList("Sport", "News", "sport"));
        Assert.assertEquals(Arrays.asList("news", "sport"), result);

        // 26 topics but 2 are duplicates — dedup brings to 25, which is valid
        List<String> topics = new ArrayList<>();
        for (int i = 0; i < TOPIC_PREFERENCE_MAX_SIZE; i++) {
            topics.add("topic_" + i);
        }
        topics.add("topic_0"); // duplicate of first
        List<String> deduped = ProfileDataHelper.normalizeTopicPreferences(topics);
        Assert.assertEquals(25, deduped.size());
        Assert.assertEquals("topic_0", deduped.get(deduped.size() - 1)); // moved to end
    }

    @Test
    public void testIsNotValidPhoneNumber() {
        // Valid use case
        Assert.assertFalse(ProfileDataHelper.isNotValidPhoneNumber("+2901234"));
        Assert.assertFalse(ProfileDataHelper.isNotValidPhoneNumber("+33612345678"));
        Assert.assertFalse(ProfileDataHelper.isNotValidPhoneNumber("+123456789123145"));
        Assert.assertFalse(ProfileDataHelper.isNotValidPhoneNumber(null));

        // Invalid use case
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("+")); // without digits
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("+1234567891231456")); // +16 digits
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("33612345678")); // Missing + char
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("+33-6-12-34-56-78")); // with dashes
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("+33 6 12 34 56 78")); // with spaces
        Assert.assertTrue(ProfileDataHelper.isNotValidPhoneNumber("")); // empty
    }

    @Test
    public void testIsNotValidMEPStringValue() throws ProfileDataHelper.AttributeValidationException {
        ProfileDataHelper.validateMEPStringValue("foo");
        ProfileDataHelper.validateMEPStringValue(buildStringOfLength(64));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.validateMEPStringValue(buildStringOfLength(65))
        );
    }

    @Test
    public void testValidateCEPStringValue() throws ProfileDataHelper.AttributeValidationException {
        ProfileDataHelper.validateCEPStringValue("foo");
        ProfileDataHelper.validateCEPStringValue(buildStringOfLength(300));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.validateCEPStringValue("")
        );
    }

    @Test
    public void testValidateArrayStringValue() throws ProfileDataHelper.AttributeValidationException {
        ProfileDataHelper.validateStringArray(Arrays.asList("foo", "bar"));
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () -> ProfileDataHelper.validateStringArray(Arrays.asList("0", "1", "2", buildStringOfLength(301)))
        );
        Assert.assertThrows(
            ProfileDataHelper.AttributeValidationException.class,
            () ->
                ProfileDataHelper.validateStringArray(
                    Arrays.asList(
                        "0",
                        "1",
                        "2",
                        "3",
                        "4",
                        "5",
                        "6",
                        "7",
                        "8",
                        "9",
                        "10",
                        "11",
                        "12",
                        "13",
                        "14",
                        "15",
                        "16",
                        "17",
                        "18",
                        "19",
                        "20",
                        "21",
                        "22",
                        "23",
                        "24",
                        "25"
                    )
                )
        );
    }

    @Test
    public void testDeduplicateKeepLast() {
        // Spec example: last occurrence wins and the value is repositioned there
        Assert.assertEquals(
            Arrays.asList("e", "d", "f", "a"),
            ProfileDataHelper.deduplicateKeepLast(Arrays.asList("d", "e", "d", "a", "f", "a"))
        );

        // No duplicates — order preserved
        Assert.assertEquals(
            Arrays.asList("a", "b", "c"),
            ProfileDataHelper.deduplicateKeepLast(Arrays.asList("a", "b", "c"))
        );

        // All same — single element
        Assert.assertEquals(
            Collections.singletonList("a"),
            ProfileDataHelper.deduplicateKeepLast(Arrays.asList("a", "a", "a"))
        );

        // Single element — unchanged
        Assert.assertEquals(
            Collections.singletonList("x"),
            ProfileDataHelper.deduplicateKeepLast(Collections.singletonList("x"))
        );
    }

    private static String buildStringOfLength(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'a');
        return new String(chars);
    }
}
