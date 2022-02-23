package com.batch.android.query;

import android.content.Context;
import com.batch.android.core.Webservice;
import com.batch.android.event.Event;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * A query to track events
 *
 */
public class TrackingQuery extends Query {

    /**
     * Events to track
     */
    private List<Event> events;

    // ------------------------------------------>

    /**
     * @param context
     * @param events
     */
    public TrackingQuery(Context context, List<Event> events) {
        super(context, QueryType.TRACKING);
        if (events == null || events.isEmpty()) {
            throw new NullPointerException("Empty events");
        }

        this.events = new ArrayList<>(events);
    }

    // ------------------------------------------>

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject obj = super.toJSON();

        JSONObject eventsObj = new JSONObject();
        JSONArray newEventsArray = new JSONArray();
        JSONArray oldEventsArray = new JSONArray();

        for (Event event : events) {
            JSONObject eventObj = new JSONObject();

            Calendar cal = Calendar.getInstance(event.getTimezone());
            cal.setTime(event.getDate());

            eventObj.put("id", event.getId());
            eventObj.put("date", Webservice.formatDate(cal.getTime()));
            eventObj.put("name", event.getName());

            if (event.getSecureDate() != null) {
                eventObj.put("sDate", Webservice.formatDate(event.getSecureDate()));
            }

            String parameters = event.getParameters();
            eventObj.put("params", parameters == null ? JSONObject.NULL : new JSONObject(parameters));

            String sessionId = event.getSessionID();
            if (sessionId != null) {
                eventObj.put("session", sessionId);
            }

            if (event.isOld()) {
                oldEventsArray.put(eventObj);
            } else {
                newEventsArray.put(eventObj);
            }

            eventObj.put("ts", event.getServerTimestamp());
        }

        if (newEventsArray.length() > 0) {
            eventsObj.put("new", newEventsArray);
        }

        if (oldEventsArray.length() > 0) {
            eventsObj.put("old", oldEventsArray);
        }

        obj.put("evts", eventsObj);

        return obj;
    }
}
