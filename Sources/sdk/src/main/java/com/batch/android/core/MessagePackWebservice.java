package com.batch.android.core;

import android.content.Context;
import com.batch.android.post.MessagePackPostDataProvider;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public abstract class MessagePackWebservice extends Webservice implements TaskRunnable {

    private static final String MSGPACK_SCHEMA_VERSION = "1.0.0";

    private final MessagePackPostDataProvider<?> dataProvider;

    protected MessagePackWebservice(
        Context context,
        MessagePackPostDataProvider<?> dataProvider,
        String urlPattern,
        String... parameters
    ) throws MalformedURLException {
        super(context, RequestType.POST, urlPattern, parameters);
        if (dataProvider == null || dataProvider.isEmpty()) {
            throw new NullPointerException("Provider is empty");
        }
        this.dataProvider = dataProvider;
    }

    /**
     * Prepend the schema version into the url parameters
     *
     * Only used for display receipt
     * @param parameters parameters
     * @return parameters
     */
    protected static String[] addSchemaVersion(String[] parameters) {
        final String[] retParams = new String[parameters.length + 1];
        retParams[0] = MSGPACK_SCHEMA_VERSION;
        System.arraycopy(parameters, 0, retParams, 1, parameters.length);
        return retParams;
    }

    @Override
    protected Map<String, String> getHeaders() {
        HashMap<String, String> header = new HashMap<>();
        header.put("x-batch-protocol-version", MSGPACK_SCHEMA_VERSION);
        header.put("x-batch-sdk-version", Parameters.SDK_VERSION);
        return header;
    }

    @Override
    protected MessagePackPostDataProvider<?> getPostDataProvider() {
        return dataProvider;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return ParameterKeys.MESSAGE_PACK_WS_POST_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.MESSAGE_PACK_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.MESSAGE_PACK_WS_URLSORTER_PATTERN_KEY;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return ParameterKeys.MESSAGE_PACK_WS_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return ParameterKeys.MESSAGE_PACK_WS_CRYPTORMODE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.MESSAGE_PACK_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.MESSAGE_PACK_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.MESSAGE_PACK_WS_RETRYCOUNT_KEY;
    }
}
