package com.batch.android.core;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * This class implements base32 encoding and decoding
 * <p>
 * This implementation is not suitable as a general-purpose base32 encoding/decoding because it is tailor-made for use
 * with typed identifiers; this means we always encode 20 bytes and therefore we _do not_ handle padding !
 */
public class Base32Encoding {

    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    static final int INPUT_BLOCK_SIZE = 5;
    static final int ENCODED_BLOCK_SIZE = 8;

    byte[] encode;
    byte[] decodeMap;

    /**
     * Construct a new Base32Encoding using {@param encoder} as the encoding alphabet.
     *
     * @param encoder The encoding alphabet (must be 32 chars)
     */
    public Base32Encoding(String encoder) {
        Objects.requireNonNull(encoder);
        if (encoder.length() != 32) {
            String message = String.format(
                Locale.US,
                "encoder \"%s\" (length: %d) is invalid, must be 32 chars",
                encoder,
                encoder.length()
            );
            throw new IllegalArgumentException(message);
        }

        this.encode = new byte[32];
        System.arraycopy(encoder.getBytes(UTF_8_CHARSET), 0, this.encode, 0, 32);

        this.decodeMap = new byte[256];
        Arrays.fill(this.decodeMap, (byte) 0xff);

        for (int i = 0; i < this.encode.length; i++) {
            byte index = this.encode[i];
            this.decodeMap[index] = (byte) i;
        }
    }

    public static class EncodeException extends Exception {

        public EncodeException(String message) {
            super(message);
        }
    }

    /**
     * Encode the input bytes {@param src} to the output stream {@param outputStream}.
     * This method does _not_ handle padding, therefore {@param src} must have a length that is a multiple of 5.
     *
     * @param outputStream The output stream where we will write the encoded bytes
     * @param src          The input bytes to encode. Its length must be a multiple of 5.
     */
    public void encode(ByteArrayOutputStream outputStream, byte[] src) throws EncodeException {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(src);
        if ((src.length % INPUT_BLOCK_SIZE) != 0) {
            throw new EncodeException("invalid input size, must be a multiple of 5 because padding is not implemented");
        }

        int i = 0; // position in src
        int length = src.length;
        while (length > 0) {
            byte[] buf = new byte[8];
            buf[0] = (byte) ((src[i] & 0b11111000) >> 3);
            buf[1] = (byte) (((src[i] & 0b00000111) << 2) | ((src[i + 1] & 0b11000000) >> 6));
            buf[2] = (byte) ((src[i + 1] & 0b00111110) >> 1);
            buf[3] = (byte) (((src[i + 1] & 0b00000001) << 4) | ((src[i + 2] & 0b11110000) >> 4));
            buf[4] = (byte) (((src[i + 2] & 0b00001111) << 1) | ((src[i + 3] & 0b10000000) >> 7));
            buf[5] = (byte) ((src[i + 3] & 0b01111100) >> 2);
            buf[6] = (byte) (((src[i + 3] & 0b00000011) << 3) | ((src[i + 4] & 0b11100000) >> 5));
            buf[7] = (byte) (src[i + 4] & 0b00011111);

            i += INPUT_BLOCK_SIZE;
            length -= INPUT_BLOCK_SIZE;

            // 8 outputs
            for (byte b : buf) {
                outputStream.write(this.encode[b]);
            }
        }
    }

    public static class DecodeException extends Exception {

        public DecodeException(String message) {
            super(message);
        }
    }

    /**
     * Decode the input string {@param srcString} and writes the resulting bytes to a new {@link ByteArrayOutputStream}.
     *
     * @param srcString The input string to decode
     * @return a {@link ByteArrayOutputStream} containing the decoded data
     * @throws DecodeException if there's any error while decoding
     */
    public ByteArrayOutputStream decode(String srcString) throws DecodeException {
        byte[] src = srcString.getBytes(UTF_8_CHARSET);

        int i = 0; // position in src
        int length = src.length;

        if (length % ENCODED_BLOCK_SIZE != 0) {
            String message = String.format("invalid base32 string \"%s\"", srcString);
            throw new DecodeException(message);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(length);

        while (length > 0) {
            byte[] buf = new byte[ENCODED_BLOCK_SIZE];

            System.arraycopy(src, i, buf, 0, buf.length);

            length -= buf.length;
            i += buf.length;

            for (int j = 0; j < buf.length; j++) {
                buf[j] = this.decodeMap[buf[j]];
                if (buf[j] == (byte) 0xff) {
                    String message = String.format("invalid base32 string \"%s\"", srcString);
                    throw new DecodeException(message);
                }
            }

            output.write(((buf[0] & 0b00011111) << 3) | ((buf[1] & 0b00011100) >> 2));
            output.write(((buf[1] & 0b00000011) << 6) | ((buf[2] & 0b00011111) << 1) | ((buf[3] & 0b00010000) >> 4));
            output.write(((buf[3] & 0b00001111) << 4) | ((buf[4] & 0b00011110) >> 1));
            output.write(((buf[4] & 0b00000001) << 7) | ((buf[5] & 0b00011111) << 2) | ((buf[6] & 0b00011000) >> 3));
            output.write(((buf[6] & 0b00000111) << 5) | (buf[7] & 0b00011111));
        }

        return output;
    }
}
