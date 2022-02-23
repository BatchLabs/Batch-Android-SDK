package com.batch.android;

import android.content.Context;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * A mock Batch Webservice to test json validation
 *
 */
public class MockBatchWebservice extends BatchWebservice {

    /**
     * Response state to test
     *
     */
    public enum State {
        /**
         * The response is complete
         */
        COMPLETE,

        /**
         * The header is missing
         */
        MISSING_HEADER,

        /**
         * The header status is invalid
         */
        INVALID_HEADER,

        /**
         * The body is missing
         */
        MISSING_BODY,
    }

    // ---------------------------------------------------->

    /**
     * State of this Mock WS
     */
    private State state;

    // ---------------------------------------------------->

    /**
     * @param context
     * @param state
     * @throws MalformedURLException
     */
    protected MockBatchWebservice(Context context, State state) throws MalformedURLException {
        super(context, RequestType.POST, "http://mock.com/");
        this.state = state;
    }

    // ---------------------------------------------------->

    /**
     * Return a response depending on the {@link #state}
     */
    @Override
    public byte[] executeRequest() {
        switch (state) {
            case COMPLETE:
                return "{\"header\":{ \"version\":\"1.0\",\"status\":\"OK\"},\"body\":{}}".getBytes();
            case MISSING_HEADER:
                return "{\"body\":{}}".getBytes();
            case INVALID_HEADER:
                return "{\"header\":{ \"version\":\"1.0\",\"status\":\"ERROR\"},\"body\":{}}".getBytes();
            case MISSING_BODY:
                return "{\"header\":{ \"version\":\"1.0\",\"status\":\"OK\"}}".getBytes();
            default:
                return null;
        }
    }

    // ----------------------------------------->

    /**
     * Public accessor to this protected method
     *
     * @return {@link #getStandardResponseBodyIfValid()}
     */
    public JSONObject getBodyIsValid() throws JSONException, WebserviceError {
        return getStandardResponseBodyIfValid();
    }

    /**
     * Return post parameters automaticaly provided
     *
     * @return
     */
    public JSONObject getDefaultPostParameters() {
        return getPostDataProvider().getRawData();
    }

    /**
     * Return headers automaticaly provided
     *
     * @return
     */
    public Map<String, String> getDefaultHeaders() {
        super.addDefaultHeaders();
        return headers;
    }

    // ---------------------------------------->

    public static WebserviceError.Reason getNetworkErrorReason() {
        return WebserviceError.Reason.NETWORK_ERROR;
    }

    public static WebserviceError.Reason get500ErrorReason() {
        return WebserviceError.Reason.SERVER_ERROR;
    }

    public static WebserviceError.Reason get404ErrorReason() {
        return WebserviceError.Reason.NOT_FOUND_ERROR;
    }

    public static WebserviceError.Reason getUnexpectedErrorReason() {
        return WebserviceError.Reason.UNEXPECTED_ERROR;
    }

    // ----------------------------------------->

    @Override
    protected String getPropertyParameterKey() {
        return null;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return null;
    }

    @Override
    protected String getCryptorTypeParameterKey() {
        return null;
    }

    @Override
    protected String getCryptorModeParameterKey() {
        return null;
    }

    @Override
    protected String getPostCryptorTypeParameterKey() {
        return null;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return null;
    }

    @Override
    protected String getReadCryptorTypeParameterKey() {
        return null;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return null;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return null;
    }
}
