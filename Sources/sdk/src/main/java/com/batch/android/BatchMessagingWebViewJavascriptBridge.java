package com.batch.android;

import static com.batch.android.core.InternalPushData.BATCH_BUNDLE_KEY;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.batch.android.annotation.PublicSDK;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.WebViewActionListener;
import java.util.Locale;

/**
 * Class providing method implementation for the In-App Webview javascript SDK.
 * Should not be used from native code.
 *
 * @hide
 */
@PublicSDK
public class BatchMessagingWebViewJavascriptBridge {

    private static final String TAG = "BatchWebviewJSInterface";

    private final Context applicationContext;
    private final BatchMessage message;
    private final WebViewActionListener actionListener;

    public BatchMessagingWebViewJavascriptBridge(
        @NonNull Context context,
        BatchMessage message,
        WebViewActionListener actionListener
    ) {
        this.applicationContext = context.getApplicationContext();
        this.message = message;
        this.actionListener = actionListener;
    }

    @JavascriptInterface
    @NonNull
    public String postMessage(@Nullable String method, @Nullable String rawJsonArguments) {
        if (method == null || TextUtils.isEmpty(method)) {
            return makeErrorResult("Internal SDK error (-10): Invalid method");
        }

        JSONObject arguments;
        if (TextUtils.isEmpty(rawJsonArguments)) {
            return makeErrorResult("Internal SDK error (-11): Invalid arguments");
        }
        try {
            arguments = new JSONObject(rawJsonArguments);
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not deserialize postMessage arguments. Got: '" + rawJsonArguments + "'.", e);
            return makeErrorResult("Internal SDK error (-12): Invalid arguments");
        }

        try {
            return makeSuccessResult(getMethodResultProvider(method, arguments));
        } catch (UnknownMethodException e) {
            Logger.error("Messaging", "Unknown In-App Webview Javascript SDK method: '" + method + "'");
            return makeErrorResult("Unimplemented native method '" + method + "'. Is the native SDK too old?");
        } catch (BridgeResultProviderException e1) {
            // BridgeResultProviderException messages are considered public and can be forwarded as is
            Logger.error("Messaging", "In-App Webview Javascript SDK error (" + method + "): " + e1.getMessage());
            return makeErrorResult(e1.getMessage());
        } catch (BridgeResultProviderRuntimeException e2) {
            Logger.internal(TAG, "Internal bridge error: " + e2.getMessage(), e2);
            return makeErrorResult("Internal SDK error (" + e2.getCode() + ")");
        } catch (Exception e3) {
            Logger.internal(TAG, "Unexpected bridge error", e3);
            return makeErrorResult("Internal SDK error (-3)");
        }
    }

    private String makeSuccessResult(@NonNull BridgeResultProvider resultProvider)
        throws BridgeResultProviderException {
        try {
            JSONObject result = new JSONObject();
            result.put("result", resultProvider.getResult());
            return result.toString();
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not serialize success JSON", e);
            return "{'error':'unknown serialization error (-1)'}";
        }
    }

    private String makeErrorResult(@NonNull String reason) {
        try {
            JSONObject result = new JSONObject();
            result.put("error", reason);
            return result.toString();
        } catch (JSONException e) {
            Logger.internal(TAG, "Could not serialize error JSON", e);
            return "{'error':'unknown serialization error (-2)'}";
        }
    }

    private BridgeResultProvider getMethodResultProvider(@NonNull String method, @NonNull JSONObject arguments)
        throws BridgeResultProviderException, UnknownMethodException {
        switch (method.toLowerCase(Locale.US)) {
            case "getattributionid":
                return this::getAdvertisingID;
            case "getinstallationid":
                return this::getInstallationID;
            case "getcustomlanguage":
                return this::getCustomLanguage;
            case "getcustomregion":
                return this::getCustomRegion;
            case "getcustomuserid":
                return this::getCustomUserID;
            case "getcustompayload":
                return this::getCustomPayload;
            case "gettrackingid":
                return this::getTrackingID;
            case "opendeeplink":
                openDeeplink(arguments);
                return getGenericSuccessResultProvider();
            case "performaction":
                performAction(arguments);
                return getGenericSuccessResultProvider();
            case "dismiss":
                dismiss(arguments);
                return getGenericSuccessResultProvider();
            default:
                throw new UnknownMethodException();
        }
    }

    //region Bridge method implementations

    @VisibleForTesting
    @Nullable
    protected String getInstallationID() {
        return Batch.User.getInstallationID();
    }

    @VisibleForTesting
    @Nullable
    protected String getCustomLanguage() {
        return Batch.User.getLanguage(applicationContext);
    }

    @VisibleForTesting
    @Nullable
    protected String getCustomRegion() {
        return Batch.User.getRegion(applicationContext);
    }

