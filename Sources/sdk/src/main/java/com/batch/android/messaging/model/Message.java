package com.batch.android.messaging.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.model.cep.CEPMessage;
import com.batch.android.messaging.model.mep.BannerMessage;
import com.batch.android.messaging.model.mep.MEPMessage;
import java.io.Serializable;

/**
 * Represents a message to be displayed in the application.
 */
public abstract class Message implements Serializable {

    /**
     * Represents the source of the message.
     */
    public enum Source {
        UNKNOWN,
        LANDING,
        LOCAL,
        INBOX_LANDING,
    }

    protected Message(@Nullable String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
    }

    /**
     * The serial version UID for this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The unique identifier of the message. Used for analytics and tracking purposes.
     * (Only for MEP messages)
     */
    @Nullable
    public String messageIdentifier;

    /**
     * The developer tracking identifier for the message.
     */
    @Nullable
    public String devTrackingIdentifier;

    /**
     * The event data associated with the message.
     */
    @Nullable
    public JSONObject eventData;

    /**
     * The source of the message.
     */
    @NonNull
    public Source source = Source.UNKNOWN;

    /**
     * Whether this message came from MEP
     * @return Whether this message came from MEP
     */
    public boolean isMEPMessage() {
        return this instanceof MEPMessage;
    }

    /**
     * Whether this message came from CEP
     * @return Whether this message came from CEP
     */
    public boolean isCEPMessage() {
        return this instanceof CEPMessage;
    }

    /**
     * Whether this message should be displayed as a banner
     * @return Whether this message should be displayed as a banner
     */
    public boolean isBannerMessage() {
        return this instanceof BannerMessage || (this instanceof CEPMessage && ((CEPMessage) this).isBanner());
    }
}
