package com.batch.android.core;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.core.URLBuilder.CryptorMode;
import com.batch.android.core.Webservice.WebserviceError.Reason;
import com.batch.android.di.providers.OptOutModuleProvider;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.SecureDateProviderProvider;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.module.OptOutModule;
import com.batch.android.post.PostDataProvider;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

/**
 * Abstract webservice class designed for any WS call
 *
 */
public abstract class Webservice {

    private static final String TAG = "Webservice";

    /**
     * HTTP error code sent by the server if our cipher can't be read
     * Allow us to enable the fallback mode on the cipher
     */
    private static final int WEBSERVICE_ERROR_INVALID_CIPHER = 487;

    /**
     * Default retry-after delay (in seconds) when server is overloaded
     * and no header is provided.
     */
    private static final int DEFAULT_RETRY_AFTER = 60;

    /**
     * Debug interceptor. Allows the sample to tweak the SDK behaviour
     * Can be disabled using {@link Parameters#ENABLE_WS_INTERCEPTOR}
     */
    private static Interceptor wsInterceptor;

    // ------------------------------------------>

    /**
     * Unique request ID
     */
    private String id;

    /**
     * URLBuilder to build the Webservice url
     */
    private URLBuilder builder;
    /**
     * Headers of the request
     */
    protected Map<String, String> headers;

    /**
     * Context of the application
     */
    @NonNull
    protected Context applicationContext;

    /**
     * Type of the HTTP request
     */
    private RequestType type;
    /**
     * Is the webservice using a cipher v1 fallback
     */
    protected boolean isDowngradedCipher;

    /**
     * Opt Out module reference
     */
    private OptOutModule optOutModule = OptOutModuleProvider.get();

    // -------------------------------------------->

    /**
     * @param context
     * @param urlPattern
     * @throws MalformedURLException
     */
    protected Webservice(Context context, RequestType type, String urlPattern, String... parameters)
        throws MalformedURLException {
        if (context == null) {
            throw new NullPointerException("null context");
        }

        if (urlPattern == null) {
            throw new NullPointerException("null urlPattern");
        }

        if (type == null) {
            throw new NullPointerException("Null type");
        }

        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.applicationContext = context.getApplicationContext();
        this.builder = new URLBuilder(urlPattern, getGetCryptorMode(), parameters);
        this.headers = new HashMap<>();
        this.isDowngradedCipher =
            ParametersProvider.get(applicationContext).get(ParameterKeys.WS_CIPHERV2_LAST_FAILURE_KEY) != null;
    }

    // -------------------------------------------->

    /**
     * Add default parameters to the request
     * Those are device/user/system locale
     */
    protected void addDefaultParameters() {}

