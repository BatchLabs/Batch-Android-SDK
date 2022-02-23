package com.batch.android.core;

import com.batch.android.json.JSONObject;

/**
 * Helper to read webservice responses
 *
 */
final class ResponseHelper {

    private static final String TAG = "ResponseHelper";

    /**
     * Get response content as Json
     *
     * @param content
     * @return
     */
    protected static JSONObject asJson(byte[] content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }

        try {
            return new JSONObject(asString(content));
        } catch (Exception e) {
            Logger.internal(TAG, "Error while casting response to json", e);
            return null;
        }
    }

    /**
     * Get response content as String
     *
     * @param content
     * @return
     */
    protected static String asString(byte[] content) {
        if (content == null) {
            throw new NullPointerException("Null content");
        }

        try {
            return ByteArrayHelper.getUTF8String(content);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while casting response to string", e);
            return null;
        }
    }
}
