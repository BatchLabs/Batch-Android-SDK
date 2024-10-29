package com.batch.android.profile;

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
        Assert.assertTrue(
            ProfileDataHelper.isNotValidEmail(
                "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest@gmail.com"
            )
        );
    }

    @Test
    public void testIsNotValidCustomUserID() {
        Assert.assertFalse(ProfileDataHelper.isNotValidCustomUserID("customId"));
        Assert.assertFalse(ProfileDataHelper.isNotValidCustomUserID(null));
        Assert.assertTrue(
            ProfileDataHelper.isNotValidCustomUserID(
                "my_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_idmy_test_id_1111"
            )
        );
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
}
