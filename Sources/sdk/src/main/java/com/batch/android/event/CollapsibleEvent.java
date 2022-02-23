package com.batch.android.event;

import android.content.Context;
import com.batch.android.json.JSONObject;
import java.util.Date;
import java.util.TimeZone;

/**
 * A collapsible event is an event that only keeps the last occurrence in the database
 */

public class CollapsibleEvent extends Event {

    public CollapsibleEvent(Context context, long timestamp, String name, JSONObject parameters) {
        super(context, timestamp, name, parameters);
    }

    public CollapsibleEvent(
        String id,
        String name,
        Date date,
        TimeZone timezone,
        String parameters,
        State state,
        Long serverTS,
        Date secureDate,
        String sessionId
    ) {
        super(id, name, date, timezone, parameters, state, serverTS, secureDate, sessionId);
    }
}
