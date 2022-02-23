package com.batch.android.query;

import android.content.Context;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.UUID;

/**
 * Mother of all queries, containing id type and context
 *
 */
public abstract class Query {

    /**
     * App context
     */
    private Context context;
    /**
     * ID of the query
     */
    private String id;
    /**
     * Type of the query
     */
    private QueryType type;

    // ----------------------------------------->

    /**
     * @param context
     * @param type
     */
    public Query(Context context, QueryType type) {
        if (context == null) {
            throw new NullPointerException("context==null");
        }

        if (type == null) {
            throw new NullPointerException("type==null");
        }

        this.context = context.getApplicationContext();
        this.id = generateID();
        this.type = type;
    }

    // ----------------------------------------->

    /**
     * Get the ID of the query
     *
     * @return
     */
    public String getID() {
        return id;
    }

    /**
     * Get the type of the query
     *
     * @return
     */
    public QueryType getType() {
        return type;
    }

    /**
     * Get the saved application context
     *
     * @return
     */
    protected Context getContext() {
        return context;
    }

    // ------------------------------------------>

    /**
     * Serialize this query to json<br>
     * Child would typicaly extends this method to provide extra data
     *
     * @return
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = new JSONObject();

        obj.put("id", id);
        obj.put("type", type.toString());

        return obj;
    }

    // ----------------------------------------->

    /**
     * Generate a unique identifier for this query
     *
     * @return
     */
    private static String generateID() {
        return UUID.randomUUID().toString();
    }
}
