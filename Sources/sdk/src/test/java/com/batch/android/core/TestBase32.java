package com.batch.android.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TestBase32 {

    @Test(expected = IllegalArgumentException.class)
    public void testNewBase32EncodingBadEncoder() {
        new Base32Encoding("abcd");
    }

    @Test(expected = Base32Encoding.DecodeException.class)
    public void testDecodeInvalidLength() throws Base32Encoding.DecodeException {
        String input = "foo";

        Base32Encoding base32Encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        base32Encoding.decode(input);
    }

    @Test(expected = Base32Encoding.DecodeException.class)
    public void testDecodeInvalidChar() throws Base32Encoding.DecodeException {
        String input = "foobirba";

        Base32Encoding base32Encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        base32Encoding.decode(input);
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullOutputStream() throws Base32Encoding.EncodeException {
        Base32Encoding base32Encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        base32Encoding.encode(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullSrc() throws Base32Encoding.EncodeException {
        Base32Encoding base32Encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        base32Encoding.encode(new ByteArrayOutputStream(), null);
    }

    @Test(expected = Base32Encoding.EncodeException.class)
    public void testEncodeInvalidSrcLength() throws Base32Encoding.EncodeException {
        Base32Encoding base32Encoding = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        base32Encoding.encode(new ByteArrayOutputStream(), "foo".getBytes(StandardCharsets.UTF_8));
    }
}
