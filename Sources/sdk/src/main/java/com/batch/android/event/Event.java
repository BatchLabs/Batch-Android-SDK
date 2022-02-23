package com.batch.android.event;

import android.content.Context;
import com.batch.android.Batch;
import com.batch.android.core.ParameterKeys;
import com.batch.android.di.providers.ParametersProvider;
import com.batch.android.di.providers.SecureDateProviderProvider;
import com.batch.android.json.JSONObject;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Event model. Immutable.
 *
 */
public class Event {

    /**
     * Event unique ID
     */
    private final String id;

    /**
     * Event name
     */
    private final String name;

    /**
     * Date the event occured
     */
    private final Date date;

    /**
     * System timezone when the event occured
     */
    private final TimeZone timezone;

    /**
     * Last server time
     */
    private final long servertime;

    /**
     * Secure date, can be null
     */
    private final Date secureDate;

    /**
     * Additional event parameters. Can be null.
     */
    private final String parameters;

    /**
     * State of the event
     */
    private final State state;

    /**
     * Session of the event
     */
    private final String session;

    // -------------------------------------------->

    /**
     * @param context
     * @param timestamp
     * @param name
     * @param parameters
     */
    public Event(Context context, long timestamp, String name, JSONObject parameters) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("The event name cannot be empty or null");
        }

        this.name = name;

        this.id = UUID.randomUUID().toString();

        this.date = new Date(timestamp);

        this.secureDate =
            SecureDateProviderProvider.get().isSecureDateAvailable()
                ? SecureDateProviderProvider.get().getDate()
                : null;

        this.timezone = TimeZone.getDefault();

        if (context != null) {
            String serverTS = ParametersProvider.get(context).get(ParameterKeys.SERVER_TIMESTAMP);
            if (serverTS != null) {
                this.servertime = Long.parseLong(serverTS);
            } else {
                this.servertime = 0;
            }
        } else {
            this.servertime = 0;
        }

        this.state = State.NEW;

        if (parameters == null || parameters.length() == 0) {
            this.parameters = null;
        } else {
            this.parameters = parameters.toString();
        }

        this.session = Batch.getSessionID();
    }

    /**
     * @param id
     * @param name
     * @param date
     * @param timezone
     * @param parameters
     * @param state
     * @param serverTS
     * @param secureDate
     * @param sessionId
     */
    public Event(
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
        this.id = id;
        this.name = name;
        this.date = date;
        this.timezone = timezone;
        this.parameters = parameters;
        this.state = state;
        this.servertime = serverTS;
        this.secureDate = secureDate;
        this.session = sessionId;
    }

    // -------------------------------------------->

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getDate() {
        return date;
    }

    public Date getSecureDate() {
        return secureDate;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public String getParameters() {
        return parameters;
    }

    public State getState() {
        return state;
    }

    public long getServerTimestamp() {
        return servertime;
    }

    public boolean isOld() {
        return state == State.OLD;
    }

    public String getSessionID() {
        return session;
    }

    // --------------------------------------------->

    /**
     * State of the sending
     *
     */
    public enum State {
        /**
         * An event never sent and for this session
         */
        NEW(0),

        /**
         * An event currently sending
         */
        SENDING(1),

        /**
         * An old event (failed to sent during the last session)
         */
        OLD(3);

        // ------------------------------------->

        private int value;

        State(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        // ------------------------------------->

        public static State fromValue(int value) {
            for (State state : values()) {
                if (value == state.getValue()) {
                    return state;
                }
            }

            return null;
        }
    }
}
