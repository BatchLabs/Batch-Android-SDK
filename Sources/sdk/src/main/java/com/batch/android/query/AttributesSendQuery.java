package com.batch.android.query;

import android.content.Context;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.Map;
import java.util.Set;

/**
 * Query to send attributes to server
 *
 */
public class AttributesSendQuery extends Query {

    /**
     * Attributes version
     */
    private long version;

    /**
     * Attributes
     */
    private Map<String, Object> attributes;

    /**
     * Tags
     */
    private Map<String, Set<String>> tags;

    // -------------------------------------------->

    public AttributesSendQuery(
        Context context,
        long version,
        Map<String, Object> attributes,
        Map<String, Set<String>> tags
    ) {
        super(context, QueryType.ATTRIBUTES);
        if (version <= 0) {
            throw new IllegalArgumentException("version <= 0");
        }

        if (attributes == null) {
            throw new IllegalArgumentException("attributes==null");
        }

        if (tags == null) {
            throw new IllegalArgumentException("tags==null");
        }

        this.version = version;
        this.attributes = attributes;
        this.tags = tags;
    }

    // -------------------------------------------->

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = super.toJSON();

        obj.put("ver", version);
        obj.put("tags", new JSONObject(tags));
        obj.put("attrs", new JSONObject(attributes));

        return obj;
    }
}
