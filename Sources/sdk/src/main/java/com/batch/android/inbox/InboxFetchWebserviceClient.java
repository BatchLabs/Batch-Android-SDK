package com.batch.android.inbox;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.Batch;
import com.batch.android.BatchWebservice;
import com.batch.android.core.InternalPushData;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.InboxDatasourceProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.post.PostDataProvider;
import com.batch.android.webservice.listener.InboxWebserviceListener;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Webservice client for the Inbox API
 * Used to fetch notifications from the server
 */

public class InboxFetchWebserviceClient extends BatchWebservice implements TaskRunnable {

    private static final String TAG = "InboxFetchWebserviceClient";

    private long fetcherId;

    @Nullable
    private final String authentication;

    @NonNull
    private final InboxWebserviceListener listener;

    public InboxFetchWebserviceClient(
        @NonNull Context context,
        @NonNull FetcherType type,
        @NonNull String identifier,
        @Nullable String authentication,
        @Nullable Integer limit,
        @Nullable String from,
        long fetcherId,
        @NonNull InboxWebserviceListener listener
    ) throws MalformedURLException {
        super(context, RequestType.GET, Parameters.INBOX_FETCH_WS_URL, type.toWSPathElement(), identifier);
        this.authentication = authentication;
        this.fetcherId = fetcherId;
        this.listener = listener;

        if (from != null) {
            addGetParameter("from", from);
        }

        if (limit != null) {
            addGetParameter("limit", limit.toString());
        }
    }

    @Override
    protected Map<String, String> getHeaders() {
        if (authentication != null) {
            final Map<String, String> headers = new HashMap<>();
            headers.put("X-CustomID-Auth", authentication);
            return headers;
        }

        return null;
    }

    @Override
    public String getTaskIdentifier() {
        return "Batch/inboxwsc";
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "Starting inbox fetch (" + buildURL().toString() + ")");
            JSONObject response = getBasicJsonResponseBody();
            InboxWebserviceResponse parsedResponse = parseResponse(response);

            // Insert response in cache
            if (fetcherId > 0) {
                InboxDatasourceProvider.get(applicationContext).insertResponse(parsedResponse, this.fetcherId);
            }
            listener.onSuccess(parsedResponse);
        } catch (WebserviceError e1) {
            Logger.internal(TAG, "Inbox fetch failed: ", e1);

            if (e1.getReason() == WebserviceError.Reason.FORBIDDEN) {
                listener.onFailure(
                    "Inbox API call error: Unauthorized. Please make sure that the hexadecimal HMAC for that custom ID is valid. (code 11)"
                );
            } else if (e1.getReason() == WebserviceError.Reason.SDK_OPTED_OUT) {
                listener.onFailure("Inbox API call error: Batch SDK has been globally Opted Out.");
            } else {
                listener.onFailure("Internal webservice call error - code 10");
            }
        } catch (JSONException e2) {
            Logger.internal(TAG, "Inbox fetch failed: ", e2);
            listener.onFailure("Internal webservice call error - code 20");
        } catch (ResponseParsingException e3) {
            Logger.internal(TAG, "Inbox response parsing failed: ", e3);
            listener.onFailure("Internal webservice call error - code 30");
        }
    }

    private InboxWebserviceResponse parseResponse(JSONObject json) throws ResponseParsingException {
        final InboxWebserviceResponse r = new InboxWebserviceResponse();

        try {
            r.hasMore = json.getBoolean("hasMore");
            r.didTimeout = json.reallyOptBoolean("timeout", false);
            r.cursor = json.reallyOptString("cursor", null);
            if (TextUtils.isEmpty(r.cursor)) {
                r.cursor = null;
            }

            JSONArray rawNotifications = json.getJSONArray("notifications");
            for (int i = 0; i < rawNotifications.length(); i++) {
                Object rawNotification = rawNotifications.get(i);
                if (rawNotification instanceof JSONObject) {
                    try {
                        r.notifications.add(parseNotification((JSONObject) rawNotification));
                    } catch (ResponseParsingException e) {
                        Logger.internal(TAG, "Failed to parse notification content, skipping.", e);
                    }
                } else {
                    Logger.internal(
                        TAG,
                        "Invalid json element found in notification array, skipping. Found: " +
                        rawNotification.toString()
                    );
                }
            }
        } catch (JSONException e) {
            throw new ResponseParsingException("Missing key or invalid value type in response JSON", e);
        }

        return r;
    }

    protected static InboxNotificationContentInternal parseNotification(JSONObject json)
        throws ResponseParsingException {
        try {
            final JSONObject payload = json.getJSONObject("payload");
            final InternalPushData batchData = new InternalPushData(payload.getJSONObject("com.batch"));

            // If so we're probably doing useless work
            final Map<String, String> convertedPayload = new HashMap<>();
            for (String payloadKey : payload.keySet()) {
                try {
                    convertedPayload.put(payloadKey, payload.getString(payloadKey));
                } catch (JSONException ignored) {
                    Logger.internal(
                        TAG,
                        "Could not coalesce payload value to string for key \"" + payloadKey + "\". Ignoring."
                    );
                }
            }

            final NotificationIdentifiers identifiers = new NotificationIdentifiers(
                json.getString("notificationId"),
                json.getString("sendId")
            );
            identifiers.customID = json.reallyOptString("customId", null);
            identifiers.installID = json.reallyOptString("installId", null);
            identifiers.additionalData = batchData.getExtraParameters();

            final InboxNotificationContentInternal c = new InboxNotificationContentInternal(
                batchData.getSource(),
                new Date(json.getLong("notificationTime")),
                convertedPayload,
                identifiers
            );

            c.body = payload.reallyOptString(Batch.Push.BODY_KEY, null);
            c.title = payload.reallyOptString(Batch.Push.TITLE_KEY, null);
            c.isUnread = !json.reallyOptBoolean("read", false) && !json.reallyOptBoolean("opened", false);
            c.isDeleted = false;

            if (!c.isValid()) {
                throw new ResponseParsingException(
                    "Parsed notification does not pass integrity checks. You may have an empty 'payload' or missing identifiers."
                );
            }

            return c;
        } catch (JSONException e) {
            throw new ResponseParsingException("Missing key or invalid value type in response JSON", e);
        }
    }

    @Override
    protected PostDataProvider<JSONObject> getPostDataProvider() {
        return null;
    }

    @Override
    protected String getPropertyParameterKey() {
        return null;
    }

    @Override
    protected String getURLSorterPatternParameterKey() {
        return ParameterKeys.INBOX_WS_URLSORTER_PATTERN_KEY;
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
    protected String getReadCryptorTypeParameterKey() {
        return ParameterKeys.INBOX_WS_READ_CRYPTORTYPE_KEY;
    }

    @Override
    protected String getSpecificConnectTimeoutKey() {
        return ParameterKeys.INBOX_WS_CONNECT_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificReadTimeoutKey() {
        return ParameterKeys.INBOX_WS_READ_TIMEOUT_KEY;
    }

    @Override
    protected String getSpecificRetryCountKey() {
        return ParameterKeys.INBOX_WS_RETRYCOUNT_KEY;
    }
}
