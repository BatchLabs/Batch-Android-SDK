package com.batch.android.core;

import androidx.annotation.NonNull;
import com.batch.android.core.TypedIDExceptions.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;

public class TypedID {

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    static final String CUSTOM_ENCODING = "0123456789abcdefghjkmnpqrstvwxyz";

    private static final char SEPARATOR = '_';
    private static final int MAX_TYPE_LENGTH = 10;
    private static final int BASE32_PART_LENGTH = 32;

    private static final Base32Encoding base32Encoding = new Base32Encoding(CUSTOM_ENCODING);

    public final String type;
    public final Ulid ulid;

    TypedID(String type, Ulid ulid) throws InvalidTypeException {
        checkTypeIsAlnum(type);

        Objects.requireNonNull(type);
        Objects.requireNonNull(ulid);

        this.type = type;
        this.ulid = ulid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypedID typedID = (TypedID) o;
        return type.equals(typedID.type) && ulid.equals(typedID.ulid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ulid);
    }

    /**
     * Encodes the binary representation of this typed identifier to a byte array
     *
     * @return a binary representation of the typed identifier
     */
    public byte[] toBytes() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte b : type.getBytes(UTF_8_CHARSET)) {
            output.write(b);
        }
        output.write(SEPARATOR);
        for (byte b : ulid.toBytes()) {
            output.write(b);
        }
        return output.toByteArray();
    }

    /**
     * Decodes a binary representation of a typed identifier.
     *
     * @param input The bytes of the typed identifier
     * @return A valid typed identifier
     * @throws InvalidTypeException      when the type is invalid for some reason
     * @throws InvalidSizeException      when some part of the typed identifier doesn't have the right size
     * @throws InvalidSeparatorException when the separator is invalid or not present
     */
    public static TypedID fromBytes(byte[] input)
        throws InvalidSeparatorException, InvalidTypeException, InvalidSizeException {
        Objects.requireNonNull(input);

        // Find the separator
        int separatorPos = -1;
        for (int i = 0; i < input.length; i++) {
            if (input[i] == SEPARATOR) {
                separatorPos = i;
                break;
            }
        }
        if (separatorPos == -1) {
            throw new InvalidSeparatorException();
        }
        if (separatorPos > MAX_TYPE_LENGTH) {
            throw new InvalidTypeException("type is too long");
        }

        // Extract and validate the type
        String type = new String(input, 0, separatorPos, UTF_8_CHARSET);
        checkTypeIsAlnum(type);

        // Extract the ULID
        int remainingLength = input.length - (separatorPos + 1);
        if (remainingLength != Ulid.ULID_BYTES) {
            String message = String.format(Locale.US, "invalid ulid of length %d", remainingLength);
            throw new InvalidSizeException(message);
        }

        byte[] ulidBytes = new byte[Ulid.ULID_BYTES];
        System.arraycopy(input, separatorPos + 1, ulidBytes, 0, Ulid.ULID_BYTES);

        Ulid ulid;
        try {
            ulid = Ulid.from(ulidBytes);
        } catch (Ulid.InvalidBufferSizeException e) {
            throw new InvalidSizeException(e.getMessage());
        }

        return newWithULID(type, ulid);
    }

    /**
     * Encodes the text representation of this typed identifier to a string
     *
     * @return a text representation of the typed identifier
     */
    @NonNull
    public String toString() {
        // First get the checksum
        byte[] checksummableBytes = toBytes();
        int checksum = getChecksum(checksummableBytes);

        // Then encode
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte b : type.getBytes(UTF_8_CHARSET)) {
            output.write(b);
        }
        output.write(SEPARATOR);
        encodeMainPart(ulid, checksum, output);

        return new String(output.toByteArray(), UTF_8_CHARSET);
    }

    /**
     * Generates a new typed identifier of the type {@code type} and with a new random ULID using
     *
     * @param type The type of the typed identifier
     * @return A valid typed identifier instance
     * @throws InvalidTypeException when the type is invalid for some reason
     */
    public static TypedID newWithRandomULID(String type) throws InvalidTypeException {
        Ulid ulid = Ulid.randomUlid();
        return newWithULID(type, ulid);
    }

    /**
     * Generates a new typed identifier of the type {@code type} and with the ULID {@code ulid}.
     * Does some sanity checks on the type and ULID.
     *
     * @param type The type of the typed identifier
     * @param ulid The ULID for the typed identifier
     * @return A valid typed identifier instance
     * @throws InvalidTypeException when the type is invalid for some reason
     */
    public static TypedID newWithULID(String type, Ulid ulid) throws InvalidTypeException {
        Objects.requireNonNull(type);
        if (type.length() == 0) {
            throw new InvalidTypeException("type is empty");
        }
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new InvalidTypeException("type " + type + " is invalid");
        }
        Objects.requireNonNull(ulid);

        return new TypedID(type, ulid);
    }

    /**
     * Generates a new typed identifier of the type {@code type} and with the UUID {@code uuid}.
     * Does some sanity checks on the type and ULID.
     *
     * @param type The type of the typed identifier
     * @param uuid The base UUID of the ULID for the typed identifier (
     * @return A valid typed identifier instance
     * @throws InvalidTypeException when the type is invalid for some reason
     */
    public static TypedID newWithUUID(String type, UUID uuid) throws InvalidTypeException, InvalidSizeException {
        Objects.requireNonNull(type);
        if (type.length() == 0) {
            throw new InvalidTypeException("type is empty");
        }
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new InvalidTypeException("type " + type + " is invalid");
        }
        ByteBuffer buffer = ByteBuffer
            .allocate(16)
            .putLong(uuid.getMostSignificantBits())
            .putLong(uuid.getLeastSignificantBits());
        Ulid ulid;
        try {
            ulid = Ulid.from(buffer.array());
        } catch (Ulid.InvalidBufferSizeException e) {
            throw new InvalidSizeException(e.getMessage());
        }
        Objects.requireNonNull(ulid);
        return new TypedID(type, ulid);
    }

    /**
     * Parse parses the input string as a typed identifier.
     *
     * @param input The input string
     * @return A valid typed identifier
     * @throws InvalidTypeException      when the type is invalid for some reason
     * @throws InvalidSizeException      when some part of the typed identifier doesn't have the right size
     * @throws InvalidSeparatorException when the separator is invalid or not present
     * @throws InvalidChecksumException  when the checksum is invalid
     * @throws InvalidIDException        when any part of the typed identifier is invalid
     */
    public static TypedID parse(String input)
        throws InvalidTypeException, InvalidSizeException, InvalidSeparatorException, InvalidChecksumException, InvalidIDException {
        Objects.requireNonNull(input);

        int separatorPos = input.indexOf(SEPARATOR);
        if (separatorPos == -1) {
            throw new InvalidSeparatorException();
        }

        // Extract and validate the type
        String type = input.substring(0, separatorPos);
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new InvalidTypeException("type is too long");
        }
        checkTypeIsAlnum(type);

        // Decode the main part
        String mainPart = input.substring(separatorPos + 1);
        DecodedMainPart decodedMainPart = decodeMainPart(mainPart);

        // Compute the checksum and verify it
        ByteArrayOutputStream checksummableBytes = new ByteArrayOutputStream();
        for (byte b : type.getBytes(UTF_8_CHARSET)) {
            checksummableBytes.write(b);
        }
        checksummableBytes.write(SEPARATOR);
        for (byte b : decodedMainPart.ulid.toBytes()) {
            checksummableBytes.write(b);
        }

        int computedChecksum = getChecksum(checksummableBytes.toByteArray());
        if (computedChecksum != decodedMainPart.checksum) {
            String message = String.format("invalid checksum %x", decodedMainPart.checksum);
            throw new InvalidChecksumException(message);
        }

        // Finally populate the result typed identifier
        return new TypedID(type, decodedMainPart.ulid);
    }

    /**
     * Encode the main part of a typed identifier to base32
     *
     * @param ulid     The ULID of the typed identifier
     * @param checksum The checksum, CRC32(type+separator+ulid bytes)
     * @param output   The base32-encoded main part of the typed identifier
     */
    private static void encodeMainPart(Ulid ulid, int checksum, ByteArrayOutputStream output) {
        ByteBuffer buffer = ByteBuffer.allocate(Ulid.ULID_BYTES + 4);
        buffer.put(ulid.toBytes());
        buffer.putInt(checksum);

        try {
            base32Encoding.encode(output, buffer.array());
        } catch (Base32Encoding.EncodeException e) {
            throw new IllegalStateException("invalid main part", e);
        }
    }

    private static final class DecodedMainPart {

        private final Ulid ulid;

        int checksum;

        public DecodedMainPart(Ulid ulid, int checksum) {
            this.ulid = ulid;
            this.checksum = checksum;
        }
    }

    /***
     * Decode the main part of a typed identifier from base32.
     *
     * @param input The base32-encoded main part of a typed identifier
     * @return A {@link DecodedMainPart} record.
     * @throws InvalidSizeException      when some part of the typed identifier doesn't have the right size
     * @throws InvalidIDException        when any part of the typed identifier is invalid
     */
    private static DecodedMainPart decodeMainPart(String input) throws InvalidSizeException, InvalidIDException {
        if (input.length() != BASE32_PART_LENGTH) {
            String message = String.format("invalid main part \"%s\"", input);
            throw new InvalidSizeException(message);
        }

        // Decode the data
        ByteBuffer data;
        try {
            ByteArrayOutputStream decodedData = base32Encoding.decode(input);
            data = ByteBuffer.wrap(decodedData.toByteArray());
        } catch (Base32Encoding.DecodeException e) {
            String message = String.format("invalid base32 main part \"%s\"", input);
            throw new InvalidIDException(message);
        }

        // Extract the ULID
        byte[] ulidData = new byte[Ulid.ULID_BYTES];
        data.get(ulidData);
        Ulid ulid;
        try {
            ulid = Ulid.from(ulidData);
        } catch (Ulid.InvalidBufferSizeException e) {
            throw new InvalidSizeException(e.getMessage());
        }

        // Extract the checksum
        int checksum = data.getInt();

        return new DecodedMainPart(ulid, checksum);
    }

    /**
     * Computes a CRC32 checksum of {@code data}.
     *
     * @param data The data to compute the checksum of
     * @return A CRC32 checksum
     */
    private static int getChecksum(byte[] data) {
        Objects.requireNonNull(data);

        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }

    /**
     * Validates that all characters in {@code type} are ASCII alphanumerical characters.
     *
     * @param type A string
     * @throws InvalidTypeException when the type is invalid for some reason
     */
    private static void checkTypeIsAlnum(String type) throws InvalidTypeException {
        byte[] bytes = type.getBytes(UTF_8_CHARSET);
        for (byte c : bytes) {
            if (isNotAlnum(c)) {
                throw new InvalidTypeException("type contains a non alphanumeric character");
            }
        }
    }

    /**
     * Checks if the character {@code c} is ASCII alphanumerical.
     *
     * @param c The character to check
     * @return true if the character is not alphanumerical, false otherwise.
     */
    private static boolean isNotAlnum(byte c) {
        return (c < '0' || c > '9') && (c < 'A' || c > 'Z') && (c < 'a' || c > 'z');
    }
}
