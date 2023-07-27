package com.batch.android.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.batch.android.core.TypedIDExceptions.InvalidSeparatorException;
import com.batch.android.core.TypedIDExceptions.InvalidTypeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Test;

public class TestTypedID {

    @Test(expected = InvalidSeparatorException.class)
    public void testParseBadSeparator()
        throws InvalidTypeException, TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, TypedIDExceptions.InvalidChecksumException, TypedIDExceptions.InvalidIDException {
        TypedID.parse("foobar");
    }

    @Test(expected = InvalidTypeException.class)
    public void testParseTypeTooLong()
        throws TypedIDExceptions.InvalidIDException, InvalidSeparatorException, TypedIDExceptions.InvalidSizeException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException {
        TypedID.parse("foobarfoobarfoobar_abcd");
    }

    @Test(expected = InvalidTypeException.class)
    public void testParseTypeNotAlphanumerical()
        throws TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, InvalidTypeException, TypedIDExceptions.InvalidIDException, TypedIDExceptions.InvalidChecksumException {
        TypedID.parse("---aaa_abcd");
    }

    @Test(expected = TypedIDExceptions.InvalidSizeException.class)
    public void testParseBadMainPart()
        throws TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException, TypedIDExceptions.InvalidIDException {
        TypedID.parse("project_abcdef");
    }

    @Test(expected = TypedIDExceptions.InvalidIDException.class)
    public void testParseInvalidBase32InMainPart()
        throws TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException, TypedIDExceptions.InvalidIDException {
        TypedID.parse("reference_0625rst59-m26nph834fa9erg25hfft2");
    }

    @Test(expected = InvalidTypeException.class)
    public void testNewWithULIDEmptyType() throws InvalidTypeException {
        TypedID.newWithULID("", Ulid.randomUlid());
    }

    @Test(expected = InvalidTypeException.class)
    public void testNewWithULIDTypeTooLong() throws InvalidTypeException {
        TypedID.newWithULID("foobarfoobarfoobar", Ulid.randomUlid());
    }

    @Test(expected = TypedIDExceptions.InvalidChecksumException.class)
    public void testParseInvalidChecksum()
        throws TypedIDExceptions.InvalidIDException, InvalidSeparatorException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException, TypedIDExceptions.InvalidSizeException {
        TypedID.parse("0eference_0625rst59em26nph834fa9erg25hfft2");
    }

    @Test
    public void testInMap() throws InvalidTypeException {
        TypedID id = TypedID.newWithRandomULID("project");

        HashMap<TypedID, String> m = new HashMap<TypedID, String>();
        m.put(id, "foo1");
        m.put(id, "foo2");

        assertTrue(m.containsKey(id));
        assertEquals(1, m.size());
        assertEquals("foo2", m.get(id));
    }

    @Test
    public void testParse()
        throws TypedIDExceptions.InvalidIDException, InvalidSeparatorException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException, TypedIDExceptions.InvalidSizeException, Ulid.InvalidBufferSizeException {
        String input = "reference_0625rst59em26nph834fa9erg25hfft2";
        byte[] expectedULIDBytes = new byte[] {
            0x01,
            (byte) 0x84,
            0x5c,
            0x67,
            0x45,
            0x4b,
            (byte) 0xa8,
            0x23,
            0x56,
            (byte) 0xd1,
            0x40,
            (byte) 0xc8,
            (byte) 0xf5,
            0x25,
            (byte) 0xd8,
            (byte) 0x80,
        };
        Ulid expectedULID = Ulid.from(expectedULIDBytes);

        TypedID id = TypedID.parse(input);

        assertEquals("reference", id.type);
        assertEquals(expectedULID, id.ulid);
        assertEquals(input, id.toString());
    }

    @Test
    public void testEquals()
        throws TypedIDExceptions.InvalidIDException, InvalidTypeException, TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, TypedIDExceptions.InvalidChecksumException {
        TypedID id1 = TypedID.newWithRandomULID("project");
        TypedID id2 = TypedID.parse(id1.toString());

        assertEquals(id1, id2);
    }

    @Test
    public void testFromBytes()
        throws InvalidSeparatorException, TypedIDExceptions.InvalidSizeException, TypedIDExceptions.InvalidIDException, InvalidTypeException, TypedIDExceptions.InvalidChecksumException, IOException, Base32Encoding.DecodeException {
        TypedID expectedID = TypedID.parse("foobarbarb_0627qs799mkw3qrk59df2a5h96p4vka4");
        Ulid expectedULID = Ulid.from("01GHXY9TAD4Z0XY4SABBRJHCA9");

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write("foobarbarb".getBytes(StandardCharsets.UTF_8));
        data.write('_');
        data.write(expectedULID.toBytes());

        TypedID id = TypedID.fromBytes(data.toByteArray());

        assertEquals(expectedID, id);
    }

    @Test(expected = InvalidTypeException.class)
    public void testNewWithUUIDEmptyType() throws InvalidTypeException, TypedIDExceptions.InvalidSizeException {
        UUID uuid = UUID.fromString("01848670-AF41-9CF0-E1AD-3977185469CE");
        TypedID.newWithUUID("", uuid);
    }

    @Test(expected = InvalidTypeException.class)
    public void testNewWithUUIDTypeTooLong() throws InvalidTypeException, TypedIDExceptions.InvalidSizeException {
        UUID uuid = UUID.fromString("01848670-AF41-9CF0-E1AD-3977185469CE");
        TypedID.newWithUUID("foobarfoobarfoobar", uuid);
    }

    @Test
    public void testNewWithUUID()
        throws InvalidTypeException, Base32Encoding.EncodeException, TypedIDExceptions.InvalidSizeException {
        UUID uuid = UUID.fromString("01848670-AF41-9CF0-E1AD-3977185469CE");
        TypedID id = TypedID.newWithUUID("project", uuid);
        assertEquals("01GJ371BT1KKRE3B9SEWC58TEE", id.ulid.toULIDString().toUpperCase());
    }

    @Test(expected = InvalidSeparatorException.class)
    public void testFromBytesBadSeparator()
        throws InvalidTypeException, TypedIDExceptions.InvalidSizeException, InvalidSeparatorException {
        TypedID.fromBytes("fooba".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = InvalidTypeException.class)
    public void testFromBytesTypeTooLong()
        throws TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, InvalidTypeException {
        TypedID.fromBytes("foobarfoobarfoobar_abcd".getBytes(StandardCharsets.UTF_8));
    }

    @Test(expected = TypedIDExceptions.InvalidSizeException.class)
    public void testFromBytesInvalidULID()
        throws TypedIDExceptions.InvalidSizeException, InvalidSeparatorException, InvalidTypeException, IOException {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.write("foobar_".getBytes(StandardCharsets.UTF_8));
        input.write(0xff);
        input.write(0xfa);
        input.write(0xfc);

        TypedID.fromBytes(input.toByteArray());
    }
}
