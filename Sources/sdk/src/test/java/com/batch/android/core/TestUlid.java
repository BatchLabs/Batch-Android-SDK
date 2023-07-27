package com.batch.android.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestUlid {

    @Test
    public void testUlidFromBytes() throws Base32Encoding.EncodeException, Ulid.InvalidBufferSizeException {
        String expected = "01gheds1sa5pfp46t7pt8wth3k";
        Ulid ulid = Ulid.from(
            new byte[] {
                0x01,
                (byte) 0x84,
                0x5c,
                (byte) 0xdc,
                (byte) 0x87,
                0x2a,
                0x2d,
                (byte) 0x9f,
                0x62,
                0x1b,
                0x47,
                (byte) 0xb6,
                (byte) 0x91,
                (byte) 0xcd,
                0x44,
                0x73,
            }
        );
        assertEquals(expected, ulid.toULIDString());
    }

    @Test
    public void testUlidFromString() throws Base32Encoding.DecodeException {
        byte[] expected = new byte[] {
            0x01,
            (byte) 0x84,
            0x5c,
            (byte) 0xdc,
            (byte) 0x87,
            0x2a,
            0x2d,
            (byte) 0x9f,
            0x62,
            0x1b,
            0x47,
            (byte) 0xb6,
            (byte) 0x91,
            (byte) 0xcd,
            0x44,
            0x73,
        };
        Ulid ulid = Ulid.from("01gheds1sa5pfp46t7pt8wth3k");
        assertArrayEquals(expected, ulid.toBytes());
    }
}
