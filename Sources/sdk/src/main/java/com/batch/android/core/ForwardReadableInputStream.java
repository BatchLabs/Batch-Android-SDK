package com.batch.android.core;

import androidx.annotation.NonNull;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link java.io.InputStream} wrapper that allows one to read the first X bytes of an InputStream
 * on non markable streams, and use the wrapped stream as if it wasn't ever read.
 * This class will throw if the wrapped stream does not have enough bytes to read the wanted
 * bytes.
 * <p>
 * Calling {@link #read()} will be passed through the wrapped stream directly.
 * <p>
 * IMPORTANT NOTE: This class does NOT forward .close() to the wrapped input stream.
 */
public class ForwardReadableInputStream extends InputStream {

    private int[] firstBytes;

    private InputStream wrappedInputStream;

    private int readPosition = 0;
    private int maxReadPosition;

    public ForwardReadableInputStream(@NonNull InputStream is, int bytesToRead) throws IOException {
        firstBytes = new int[bytesToRead];
        maxReadPosition = bytesToRead - 1;
        wrappedInputStream = is;
        readFirstBytes(bytesToRead);
    }

    private void readFirstBytes(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            int b = wrappedInputStream.read();
            if (b == -1) {
                throw new IOException("Stream terminated abruptly");
            }
            firstBytes[i] = b;
        }
    }

    @Override
    public int read() throws IOException {
        if (readPosition <= maxReadPosition) {
            int b = firstBytes[readPosition];
            readPosition++;
            return b;
        }
        return wrappedInputStream.read();
    }

    /**
     * Get the first bytes that have already been read
     */
    public int[] getFirstBytes() {
        return firstBytes.clone();
    }
}
