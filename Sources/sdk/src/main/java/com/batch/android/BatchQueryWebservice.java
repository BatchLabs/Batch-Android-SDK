package com.batch.android;

import android.content.Context;
import com.batch.android.core.Logger;
import com.batch.android.di.providers.WebserviceMetricsProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.post.JSONPostDataProvider;
import com.batch.android.post.PostDataProvider;
import com.batch.android.query.Query;
import com.batch.android.query.QueryType;
import com.batch.android.query.response.Response;
import com.batch.android.query.serialization.deserializers.AttributesCheckResponseDeserializer;
import com.batch.android.query.serialization.deserializers.AttributesSendResponseDeserializer;
import com.batch.android.query.serialization.deserializers.LocalCampaignsResponseDeserializer;
import com.batch.android.query.serialization.deserializers.PushResponseDeserializer;
import com.batch.android.query.serialization.deserializers.ResponseDeserializer;
import com.batch.android.query.serialization.deserializers.StartResponseDeserializer;
import com.batch.android.query.serialization.deserializers.TrackingResponseDeserializer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract webservice to manage queries
 *
 * @hide
 */
abstract class BatchQueryWebservice extends BatchWebservice {

    private static final String TAG = "BatchQueryWebservice";

    /**
     * List of queries send to the webservices
     */
    private List<Query> queries;
    /**
     * List of responses given by the webservice (is null if {@link #parseResponse(JSONObject)}) has not been called
     */
    private List<Response> responses;

    protected WebserviceMetrics webserviceMetrics;

    // ------------------------------------------->

    /**
     * @param context
     * @param type
     * @param baseURLFormat
     * @param parameters
     * @throws MalformedURLException
     */
    protected BatchQueryWebservice(Context context, RequestType type, String baseURLFormat, String... parameters)
        throws MalformedURLException {
        super(context, type, baseURLFormat, parameters);
        this.webserviceMetrics = WebserviceMetricsProvider.get();
    }

    // ------------------------------------------->

    @Override
    protected PostDataProvider<JSONObject> getPostDataProvider() {
        JSONObject body = null;

        /*
         * retrieve body of parent or create a new one if necessary
         */
        PostDataProvider<JSONObject> superPostDataProvider = super.getPostDataProvider();
        if (superPostDataProvider == null) {
            body = new JSONObject();
        } else {
            body = superPostDataProvider.getRawData();
        }

        /*
         * Get queries from children (only if not already done, otherwise it will reset the retry count)
         */
        if (queries == null) {
            queries = getQueries();
            if (queries == null || queries.isEmpty()) {
                throw new IllegalStateException("Cannot create a WS without any query");
            }
        }

        /*
         * Put queries in body
         */
        try {
            JSONArray queries = new JSONArray();

            for (Query query : this.queries) {
                queries.put(query.toJSON());
            }

            body.put("queries", queries);
        } catch (Exception e) {
            Logger.internal(TAG, "Error while adding queries to WS body", e);
        }

        return new JSONPostDataProvider(body);
    }

    // ------------------------------------------->

    /**
     * Get the queries to execute
     *
     * @return a list of queries, will throw an exception if null or empty
     */
    protected abstract List<Query> getQueries();

    /**
     * Parse the given response to handle parameters, i and queries
     *
     * @param jsonResponse
     * @throws JSONException
     * @throws IllegalStateException
     */
    protected void parseResponse(JSONObject jsonResponse) throws JSONException, IllegalStateException {
        /*
         * Register parameters if any
         */
        handleParameters(jsonResponse);

        /*
         * Register server id (i)
         */
        handleServerID(jsonResponse);

        /*
         * Parse response to retrieve responses for queries
         */
        parseQueries(jsonResponse);
    }

    /**
     * Parse queries and retrieve responses
     *
     * @param jsonResponse
     * @throws JSONException         if "queries" tag is null
     * @throws IllegalStateException if number of responses and queries mismatch or if we cannot retrieve query for a response
     */
    private void parseQueries(JSONObject jsonResponse) throws JSONException, IllegalStateException {
        if (!jsonResponse.has("queries") || jsonResponse.isNull("queries")) {
            throw new JSONException("Missing queries attribute in response");
        }

        JSONArray responses = jsonResponse.getJSONArray("queries");
        if (responses.length() != queries.size()) {
            throw new IllegalStateException(
                "Number of queries and responses mismatch(" +
                queries.size() +
                " queries / " +
                responses.length() +
                " responses)"
            );
        }

        // Create responses array
        this.responses = new ArrayList<>(responses.length());

        /*
         * Parse responses
         */
        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.getJSONObject(i);

            /*
             * retrieve query for this response
             */
            String queryID = response.getString("id");
            Query query = getQueryForID(queryID);
            if (query == null) {
                throw new IllegalStateException("Unable to find query with ID " + queryID);
            }

            /*
             * Instantiate the right deserializer according to the query type
             */
            ResponseDeserializer responseDeserializer = null;
            switch (query.getType()) {
                case START:
                    responseDeserializer = new StartResponseDeserializer(response);
                    break;
                case TRACKING:
                    responseDeserializer = new TrackingResponseDeserializer(response);
                    break;
                case PUSH:
                    responseDeserializer = new PushResponseDeserializer(response);
                    break;
                case ATTRIBUTES:
                    responseDeserializer = new AttributesSendResponseDeserializer(response);
                    break;
                case ATTRIBUTES_CHECK:
                    responseDeserializer = new AttributesCheckResponseDeserializer(response);
                    break;
                case LOCAL_CAMPAIGNS:
                    responseDeserializer = new LocalCampaignsResponseDeserializer(response);
                    break;
            }
            // Build the response
            Response resp = responseDeserializer.deserialize();
            this.responses.add(resp);
        }
    }

    /**
     * retrieve the response for the given type and params
     *
     * @param clazz the output class (must be correct, exception thrown otherwise)
     * @param type  type of response wanted
     * @return a response if found, null otherwise
     * @throws ClassCastException if the given clazz is not of the right type
     */
    @SuppressWarnings("unchecked")
    protected <T extends Response> T getResponseFor(Class<T> clazz, QueryType type) throws ClassCastException {
        if (responses == null) {
            throw new IllegalStateException("You forgot to call parseResponse method");
        }

        Response response = getResponseForType(type);
        if (response == null) {
            return null;
        }

        return (T) response;
    }

    // ------------------------------------------->

    /**
     * retrieve the response for the given query type<br>
     *
     * @param type
     * @return response if found, null otherwise
     */
    private Response getResponseForType(QueryType type) {
        for (Response response : responses) {
            if (response.getQueryType() == type) {
                return response;
            }
        }

        return null;
    }

    /**
     * retrieve the query for the given ID
     *
     * @param queryID
     * @return the query if found, null otherwise
     */
    private Query getQueryForID(String queryID) {
        for (Query query : queries) {
            if (query.getID().equals(queryID)) {
                return query;
            }
        }

        return null;
    }
}
