package com.batch.android.post;

import com.batch.android.core.ByteArrayHelper;
import com.batch.android.core.Webservice;
import java.util.HashMap;
import java.util.Map;

/**
 * A parameters post data provider
 *
 */
public class ParametersPostDataProvider implements PostDataProvider<Map<String, String>> {

    /**
     * Map of parameters with their values
     */
    private Map<String, String> params = new HashMap<>();

    // --------------------------------------------->

    /**
     * Create a provider with empty parameters
     */
    public ParametersPostDataProvider() {
        this(new HashMap<>(0));
    }

    /**
     * Create a provider with given parameters
     *
     * @param params
     */
    public ParametersPostDataProvider(Map<String, String> params) {
        if (params == null) {
            throw new NullPointerException("Null params");
        }

        this.params.putAll(params);
    }

    // --------------------------------------------->

    @Override
    public Map<String, String> getRawData() {
        return params;
    }

    @Override
    public byte[] getData() {
        /*
         * Transform the map into an parameters url
         */
        StringBuilder result = new StringBuilder();

        boolean first = true;
        for (String key : params.keySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(Webservice.encode(key));
            result.append("=");
            result.append(Webservice.encode(params.get(key)));
        }

        /*
         * Return bytes of this url
         */
        return ByteArrayHelper.getUTF8Bytes(result.toString());
    }

    @Override
    public String getContentType() {
        return "application/x-www-form-urlencoded";
    }

    @Override
    public boolean isEmpty() {
        return params.isEmpty();
    }
}
