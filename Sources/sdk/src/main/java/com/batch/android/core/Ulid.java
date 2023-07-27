package com.batch.android.core;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;

public class Ulid {

    /**
     * Charset
     */
    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    /**
     * Generator used to get random bytes when creating a new ulid
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Ulid buffer size (6 timestamp + 10 random)
     */
    public static int ULID_BYTES = 16;

    /**
     * Ulid buffer
     */
    private final byte[] buffer;

    /**
     * Create a new Ulid object from a buffer
     *
     * @param buffer The Ulid buffer, must be 16 bytes
     * @return The Ulid object
     */
    public static Ulid from(byte[] buffer) throws InvalidBufferSizeException {
        if (buffer.length != ULID_BYTES) {
            throw new InvalidBufferSizeException("Ulid must be a 16 bytes buffer");
        }
        return new Ulid(buffer);
    }

    /**
     * Create a new Ulid object from a string
     *
     * @param ulidString The encoded Base32 string representation
     * @return The ulid object
     * @throws Base32Encoding.DecodeException when the ulid string is not valid
     */
    public static Ulid from(String ulidString) throws Base32Encoding.DecodeException {
        Base32Encoding encoder = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        String padding = "aaaaaa";
        ByteArrayOutputStream decodedData = encoder.decode(padding.concat(ulidString.toLowerCase(Locale.US)));
        byte[] buffer = Arrays.copyOfRange(decodedData.toByteArray(), 4, 20);
        return new Ulid(buffer);
    }

    /**
     * Create a new Ulid with the current timestamp and random bytes
     *
     * @return ulid
     */
    public static Ulid randomUlid() {
        byte[] buffer = Ulid.generateUlidBuffer();
        return new Ulid(buffer);
    }

    /**
     * Private constructor
     *
     * @param buffer ulid buffer
     */
    private Ulid(byte[] buffer) {
        this.buffer = buffer;
    }

    /**
     * Get this Ulid as a byte array
     *
     * @return A binary representation of the Ulid
     */
    public byte[] toBytes() {
        return this.buffer;
    }

    /**
     * Get the ULID String representation
     *
     * @return The encoded Base32 string representation
     * @throws Base32Encoding.EncodeException When the buffer to encode is invalid
     */
    public String toULIDString() throws Base32Encoding.EncodeException {
        Base32Encoding encoder = new Base32Encoding(TypedID.CUSTOM_ENCODING);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encoder.encode(bos, ByteArrayHelper.concat(new byte[4], this.buffer));
        return new String(bos.toByteArray(), UTF_8_CHARSET).substring(6);
    }

    /**
     * Generate a new Ulid buffer with the current timestamp + random bytes
     *
     * @return The generated Ulid buffer
     */
    private static byte[] generateUlidBuffer() {
        // Timestamp part (6 bytes)
        DateProvider dateProvider = new SystemDateProvider();
        long timestamp = dateProvider.getCurrentDate().getTime();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putLong(timestamp);
        byte[] timestampBuffer = Arrays.copyOfRange(byteBuffer.array(), 2, 8);

        // Random part (10 bytes)
        byte[] randomBuffer = SECURE_RANDOM.generateSeed(10);

        return ByteArrayHelper.concat(timestampBuffer, randomBuffer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ulid)) {
            return false;
        }
        Ulid ulid = (Ulid) o;
        return Arrays.equals(buffer, ulid.buffer);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(buffer);
    }

    public static class InvalidBufferSizeException extends Exception {

        public InvalidBufferSizeException(String message) {
            super(message);
        }
    }
}