    /**
     * Add the parameters returned by {@link #getParameters()} to the URL if any
     */
    protected void addParameters() {
        Map<String, String> parameters = getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            for (String key : parameters.keySet()) {
                String value = parameters.get(key);

                if (key != null && value != null) {
                    builder.addGETParameter(key, value);
                }
            }
        }
    }

    /**
     * Add a specific GET parameter.
     *
     * @param key
     * @param value
     */
    protected void addGetParameter(String key, String value) {
        if (key == null || key.length() == 0) {
            return;
        }
        if (value == null || value.length() == 0) {
            return;
        }

        builder.addGETParameter(key, value);
    }

    /**
     * Prepend the API Key into the url parameters
     *
     * @param parameters
     * @return the same parameters with Batch key prepended
     */
    protected static String[] addBatchApiKey(String[] parameters) {
        final String[] retParams = new String[parameters.length + 1];
        retParams[0] = Batch.getAPIKey();
        System.arraycopy(parameters, 0, retParams, 1, parameters.length);
        return retParams;
    }

    /**
     * Return the specific GET parameters you want to add to the request<br>
     * You should override this method to provide custom get parameters
     *
     * @return Map of parameters if any, null if you don't want any
     */
    protected Map<String, String> getParameters() {
        return null;
    }

    /**
     * Returns whether this webservice is allowed to bypass the global opt out
     * <p>
     * Almost none of the webservice implementations should return true
     */
    protected boolean canBypassOptOut() {
        return false;
    }

    // -------------------------------------------->

    /**
     * Add default headers to the WS request
     */
    protected void addDefaultHeaders() {
        headers.put("Accept-Encoding", "gzip");
        headers.put("X-Batch-Nonce", this.id);

        String cipherVersion = "2";
        if (isDowngradedCipher) {
            headers.put("X-Batch-Cipher-Downgraded", "1");
            cipherVersion = "1";
        }

        // Add response body cipher version
        headers.put("X-Batch-Accept-Cipher", cipherVersion);
        if (type == RequestType.POST) {
            // Add request body cipher version
            headers.put("X-Batch-Content-Cipher", cipherVersion);
        }
    }

    /**
     * Add the headers returned by {@link #getHeaders()} if any
     */
    protected void addHeaders() {
        Map<String, String> headers = getHeaders();
        if (headers != null && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
    }

    /**
     * Return the specific header you want to add to the request<br>
     * You should override this method if you want to provide headers
     *
     * @return Map of headers if any, null if you don't want any
     */
    protected Map<String, String> getHeaders() {
        return null;
    }

    // -------------------------------------------->

    /**
     * Return the POST body you want to add to the request
     *
     * @return the post data provider if any, null otherwise
     */
    protected abstract PostDataProvider<?> getPostDataProvider();

    // -------------------------------------------->

    /**
     * Return the URLSorter to use.
     *
     * @return
     */
    private PatternURLSorter getURLSorter() {
        String parameterKey = getURLSorterPatternParameterKey();
        if (parameterKey == null) {
            return null;
        }

        String pattern = ParametersProvider.get(applicationContext).get(parameterKey);
        if (pattern == null) {
            return null;
        }

        return new PatternURLSorter(pattern);
    }

    /**
     * Return the parameter key to get the URL sorter pattern
     *
     * @return key of the parameter or null if no sortor is wanted
     */
    protected abstract String getURLSorterPatternParameterKey();

    // -------------------------------------------->

    /**
     * Return a cryptor for this URL.
     *
     * @return
     */
    private Cryptor getCryptor() {
        String cryptorTypeParamKey = getCryptorTypeParameterKey();
        if (cryptorTypeParamKey == null) {
            return null;
        }

        String cryptorType = ParametersProvider.get(applicationContext).get(cryptorTypeParamKey);
        if (cryptorType == null) {
            return null;
        }

        if (isDowngradedCipher) {
            return CryptorFactory.getCryptorForType(CryptorFactory.CryptorType.EAS_BASE64);
        }
        return CryptorFactory.getCryptorForTypeValue(Integer.parseInt(cryptorType));
    }

    /**
     * Return the type of the get cryptor wanted for this webservice
     *
     * @return type or null if no encryption is wanted
     */
    protected abstract String getCryptorTypeParameterKey();

    /**
     * Return the mode of the get cryptor
     *
     * @return Cryptor mode if found, fallback on ALL
     */
    private CryptorMode getGetCryptorMode() {
        String cryptorModeParamKey = getCryptorModeParameterKey();
        if (cryptorModeParamKey != null) {
            String cryptorMode = ParametersProvider.get(applicationContext).get(cryptorModeParamKey);
            if (cryptorMode != null) {
                try {
                    CryptorMode value = CryptorMode.fromValue(Integer.parseInt(cryptorMode));
                    if (value != null) {
                        return value;
                    }
                } catch (Exception e) {
                    Logger.internal(TAG, "Error while getting cryptor mode for key : " + cryptorModeParamKey, e);
                }
            }
        }

        return CryptorMode.ALL;
    }

    /**
     * Return the parameter key for the cryptor mode
     *
     * @return param key or null if no mode is provided ({@link CryptorMode#ALL} will be used)
     */
    protected abstract String getCryptorModeParameterKey();

    /**
     * Return a post cryptor for this webservice.
     *
     * @return
     */
    private WebserviceCryptor getPostCryptor() {
        String cryptorTypeParamKey = getPostCryptorTypeParameterKey();
        if (cryptorTypeParamKey == null) {
            return null;
        }

        String cryptorType = ParametersProvider.get(applicationContext).get(cryptorTypeParamKey);
        if (cryptorType == null) {
            return null;
        }

        if (isDowngradedCipher) {
            return new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);
        }
        return new WebserviceCryptor(Integer.parseInt(cryptorType));
    }

    /**
     * Return the type of the post cryptor wanted for this webservice
     *
     * @return type or null if no encryption is wanted
     */
    protected abstract String getPostCryptorTypeParameterKey();

    /**
     * Return a cryptor to read WS response
     *
     * @return
     */
    private WebserviceCryptor getReadCryptor() {
        String cryptorTypeParamKey = getReadCryptorTypeParameterKey();
        if (cryptorTypeParamKey == null) {
            return null;
        }

        String cryptorType = ParametersProvider.get(applicationContext).get(cryptorTypeParamKey);
        if (cryptorType == null) {
            return null;
        }

        if (isDowngradedCipher) {
            return new WebserviceCryptor(CryptorFactory.CryptorType.EAS_BASE64);
        }
        return new WebserviceCryptor(Integer.parseInt(cryptorType));
    }

    /**
     * Return the type of the read cryptor wanted for this webservice
     *
     * @return type or null if no encryption is wanted
     */
    protected abstract String getReadCryptorTypeParameterKey();

    // -------------------------------------------->

    /**
     * Execute the WS call synchronously and return the response
     *
     * @return byte[] on success, throws error on failure
     * @throws WebserviceError on error
     */
    public byte[] executeRequest() throws WebserviceError {
        if (!canBypassOptOut() && optOutModule.isOptedOutSync(applicationContext)) {
            throw new WebserviceError(Reason.SDK_OPTED_OUT);
        }

        HttpURLConnection connection = null;
        WebserviceError error = null;
        int responseCode = -1;

        /*
         * Execute request, with retry
         */
        int count = 0;
        do {
            // Handle retry
            if (count > 0) {
                sendRetrySignal(error);
            }

            InputStream in = null;
            ByteArrayOutputStream baos = null;
            try {
                try {
                    connection = buildConnection();
                    connection.connect();
                } catch (IOException ce) {
                    error = new WebserviceError(WebserviceError.Reason.NETWORK_ERROR, ce);
                    responseCode = -1;
                    count++;
                    continue;
                } catch (Exception e) {
                    throw new WebserviceError(WebserviceError.Reason.UNEXPECTED_ERROR, e);
                }

                try {
                    in = new BufferedInputStream(connection.getInputStream());
                } catch (SocketTimeoutException e) {
                    error = new WebserviceError(WebserviceError.Reason.NETWORK_ERROR, e);
                    responseCode = -1;
                    count++;
                    continue;
                } catch (IOException ioe) {
                    // Silently continue since error will be handled by isResponseValid();
                }

                responseCode = connection.getResponseCode();

                if (isResponseValid(responseCode)) {
                    // Treat GZIP stream.
                    String header = connection.getHeaderField("Content-Encoding");
                    if (header != null && header.equals("gzip")) {
                        in = new GZIPInputStream(in);
                    }

                    /*
                     * Read response as an array of byte
                     * @see http://www.gregbugaj.com/?p=283
                     */
                    baos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[8192];
                    int read = 0;
                    while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    baos.flush();

                    byte[] ba = baos.toByteArray();

                    // Decrypt if needed
                    WebserviceCryptor readCryptor = getReadCryptor();
                    if (readCryptor != null) {
                        ba = readCryptor.decryptData(ba, this, connection);
                        if (ba == null) { // Null = decrypt fail
                            enabledDowngradedMode();
                            throw new Exception("Unable to read encrypted data");
                        }
                    }

                    if (Parameters.ENABLE_WS_INTERCEPTOR && wsInterceptor != null) {
                        wsInterceptor.onSuccess(id, connection, ba, baos.size());
                    }

                    return ba;
                } else {
                    Reason reason = getResponseErrorCause(responseCode);
                    error = new WebserviceError(reason, new IOException("Response code : " + responseCode));
                    if (reason == Reason.TOO_MANY_REQUESTS) {
                        error.setRetryAfter(connection.getHeaderFieldInt("Retry-After", DEFAULT_RETRY_AFTER));
                    }
                    if (responseCode == WEBSERVICE_ERROR_INVALID_CIPHER) {
                        enabledDowngradedMode();
                    }
                }
            } catch (Exception e) {
                error = new WebserviceError(WebserviceError.Reason.UNEXPECTED_ERROR, e);
            } finally {
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (Exception ignored) {}
                }

                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {}
                }

                try {
                    connection.disconnect();
                } catch (Exception ignored) {}
            }

            if (Parameters.ENABLE_WS_INTERCEPTOR && wsInterceptor != null) {
                wsInterceptor.onError(id, connection, error);
            }

            count++;
        } while (count <= getMaxRetryCount() && shouldRetry(responseCode));

        throw error;
    }

    /**
     * Should we retry after last failure based on error code
     *
     * @param errorCode last http error code (default & network = -1)
     * @return
     */
    private boolean shouldRetry(int errorCode) {
        if (errorCode <= 0) {
            return true;
        }

        if (errorCode == 502 || errorCode == 504 || errorCode == 503 || errorCode == 499) {
            return true;
        }

        return false;
    }

    /**
     * Call the onRetry method with the right cause depending on the given error
     *
     * @param error
     */
    private void sendRetrySignal(WebserviceError error) {
        WebserviceErrorCause cause = WebserviceErrorCause.OTHER;

        if (error != null) {
            switch (error.reason) {
                case NETWORK_ERROR:
                    {
                        if (error.getCause() instanceof SocketTimeoutException) {
                            cause = WebserviceErrorCause.NETWORK_TIMEOUT;
                        } else if (error.getCause() instanceof SSLHandshakeException) {
                            cause = WebserviceErrorCause.SSL_HANDSHAKE_FAILURE;
                        } else {
                            cause = WebserviceErrorCause.OTHER;
                        }
                        break;
                    }
                case UNEXPECTED_ERROR:
                    cause = WebserviceErrorCause.PARSING_ERROR;
                    break;
                case SERVER_ERROR:
                    cause = WebserviceErrorCause.SERVER_ERROR;
                    break;
                default:
                    break;
            }
        }

        if (error != null) {
            Logger.internal(TAG, "Sending retry signal: " + cause, error);
        }

        onRetry(cause);
    }

    /**
     * Called when a retry is made after a failure<br>
     * Nothing done here, but child may want to override it
     *
     * @param cause
     */
    protected void onRetry(WebserviceErrorCause cause) {
        Logger.internal(TAG, "Retry webservice, cause : " + cause);
        // Nothing to do here but child may want to override it
    }

    /**
     * Return the JSON body if the response is valid
     * Note that this requires the body to be in the "standard" form, mostly used by queries
     * That is {"status": "OK", "header": ..., "ts": ..., "body": {...}}
     * For webservice clients that use a different format, please use {@link #getBasicJsonResponseBody}
     *
     * @return JSONBody if valid
     * @throws WebserviceError on error on the webservice
     * @throws JSONException   on error validating the JSON
     */
    protected JSONObject getStandardResponseBodyIfValid() throws WebserviceError, JSONException {
        byte[] resp = executeRequest();

        JSONObject response = ResponseHelper.asJson(resp);
        if (response == null) {
            throw new JSONException("Unable to parse the response as json");
        }

        if (!response.has("header") || response.isNull("header")) {
            throw new JSONException("Missing header");
        }

        if (!response.has("body")) {
            throw new JSONException("Missing body");
        }

        /*
         * Header
         */
        JSONObject header = response.getJSONObject("header");
        if (!header.has("status") || header.isNull("status")) {
            throw new JSONException("Missing header status");
        }

        /*
         * Status
         */
        String status = header.getString("status");
        if (!status.equals("OK")) {
            switch (status) {
                case "INVALID_APIKEY":
                    throw new WebserviceError(Reason.INVALID_API_KEY);
                case "DESACTIVATED_APIKEY":
                    throw new WebserviceError(Reason.DEACTIVATED_API_KEY);
                default:
                    throw new JSONException("Status not OK : " + status);
            }
        }

        /*
         * Server timestamp
         */
        if (header.has("ts") && !header.isNull("ts")) {
            long ts = header.getLong("ts");

            String currentTSString = ParametersProvider.get(applicationContext).get(ParameterKeys.SERVER_TIMESTAMP);
            if (currentTSString != null) { // If we already have a ts, be sure the new one is >
                long currentTS = Long.parseLong(currentTSString);
                if (currentTS < ts) {
                    ParametersProvider
                        .get(applicationContext)
                        .set(ParameterKeys.SERVER_TIMESTAMP, Long.toString(ts), true);
                } else {
                    ts = currentTS;
                }
            } else {
                ParametersProvider.get(applicationContext).set(ParameterKeys.SERVER_TIMESTAMP, Long.toString(ts), true);
            }

            SecureDateProviderProvider.get().initServerDate(new Date(ts));
        }

        return response.getJSONObject("body");
    }

    /**
     * Return the JSON body if the response is valid
     *
     * @return JSONBody if valid
     * @throws WebserviceError on error on the webservice
     * @throws JSONException   on error validating the JSON
     */
    protected JSONObject getBasicJsonResponseBody() throws WebserviceError, JSONException {
        byte[] resp = executeRequest();

        JSONObject response = ResponseHelper.asJson(resp);
        if (response == null) {
            throw new JSONException("Unable to parse the response as json");
        }

        return response;
    }

    /**
     * Tell if an http response code is valid or not
     *
     * @param statusCode
     * @return
     */
    public static boolean isResponseValid(int statusCode) {
        if (statusCode < 200 || statusCode > 399) {
            return false;
        }

        return true;
    }

    /**
     * Get error reason depending on the status code
     *
     * @param statusCode
     * @return
     */
    public static WebserviceError.Reason getResponseErrorCause(int statusCode) {
        if (isResponseValid(statusCode)) {
            return Reason.UNEXPECTED_ERROR;
        }

        if (statusCode == 429) {
            return Reason.TOO_MANY_REQUESTS;
        }

        if (statusCode == 404) {
            return Reason.NOT_FOUND_ERROR;
        }

        if (statusCode == 403) {
            return Reason.FORBIDDEN;
        }

        return Reason.SERVER_ERROR;
    }

    /**
     * URLEncode the part
     *
     * @param part
     * @return
     */
    public static String encode(String part) {
        try {
            return URLEncoder.encode(part, ByteArrayHelper.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not supported, can't build URL");
        }
    }

    /**
     * Build the url used by the connection with all parameters set
     *
     * @return
     */
    protected URL buildURL() {
        buildParameters();

        URL url = builder.build(getURLSorter(), getCryptor());

        if (Parameters.ENABLE_WS_INTERCEPTOR && wsInterceptor != null) {
            url = wsInterceptor.onBuildURL(url);
        }

        return url;
    }

    /**
     * Build a new and not already opened HttpURLConnection with all parameters set
     *
     * @return HttpURLConnection on success|null otherwise
     */
    protected HttpURLConnection buildConnection() throws Exception {
        /*
         * Create connection
         */
        HttpURLConnection connection = (HttpURLConnection) buildURL().openConnection();

        if (Parameters.ENABLE_WS_INTERCEPTOR && wsInterceptor != null) {
            connection = wsInterceptor.onBuildHttpConnection(connection);
        }

        if (connection instanceof HttpsURLConnection) {
            // If HTTPS is enable, we enforce the TLS versions
            ((HttpsURLConnection) connection).setSSLSocketFactory(new TLSSocketFactory());
        }

        /*
         * Add timeouts
         */
        connection.setConnectTimeout(getConnectTimeout());
        connection.setReadTimeout(getReadTimeout());

        /*
         * add headers
         */
        if (!headers.isEmpty()) {
            for (String key : headers.keySet()) {
                String value = headers.get(key);

                if (key != null && value != null) {
                    connection.setRequestProperty(key, value);
                }
            }
        }
        connection.setRequestProperty("Accept-Encoding", "gzip");

        /*
         * Post parameters
         * @see http://stackoverflow.com/questions/9767952/how-to-add-parameters-to-httpurlconnection-using-post
         */
        byte[] postContent = null;
        if (type == RequestType.POST) {
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            PostDataProvider<?> postProvider = getPostDataProvider();
            if (postProvider != null) {
                connection.setRequestProperty("Content-Type", postProvider.getContentType());
                postContent = buildPostParameters(postProvider);
            }
        }

        // Add signature and checksum headers
        addRequestSignatures(connection, postContent);

        if (Parameters.ENABLE_WS_INTERCEPTOR && wsInterceptor != null) {
            byte[] data = null;

            if (type == RequestType.POST) {
                PostDataProvider<?> provider = getPostDataProvider();
                if (provider != null) {
                    data = provider.getData();
                } else {
                    data = "{'error':'Internal interceptor error'}".getBytes("UTF-8");
                }
            }

            wsInterceptor.onPreConnect(id, connection, data, postContent != null ? postContent.length : 0);
        }

        /*
         * Once we write to the output stream, we cannot update anything in the connection
         */
        if (postContent != null) {
            try (DataOutputStream bos = new DataOutputStream(connection.getOutputStream())) {
                bos.write(postContent);
            }
        }
        return connection;
    }

    /**
     * Build parameters by executing all add methods
     */
    protected void buildParameters() {
        addDefaultParameters();
        addParameters();

        addDefaultHeaders();
        addHeaders();
    }

    /**
     * Build the request body from the json object
     *
     * @param provider
     * @return
     */
    private byte[] buildPostParameters(PostDataProvider<?> provider) throws Exception {
        byte[] data = provider.getData();

        /*
         * Encrypt if needed
         */
        WebserviceCryptor cryptor = getPostCryptor();
        if (cryptor != null) {
            return cryptor.encryptData(data, this);
        }

        return data;
    }

    private void enabledDowngradedMode() {
        String now = Long.toString(System.currentTimeMillis());
        ParametersProvider.get(applicationContext).set(ParameterKeys.WS_CIPHERV2_LAST_FAILURE_KEY, now, true);
    }

    protected void addRequestSignatures(HttpURLConnection connection, @Nullable byte[] body) {
        if (body != null) {
            String contentSha1 = ByteArrayHelper.SHA1Base64Encoded(body);
            connection.setRequestProperty("Content-SHA1", contentSha1);
        }

        // Get the list of keys and sort it
        List<String> keys = new ArrayList<>(connection.getRequestProperties().keySet());
        Collections.sort(keys);

        String signatureBody = getSignatureBody(connection, keys);
        String signature = WebserviceSignature.encryptSignatureData(signatureBody);

        StringBuilder signatureHeader = new StringBuilder();
        signatureHeader.append("SHA256");

        boolean first = true;
        for (String key : keys) {
            if (!first) {
                signatureHeader.append(',');
            } else {
                signatureHeader.append(' ');
            }
            signatureHeader.append(key.toLowerCase(Locale.US));
            first = false;
        }

        signatureHeader.append(' ');
        signatureHeader.append(signature);
        connection.setRequestProperty("X-Batch-Signature", signatureHeader.toString());
    }

    protected String getSignatureBody(HttpURLConnection connection, List<String> keys) {
        StringBuilder signature = new StringBuilder();
        signature.append(connection.getRequestMethod().toUpperCase(Locale.US));
        signature.append(' ');

        String path = connection.getURL().getPath();
        if (TextUtils.isEmpty(path)) {
            signature.append("/");
        } else {
            signature.append(path);
        }

        String query = connection.getURL().getQuery();
        if (query != null) {
            signature.append('?');
            signature.append(query);
        }

        ListIterator<String> iterator = keys.listIterator();
        while (iterator.hasNext()) {
            String propertyKey = iterator.next();
            String value = connection.getRequestProperty(propertyKey);
            if (!TextUtils.isEmpty(value)) {
                signature.append('\n');

                signature.append(propertyKey.toLowerCase(Locale.US));
                signature.append(": ");
                signature.append(connection.getRequestProperty(propertyKey));
            } else {
                // If an property is null or empty, we don't include it at all and remove the key from the list
                iterator.remove();
            }
        }

        return signature.toString();
    }

    // --------------------------------------------->

    /**
     * Return the timeout for connect<br>
     * Looking first at specific parameter and fallback on default
     *
     * @return
     */
    private int getConnectTimeout() {
        String key = getSpecificConnectTimeoutKey();
        if (key != null) {
            String value = ParametersProvider.get(applicationContext).get(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        }

        return Integer.parseInt(
            ParametersProvider.get(applicationContext).get(ParameterKeys.DEFAULT_CONNECT_TIMEOUT_KEY)
        );
    }

    /**
     * Return the {@link Parameters} key to search specific connect timeout for this webservice
     *
     * @return key if we need specific value, null otherwise
     */
    protected abstract String getSpecificConnectTimeoutKey();

    /**
     * Return the timeout for read<br>
     * Looking first at specific parameter and fallback on default
     *
     * @return
     */
    private int getReadTimeout() {
        String key = getSpecificReadTimeoutKey();
        if (key != null) {
            String value = ParametersProvider.get(applicationContext).get(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        }

        return Integer.parseInt(ParametersProvider.get(applicationContext).get(ParameterKeys.DEFAULT_READ_TIMEOUT_KEY));
    }

    /**
     * Return the {@link Parameters} key to search specific read timeout for this webservice
     *
     * @return key if we need specific value, null otherwise
     */
    protected abstract String getSpecificReadTimeoutKey();

    /**
     * Return the max number of time we should retry in fail cases<br>
     * Looking first at specific parameter and fallback on default
     *
     * @return
     */
    private int getMaxRetryCount() {
        String key = getSpecificRetryCountKey();
        if (key != null) {
            String value = ParametersProvider.get(applicationContext).get(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        }

        return Integer.parseInt(ParametersProvider.get(applicationContext).get(ParameterKeys.DEFAULT_RETRY_NUMBER_KEY));
    }

    /**
     * Return the {@link Parameters} key to search specific number of retry for this webservice
     *
     * @return key if we need specific value, null otherwise
     */
    protected abstract String getSpecificRetryCountKey();

    // --------------------------------------------->

    /**
     * Type of the HTTP Request
     *
     */
    protected enum RequestType {
        /**
         * Webservice will perform a GET request
         */
        GET,

        /**
         * Webservice will perform a POST request
         */
        POST,
    }

    /**
     * Error thrown by Webservice on error
     *
     */
    public static class WebserviceError extends Throwable {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /**
         * The reason of the error
         */
        private Reason reason;

        /**
         * Number of seconds we have to wait before sending another request to a webservice who failed.
         *
         * Server can respond with an HTTP status code 429 ({@link Reason#TOO_MANY_REQUESTS})
         * and specify the time we have to wait with a 'Retry-After' header.
         */
        private int retryAfter = 0;

        // ------------------------------------------>

        /**
         * @param reason
         * @param cause
         */
        protected WebserviceError(Reason reason, Throwable cause) {
            super(cause);
            if (reason == null) {
                throw new NullPointerException("Null reason");
            }

            this.reason = reason;
        }

        /**
         * @param reason
         */
        protected WebserviceError(Reason reason) {
            super("");
            if (reason == null) {
                throw new NullPointerException("Null reason");
            }

            this.reason = reason;
        }

        // ------------------------------------------>

        /**
         * Return the reason of the error
         *
         * @return
         */
        public Reason getReason() {
            return reason;
        }

        // ------------------------------------------>

        /**
         * A possible reason of error
         *
         */
        public enum Reason {
            /**
             * Network is unavailable
             */
            NETWORK_ERROR,

            /**
             * Server error (500)
             */
            SERVER_ERROR,

            /**
             * Server overloaded (429)
             */
            TOO_MANY_REQUESTS,

            /**
             * Server returns a not found status (404)
             */
            NOT_FOUND_ERROR,

            /**
             * Server return an invalid API key error
             */
            INVALID_API_KEY,

            /**
             * Server return a deactivated API key error
             */
            DEACTIVATED_API_KEY,

            /**
             * Runtime & unexpected error
             */
            UNEXPECTED_ERROR,

            /**
             * Auth problem (HTTP 403)
             */
            FORBIDDEN,

            /**
             * Batch has globally been opted out from: network calls are not allowed
             */
            SDK_OPTED_OUT,
        }

        /**
         * Get the time to wait before sending another request
         * @return retryAfter (in milliseconds)
         */
        public int getRetryAfterInMillis() {
            return retryAfter * 1000;
        }

        /**
         * Set the time (in seconds) to wait before sending another request
         */
        public void setRetryAfter(int retryAfter) {
            this.retryAfter = retryAfter;
        }
    }

    // -------------------------------------------------->

    /**
     * Return the date formatted with RFC 3339 format
     *
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(date);
    }

    // -------------------------------------------------->

    interface Interceptor {
        HttpURLConnection onBuildHttpConnection(HttpURLConnection originalConnection);

        URL onBuildURL(URL originalURL);

        void onPreConnect(String id, HttpURLConnection connection, byte[] data, long compressedSize);

        void onSuccess(String id, HttpURLConnection connection, byte[] data, long compressedSize);

        void onError(String id, HttpURLConnection connection, WebserviceError error);
    }

    static void setWsInterceptor(Interceptor i) {
        if (Parameters.ENABLE_WS_INTERCEPTOR) {
            wsInterceptor = i;
        }
    }
}
