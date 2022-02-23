package com.batch.android.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * URL builder that automaticaly manage GET parameters
 *
 */
public final class URLBuilder {

    private static final String TAG = "URLBuilder";

    /**
     * Base url of the query without parameters (obtained by parsing the given URL)
     */
    private String baseURL;

    /**
     * GET Parameters of the URL
     */
    private Map<String, String> getParameters;

    /**
     * Mode of the url cryptor
     */
    private CryptorMode cryptorMode;

    // ------------------------------------------->

    /**
     * Initiate a builder and parse the given string to extract any parameters
     *
     * @param urlPattern
     * @param mode
     * @param parameters
     * @throws MalformedURLException
     */
    public URLBuilder(String urlPattern, CryptorMode mode, String[] parameters) throws MalformedURLException {
        if (urlPattern == null || urlPattern.length() == 0) {
            throw new NullPointerException("Null urlString");
        }

        this.getParameters = new HashMap<>();
        this.parseURL(urlPattern, parameters);
        this.cryptorMode = mode;
    }

    // ------------------------------------------->

    /**
     * Parse the url and extract parameters if any
     *
     * @param urlPattern
     * @param parameters
     * @throws MalformedURLException
     */
    private void parseURL(String urlPattern, String[] parameters) throws MalformedURLException {
        URL url = null;
        String urlString = null;
        try {
            /*
             * Apply parameters if needed
             */
            urlString = urlPattern;
            if (parameters != null && parameters.length > 0) {
                // Apply url encoding on each parameter
                List<String> encodedParameters = new ArrayList<>(parameters.length);
                for (String parameter : parameters) {
                    if (parameter == null) {
                        encodedParameters.add("null"); // This is ugly but it's what it will look like after String.format applied
                        continue; // And it's not supposed to happen...
                    }

                    encodedParameters.add(URLEncoder.encode(parameter, ByteArrayHelper.UTF_8));
                }

                urlString = String.format(urlPattern, encodedParameters.toArray());
            }

            /*
             * Build URL object
             */
            url = new URL(urlString);
        } catch (Exception e) {
            Logger.internal(TAG, "MalformedURLException source: ", e);
            throw new MalformedURLException("Error while initializing URL with pattern : " + urlString);
        }

        String query = url.getQuery();
        if (query != null && query.length() > 0) {
            try {
                baseURL = urlString.substring(0, urlString.indexOf("?"));
                getParameters.putAll(parseQuery(query));
            } catch (Exception e) {
                throw new MalformedURLException(e.getMessage());
            }
        } else {
            baseURL = urlString;
        }
    }

    /**
     * Parse GET parameters string
     *
     * @param query
     * @return
     */
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();

        String[] paramsString = query.split("&");
        for (String paramString : paramsString) {
            String[] paramValue = paramString.split("=");
            if (paramValue.length != 2) {
                continue;
            }

            params.put(paramValue[0], paramValue[1]);
        }

        return params;
    }

    // ------------------------------------------->

    /**
     * Add a GET parameter to the query, if this parameter is already set, it will be overridden
     * This method do not URL encode the key or value !
     *
     * @param key
     * @param value
     */
    public void addGETParameter(String key, String value) {
        if (key == null || key.length() == 0) {
            throw new NullPointerException("Null key");
        }

        if (value == null || value.length() == 0) {
            throw new NullPointerException("Null value");
        }

        getParameters.put(key, value);
    }

    /**
     * Remove the GET parameter with the given key
     *
     * @param key
     */
    public void removeGETParameter(String key) {
        if (key == null) {
            throw new NullPointerException("Null key");
        }

        getParameters.remove(key);
    }

    // ------------------------------------------->

    /**
     * Build the query to an URL object
     *
     * @return
     */
    public URL build() {
        return build(null, null);
    }

    /**
     * Build the query according to a sorter.
     *
     * @param sorter
     * @return
     */
    public URL build(PatternURLSorter sorter, Cryptor cryptor) {
        if (sorter == null) {
            sorter = new PatternURLSorter();
        }

        String query = buildQuery(sorter, cryptor);

        try {
            if (query == null) {
                return new URL(baseURL);
            } else {
                return new URL(String.format("%s?%s", baseURL, query));
            }
        } catch (MalformedURLException e) { // Must never happend since we test it earlier
            Logger.internal(TAG, "Error while building URL", e);
            return null;
        }
    }

    /**
     * Build the GET parameters query
     *
     * @param sorter
     * @return string if any parameters, null otherwise
     */
    private String buildQuery(PatternURLSorter sorter, Cryptor cryptor) {
        if (sorter == null) {
            throw new NullPointerException("null sorter");
        }

        if (getParameters.isEmpty()) {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        // If no cryptor, just build the query
        if (cryptor == null) {
            buildRawQuery(sorter, builder);

            return builder.toString();
        } else {
            switch (cryptorMode) {
                // Build raw query and put crypted value into a param named "o"
                case ALL:
                    {
                        buildRawQuery(sorter, builder);

                        return "o=" + cryptor.encrypt(builder.toString());
                    }
                // Build query by encoding only values
                case VALUE:
                    {
                        for (String key : sorter.getKeysOrdered(getParameters)) {
                            addParameter(builder, key, cryptor.encrypt(getParameters.get(key)));
                        }

                        cleanURL(builder);

                        return builder.toString();
                    }
                // Build query by encoding keys & values
                case EACH:
                    {
                        for (String key : sorter.getKeysOrdered(getParameters)) {
                            addParameter(builder, cryptor.encrypt(key), cryptor.encrypt(getParameters.get(key)));
                        }

                        cleanURL(builder);

                        return builder.toString();
                    }
                default:
                    {
                        buildRawQuery(sorter, builder);
                        return builder.toString();
                    }
            }
        }
    }

    /**
     * Build the raw query URL style<br>
     * Ex : key1=value1&key2=value2
     *
     * @param sorter
     * @param builder
     */
    private void buildRawQuery(PatternURLSorter sorter, StringBuilder builder) {
        for (String key : sorter.getKeysOrdered(getParameters)) {
            addParameter(builder, key, getParameters.get(key));
        }

        cleanURL(builder);
    }

    /**
     * Add a parameter and a separator to the builder
     *
     * @param builder
     * @param key
     * @param value
     */
    private void addParameter(StringBuilder builder, String key, String value) {
        builder.append(String.format("%s=%s&", key, value));
    }

    /**
     * Clean the builder by removing the last separator
     *
     * @param builder
     */
    private void cleanURL(StringBuilder builder) {
        builder.deleteCharAt(builder.length() - 1); // Remove last & at the end of the query
    }

    // --------------------------------------->

    /**
     * Mode to crypt get parameters
     *
     */
    public enum CryptorMode {
        /**
         * Crypt everything into a parameter named o
         */
        ALL(2),

        /**
         * Crypt only values of parameters
         */
        VALUE(0),

        /**
         * Crypt values & key of parameters
         */
        EACH(1);

        // ----------------------------------------->

        /**
         * Value used to be set by start WS
         */
        private int value;

        /**
         * @param value
         */
        CryptorMode(int value) {
            this.value = value;
        }

        /**
         * Return the value of this mode
         *
         * @return
         */
        public int getValue() {
            return value;
        }

        /**
         * Found the mode for the given value
         *
         * @param value
         * @return the mode if found, null otherwise
         */
        public static CryptorMode fromValue(int value) {
            for (CryptorMode mode : values()) {
                if (mode.getValue() == value) {
                    return mode;
                }
            }

            return null;
        }
    }
}
