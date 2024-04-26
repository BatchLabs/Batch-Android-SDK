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
}