    @VisibleForTesting
    @Nullable
    protected String getCustomUserID() {
        return Batch.User.getIdentifier(applicationContext);
    }

    @VisibleForTesting
    @NonNull
    protected String getAdvertisingID() throws BridgeResultProviderException {
        if (isAdvertisingIDAllowedByConfig()) {
            String advertisingID = getAdvertisingIDValue();
            if (advertisingID != null) {
                return advertisingID;
            } else {
                throw new BridgeResultProviderException(
                    "Advertising ID unavailable: Couldn't fetch it from the system provider. Device user may have disabled it, missing project dependency or an library didn't return any."
                );
            }
        } else {
            throw new BridgeResultProviderException("Advertising ID unavailable: Disabled by config");
        }
    }

    @VisibleForTesting
    protected boolean isAdvertisingIDAllowedByConfig() {
        return Batch.shouldUseAdvertisingID();
    }

    @VisibleForTesting
    @Nullable
    protected String getAdvertisingIDValue() {
        try {
            AdvertisingID advertisingID = Batch.getAdvertisingID();
            if (advertisingID != null && advertisingID.isNotNull()) {
                return advertisingID.get();
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    @NonNull
    private String getCustomPayload() {
        try {
            JSONObject payload = new JSONObject(message.getCustomPayloadInternal());
            payload.remove(BATCH_BUNDLE_KEY);
            return payload.toString();
        } catch (JSONException e) {
            throw new BridgeResultProviderRuntimeException(-23, "Could not copy custom payload JSON", e);
        }
    }

    private String getTrackingID() {
        JSONObject payload = message.getJSON();
        if (payload != null) {
            return payload.reallyOptString("did", null);
        }
        throw new BridgeResultProviderRuntimeException(-20, "Could not get message JSON");
    }

    private void dismiss(@NonNull JSONObject arguments) {
        String analyticsID = arguments.reallyOptString("analyticsID", null);

        if (actionListener != null) {
            actionListener.onDismissAction(analyticsID);
        }
    }

    private void performAction(@NonNull JSONObject arguments) {
        String name = arguments.reallyOptString("name", null);
        if (TextUtils.isEmpty(name)) {
            throw new BridgeResultProviderRuntimeException(-21, "Cannot perform action: empty name");
        }

        JSONObject actionArguments = arguments.optJSONObject("args");
        if (actionArguments == null) {
            actionArguments = new JSONObject();
        }

        String analyticsID = arguments.reallyOptString("analyticsID", null);

        if (actionListener != null) {
            actionListener.onPerformAction(name, actionArguments, analyticsID);
        }
    }

    private void openDeeplink(@NonNull JSONObject arguments) {
        String url = arguments.reallyOptString("url", null);
        if (TextUtils.isEmpty(url)) {
            throw new BridgeResultProviderRuntimeException(-21, "Cannot perform action: empty URL");
        }

        // null means that the parent will use its default value
        Boolean openInAppOverride = arguments.reallyOptBoolean("openInApp", null);

        String analyticsID = arguments.reallyOptString("analyticsID", null);

        if (actionListener != null) {
            actionListener.onOpenDeeplinkAction(url, openInAppOverride, analyticsID);
        }
    }

    //endregion

    private BridgeResultProvider getGenericSuccessResultProvider() {
        return () -> "ok";
    }

    private interface BridgeResultProvider {
        @Nullable
        String getResult() throws BridgeResultProviderException;
    }

    /**
     * Bridge exceptions are public exceptions, meaning that they represent an error
     * that is expected and can be forwarded to the JS SDK as is.
     * Their message is considered public.
     */
    private static class BridgeResultProviderException extends Exception {

        public BridgeResultProviderException(@NonNull String publicMessage) {
            super(publicMessage);
        }

        @SuppressWarnings("ConstantConditions")
        @NonNull
        @Override
        public String getMessage() {
            return super.getMessage();
        }
    }

    /**
     * Internal bridge exception
     * Should not be surfaced to the developer, but should log internal info.
     * The JS SDK will log a error code for support
     */
    private static class BridgeResultProviderRuntimeException extends RuntimeException {

        private final int code;
        private final String internalMessage;

        public BridgeResultProviderRuntimeException(int code, @NonNull String internalMessage) {
            super();
            this.code = code;
            this.internalMessage = internalMessage;
        }

        public BridgeResultProviderRuntimeException(
            int code,
            @NonNull String internalMessage,
            @NonNull Throwable cause
        ) {
            super(cause);
            this.code = code;
            this.internalMessage = internalMessage;
        }

        public int getCode() {
            return code;
        }

        @NonNull
        @Override
        public String getMessage() {
            return internalMessage;
        }
    }

    private static class UnknownMethodException extends Exception {}

    public enum DevelopmentErrorCause {
        UNKNOWN,
        SSL,
        BAD_HTTP_STATUSCODE,
        TIMEOUT,
    }
}
