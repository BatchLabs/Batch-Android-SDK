package com.batch.android.core;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test email formats
 *
 */
public class EmailValidationTest {

    /**
     * Tests email patterns
     */
    @Test
    public void testEmailPatterns() {
        Assert.assertTrue(GenericHelper.isValidEmail("foo@batch.com"));
        Assert.assertTrue(GenericHelper.isValidEmail("bar@foo.batch.com"));
        Assert.assertTrue(GenericHelper.isValidEmail("bar+foo@batch.com"));
        Assert.assertTrue(GenericHelper.isValidEmail("FOObar@Test.Batch.COM"));

        Assert.assertFalse(GenericHelper.isValidEmail("@gmail.com"));
        Assert.assertFalse(GenericHelper.isValidEmail("invalid@gmail"));
        Assert.assertFalse(GenericHelper.isValidEmail("inva lid@gmail.com"));
        Assert.assertFalse(GenericHelper.isValidEmail("invalid@gmail .com"));
        Assert.assertFalse(GenericHelper.isValidEmail("invalid@inva lid.gmail.com"));
    }
}
