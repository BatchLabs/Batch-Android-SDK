package com.batch.android.post;

import com.batch.android.core.Logger;

public abstract class MessagePackPostDataProvider<T> implements PostDataProvider<T> {

    private static final String TAG = "MessagePackPostDataProvider";

    abstract byte[] pack() throws Exception;

    @Override
    public byte[] getData() {
        try {
            return pack();
        } catch (Exception e) {
            Logger.internal(TAG, "Could not pack data", e);
            return new byte[0];
        }
    }

    @Override
    public String getContentType() {
        return "application/msgpack";
    }
}
