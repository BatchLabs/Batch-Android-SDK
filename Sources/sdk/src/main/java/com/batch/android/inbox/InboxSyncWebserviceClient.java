package com.batch.android.inbox;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchWebservice;
import com.batch.android.core.Logger;
import com.batch.android.core.ParameterKeys;
import com.batch.android.core.Parameters;
import com.batch.android.core.TaskRunnable;
import com.batch.android.di.providers.InboxDatasourceProvider;
import com.batch.android.json.JSONArray;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.post.InboxSyncPostDataProvider;
import com.batch.android.post.PostDataProvider;
import com.batch.android.webservice.listener.InboxWebserviceListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webservice client for the Inbox API
 * Used to sync notifications from the server
 */

public class InboxSyncWebserviceClient extends BatchWebservice implements TaskRunnable {

    private static final String TAG = "InboxSyncWebserviceClient";

    private final long fetcherId;

    @Nullable
    private final String authentication;

    @NonNull
    private List<InboxCandidateNotificationInternal> candidates;

    @NonNull
    private final InboxSyncPostDataProvider dataProvider;

    @NonNull
    private final InboxWebserviceListener listener;

    public InboxSyncWebserviceClient(
        @NonNull Context context,
        @NonNull FetcherType type,
        @NonNull String identifier,
        @Nullable String authentication,
        @Nullable Integer limit,
        @Nullable String from,
        long fetcherId,
        @NonNull List<InboxCandidateNotificationInternal> candidates,
        @NonNull InboxWebserviceListener listener
    ) throws MalformedURLException {
        super(context, RequestType.POST, Parameters.INBOX_SYNC_WS_URL, type.toWSPathElement(), identifier);
        this.authentication = authentication;
        this.fetcherId = fetcherId;
        this.candidates = candidates;
        this.dataProvider = new InboxSyncPostDataProvider(candidates);
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
        return "Batch/inboxsyncwsc";
    }

    @Override
    public void run() {
        try {
            Logger.internal(TAG, "Starting inbox sync (" + buildURL().toString() + ")");
            JSONObject response = getBasicJsonResponseBody();
            listener.onSuccess(computeResponse(response));
        } catch (WebserviceError e1) {
            Logger.internal(TAG, "Inbox sync failed: ", e1);

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
            Logger.internal(TAG, "Inbox sync failed: ", e2);
            listener.onFailure("Internal webservice call error - code 20");
        } catch (ResponseParsingException e3) {
            Logger.internal(TAG, "Inbox response parsing failed: ", e3);
            listener.onFailure("Internal webservice call error - code 30");
        }
    }

    private InboxWebserviceResponse computeResponse(JSONObject json) throws ResponseParsingException {
        try {
            final InboxWebserviceResponse r = new InboxWebserviceResponse();

            r.hasMore = json.getBoolean("hasMore");
            r.didTimeout = json.reallyOptBoolean("timeout", false);
            r.cursor = json.reallyOptString("cursor", null);
            if (TextUtils.isEmpty(r.cursor)) {
                r.cursor = null;
            }

            JSONObject cache = json.optJSONObject("cache");
            if (cache != null) {
                // Handle caching operations
                long cacheMarkAllAsRead = cache.reallyOptLong("lastMarkAllAsRead", -1L);
                if (cacheMarkAllAsRead > 0) {
                    InboxDatasourceProvider.get(applicationContext).markAllAsRead(cacheMarkAllAsRead, fetcherId);
                }

                JSONArray delete = cache.optJSONArray("delete");
                if (delete != null && delete.length() > 0) {
                    List<String> deleteIds = new ArrayList<>();
                    for (int i = 0; i < delete.length(); ++i) {
                        String deleteId = delete.optString(i);
                        if (!TextUtils.isEmpty(deleteId)) {
                            deleteIds.add(deleteId);
                        }
                    }
                    InboxDatasourceProvider.get(applicationContext).deleteNotifications(deleteIds);
                }
            }

            List<String> ids = new ArrayList<>();
            JSONArray rawNotifications = json.getJSONArray("notifications");
            for (int i = 0; i < rawNotifications.length(); i++) {
                Object rawNotification = rawNotifications.get(i);
                if (rawNotification instanceof JSONObject) {
                    String notificationId = ((JSONObject) rawNotification).reallyOptString("notificationId", null);

                    if (notificationId != null) {
                        if (isCandidates(notificationId)) {
                            // The notification is a candidate, it's already in DB, update it
                            String id = InboxDatasourceProvider
                                .get(applicationContext)
                                .updateNotification((JSONObject) rawNotification, fetcherId);
                            if (id != null) {
                                ids.add(id);
                            }
                        } else {
                            // The notification isn't a candidate, inserting it
                            try {
                                InboxNotificationContentInternal newNotification = InboxFetchWebserviceClient.parseNotification(
                                    (JSONObject) rawNotification
                                );
                                if (
                                    InboxDatasourceProvider.get(applicationContext).insert(newNotification, fetcherId)
                                ) {
                                    ids.add(notificationId);
                                }
                            } catch (ResponseParsingException e) {
                                Logger.internal(TAG, "Failed to parse notification content, skipping.", e);
                            }
                        }
                    } else {
                        Logger.internal(
                            TAG,
                            "Json element doesn't have an id in notification array, skipping. Found: " +
                            rawNotification.toString()
                        );
                    }
                } else {
                    Logger.internal(
                        TAG,
                        "Invalid json element found in notification array, skipping. Found: " +
                        rawNotification.toString()
                    );
                }
            }

            r.notifications = InboxDatasourceProvider.get(applicationContext).getNotifications(ids, fetcherId);
            return r;
        } catch (JSONException e) {
            throw new ResponseParsingException("Missing key or invalid value type in response JSON", e);
        }
    }

    private boolean isCandidates(String notificationId) {
        for (InboxCandidateNotificationInternal candidate : candidates) {
            if (candidate.identifier.equals(notificationId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected PostDataProvider<JSONObject> getPostDataProvider() {
        return dataProvider;
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
        return ParameterKeys.INBOX_WS_POST_CRYPTORTYPE_KEY;
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
