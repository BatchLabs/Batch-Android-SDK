package com.batch.android.messaging.view.formats;

import static android.widget.RelativeLayout.ALIGN_PARENT_RIGHT;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.CENTER_IN_PARENT;
import static com.batch.android.BatchMessagingWebViewJavascriptBridge.DevelopmentErrorCause;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import com.batch.android.Batch;
import com.batch.android.BatchMessagingWebViewJavascriptBridge;
import com.batch.android.R;
import com.batch.android.core.ReflectionHelper;
import com.batch.android.messaging.WebViewActionListener;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.model.WebViewMessage;
import com.batch.android.messaging.view.AnimatedCloseButton;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ThemeHelper;
import com.batch.android.messaging.view.helper.ViewCompat;
import com.batch.android.messaging.view.styled.WebView;
import java.util.HashMap;
import java.util.Map;

/**
 * View rendering the Image format
 * Not called ImageView for obvious reasons
 */
@SuppressLint("ViewConstructor")
public class WebFormatView extends FrameLayout {

    private static final String STATE_TIMEOUT_DONE_KEY = "timeoutDone";

    private static final float CLOSE_SIZE_DP = 52;
    private static final float CLOSE_PADDING_DP = 10;
    private static final float FULLSCREEN_CLOSE_BUTTON_MARGIN_DP = 20;

    private final Context context;
    private final WebViewMessage message;
    private final Document style;
    private final Point screenSizeDP;

    private final AnimatedCloseButton closeButton;
    private final RelativeLayout rootContainerView;
    //private final WebViewContainerView webViewContainerView;
    private final ProgressBar webViewLoader;

    private Handler timeoutHandler;

    private WebView webView;
    private boolean timeoutDone = false;

    private WebViewActionListener actionListener;

    /**
     * Create an image format view from the given message
     *
     * @param context
     */
    @SuppressLint({ "SetJavaScriptEnabled", "AddJavascriptInterface" })
    public WebFormatView(
        @NonNull Context context,
        @NonNull WebViewMessage message,
        @Nullable Document cachedStyle,
        @NonNull BatchMessagingWebViewJavascriptBridge jsInterface
    ) {
        super(context);
        setId(R.id.com_batchsdk_messaging_root_view);

        this.context = context;
        this.message = message;
        this.style = cachedStyle != null ? cachedStyle : StyleHelper.parseStyle(message.css);
        this.screenSizeDP = ViewCompat.getScreenSize(context);

        rootContainerView = addRootContainerView();
        webView = addWebView(rootContainerView);
        webViewLoader = addWebViewLoader(rootContainerView);
        closeButton = addCloseButton(rootContainerView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setSupportMultipleWindows(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            // Work around an issue where android could show "ERR_CACHE_MISS"
            // In a perfect world we would like to make use of the browser cache
            // but as those messages are usually one shot, it doesn't really matter.
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

        webView.addJavascriptInterface(jsInterface, "_batchAndroidBridge");
        webView.setWebChromeClient(
            new WebChromeClient() {
                @Override
                public boolean onCreateWindow(
                    android.webkit.WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    Message resultMsg
                ) {
                    if (!isDialog && isUserGesture && actionListener != null) {
                        // A link with target="_blank" has been clicked
                        WebView.HitTestResult result = view.getHitTestResult();
                        actionListener.onOpenDeeplinkAction(result.getExtra(), null, null);
                    }
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
                }

                @Override
                public void onCloseWindow(android.webkit.WebView window) {
                    super.onCloseWindow(window);
                    if (window.getId() == R.id.com_batchsdk_messaging_web_view) {
                        dismissMessage();
                    }
                }

                @Override
                public void onProgressChanged(android.webkit.WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    if (newProgress >= 100) {
                        removeWebViewLoader();
                    }
                }

                @Override
                public boolean onJsAlert(android.webkit.WebView view, String url, String message, JsResult result) {
                    AlertDialog.Builder builder = makeAlertBuilder();
                    builder.setTitle(android.R.string.dialog_alert_title);
                    builder.setMessage(message);
                    builder.setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> {
                            result.confirm();
                        }
                    );
                    builder.setOnCancelListener(dialog -> {
                        result.cancel();
                    });

                    builder.show();
                    return true;
                }

                @Override
                public boolean onJsConfirm(android.webkit.WebView view, String url, String message, JsResult result) {
                    AlertDialog.Builder builder = makeAlertBuilder();
                    builder.setTitle(android.R.string.dialog_alert_title);
                    builder.setMessage(message);
                    builder.setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> {
                            result.confirm();
                        }
                    );
                    builder.setOnCancelListener(dialog -> {
                        result.cancel();
                    });

                    builder.show();
                    return true;
                }

                @Override
                public boolean onJsPrompt(
                    android.webkit.WebView view,
                    String url,
                    String message,
                    String defaultValue,
                    JsPromptResult result
                ) {
                    EditText editText = new EditText(context);
                    editText.setText(defaultValue);

                    AlertDialog.Builder builder = makeAlertBuilder();
                    builder.setTitle(android.R.string.dialog_alert_title);
                    builder.setMessage(message);
                    builder.setView(editText);
                    builder.setPositiveButton(
                        android.R.string.ok,
                        (dialogInterface, i) -> {
                            String text = editText.getText().toString();
                            result.confirm(text);
                        }
                    );
                    builder.setNegativeButton(
                        android.R.string.cancel,
                        (dialog, which) -> {
                            result.cancel();
                        }
                    );
                    builder.setOnCancelListener(dialog -> {
                        result.cancel();
                    });

                    builder.show();
                    return true;
                }
            }
        );

        webView.setWebViewClient(
            new WebViewClient() {
                private boolean mainFrameFinished = false;

                // Page started to be drawn on screen. We consider this as loaded, meaning that we should dismiss the loaders
                // and stop the timeouts.
                // On API 21-22, this will unfortunately be when the DOM is ready as there is no
                // better callback.
                // This method is also prefixed so that we never conflict with an Android SDK update
                public void onBatchPageStartedDrawing() {
                    timeoutDone = true;
                    if (!mainFrameFinished) {
                        mainFrameFinished = true;
                        // Fist page has been successfully loaded, hide the spinner
                        removeWebViewLoader();
                    }
                }

                @Override
                public void onPageCommitVisible(android.webkit.WebView view, String url) {
                    super.onPageCommitVisible(view, url);
                    onBatchPageStartedDrawing();
                }

                @Override
                public void onPageFinished(android.webkit.WebView view, String url) {
                    // Only called for main frame !
                    super.onPageFinished(view, url);
                    // On API 23+, onPageCommitVisible will be called instead
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        onBatchPageStartedDrawing();
                    }
                }

                @Override
                public void onReceivedError(
                    android.webkit.WebView view,
                    int errorCode,
                    String description,
                    String failingUrl
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    // Only needed when API lvl < 21
                    if (
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&
                        !mainFrameFinished &&
                        (failingUrl != null && failingUrl.equals(view.getUrl()))
                    ) {
                        handleWebViewError(description, errorCode);
                    }
                }

                @Override
                public void onReceivedSslError(android.webkit.WebView view, SslErrorHandler handler, SslError error) {
                    super.onReceivedSslError(view, handler, error);
                    if (!mainFrameFinished) {
                        closeMessageWithError(DevelopmentErrorCause.SSL, MessagingError.SERVER_FAILURE, null);
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onReceivedHttpError(
                    android.webkit.WebView view,
                    WebResourceRequest request,
                    WebResourceResponse errorResponse
                ) {
                    super.onReceivedHttpError(view, request, errorResponse);
                    if (request.isForMainFrame()) {
                        closeMessageWithError(
                            DevelopmentErrorCause.BAD_HTTP_STATUSCODE,
                            MessagingError.SERVER_FAILURE,
                            Integer.toString(errorResponse.getStatusCode())
                        );
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onReceivedError(
                    android.webkit.WebView view,
                    WebResourceRequest request,
                    WebResourceError error
                ) {
                    super.onReceivedError(view, request, error);
                    if (request.isForMainFrame() && request.getUrl().toString().equals(view.getUrl())) {
                        handleWebViewError(error.getDescription().toString(), error.getErrorCode());
                    }
                }
            }
        );

        setFitsSystemWindows(true);
        ReflectionHelper.optOutOfDarkModeRecursively(this);
    }

    public View getCloseButton() {
        return closeButton;
    }

    public void startLoading() {
        scheduleTimeout();

        final Map<String, String> headers = new HashMap<>();
        final String language = Batch.User.getLanguage(this.getContext());
        final String region = Batch.User.getRegion(this.getContext());

        if (!TextUtils.isEmpty(language)) {
            headers.put("X-Batch-Custom-Language", language);
        }

        if (!TextUtils.isEmpty(region)) {
            headers.put("X-Batch-Custom-Region", language);
        }

        webView.loadUrl(message.url, headers);
    }

    private void handleWebViewError(String description, int errorCode) {
        // Try to get an accurate MessagingError out of the WebView error code
        MessagingError messagingCause = MessagingError.UNKNOWN;
        switch (errorCode) {
            case WebViewClient.ERROR_HOST_LOOKUP:
            case WebViewClient.ERROR_CONNECT:
            case WebViewClient.ERROR_TIMEOUT:
            case WebViewClient.ERROR_IO:
                messagingCause = MessagingError.CLIENT_NETWORK;
                break;
            case WebViewClient.ERROR_AUTHENTICATION:
            case WebViewClient.ERROR_FILE_NOT_FOUND:
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
            case WebViewClient.ERROR_BAD_URL:
            case WebViewClient.ERROR_UNSAFE_RESOURCE:
                messagingCause = MessagingError.SERVER_FAILURE;
                break;
        }

        closeMessageWithError(DevelopmentErrorCause.UNKNOWN, messagingCause, description);
    }

    private void closeMessage() {
        if (actionListener != null) {
            actionListener.onCloseAction();
        }
    }

    private void dismissMessage() {
        if (actionListener != null) {
            actionListener.onDismissAction(null);
        }
    }

    private void closeMessageWithError(
        DevelopmentErrorCause developmentCause,
        MessagingError messagingCause,
        String detail
    ) {
        if (actionListener != null) {
            actionListener.onErrorAction(developmentCause, messagingCause, detail);
        }
    }

    private void scheduleTimeout() {
        if (message.timeout > 0) {
            timeoutHandler = new Handler();
            timeoutHandler.postDelayed(this::performTimeout, message.timeout);
        }
    }

    private void performTimeout() {
        if (!timeoutDone) {
            closeMessageWithError(DevelopmentErrorCause.TIMEOUT, MessagingError.CLIENT_NETWORK, null);
        }
    }

    public void setActionListener(WebViewActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void saveState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_TIMEOUT_DONE_KEY, timeoutDone);
        webView.saveState(outState);
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
            timeoutHandler = null;
        }
    }

    public void restoreState(@NonNull Bundle outState) {
        webView.restoreState(outState);
        timeoutDone = outState.getBoolean(STATE_TIMEOUT_DONE_KEY, false);
        if (!timeoutDone) {
            scheduleTimeout();
        }
    }

    public boolean canAutoClose() {
        return false;
    }

    private AlertDialog.Builder makeAlertBuilder() {
        return new AlertDialog.Builder(new ContextThemeWrapper(context, ThemeHelper.getDefaultTheme(context)));
    }

    private Map<String, String> getRulesForView(DOMNode node) {
        return this.style.getFlatRules(node, screenSizeDP);
    }

    //region Layouting

    private RelativeLayout addRootContainerView() {
        final RelativeLayout rootContainer = new RelativeLayout(context);
        rootContainer.setId(R.id.com_batchsdk_messaging_container_view);
        rootContainer.setLayoutParams(
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );

        addView(rootContainer);
        return rootContainer;
    }

    private WebView addWebView(@NonNull RelativeLayout container) {
        final WebView webView = new WebView(context);
        webView.setId(R.id.com_batchsdk_messaging_web_view);
        webView.setBackgroundColor(Color.TRANSPARENT);

        final Map<String, String> rules = getRulesForView(new DOMNode("webview"));
        webView.applyStyleRules(rules);

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webView.setLayoutParams(lp);

        container.addView(webView);
        return webView;
    }

    private ProgressBar addWebViewLoader(@NonNull RelativeLayout container) {
        final ProgressBar webViewLoader = new ProgressBar(context);
        webViewLoader.setId(ViewCompat.generateViewId());
        webViewLoader.setIndeterminate(true);

        boolean darkProgressBar = false;
        Map<String, String> rules = getRulesForView(new DOMNode("root"));
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            if ("loader".equalsIgnoreCase(rule.getKey())) {
                String ruleValue = rule.getValue();
                if ("dark".equalsIgnoreCase(ruleValue)) {
                    darkProgressBar = true;
                } else if ("light".equalsIgnoreCase(ruleValue)) {
                    darkProgressBar = false;
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (darkProgressBar) {
                webViewLoader.setIndeterminateTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                webViewLoader.setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));
            }
        }

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.addRule(CENTER_IN_PARENT);
        webViewLoader.setLayoutParams(lp);

        container.addView(webViewLoader);
        return webViewLoader;
    }

    private void removeWebViewLoader() {
        if (webViewLoader != null && webViewLoader.getParent() != null) {
            ((ViewGroup) webViewLoader.getParent()).removeView(webViewLoader);
        }
    }

    private AnimatedCloseButton addCloseButton(@NonNull RelativeLayout container) {
        final AnimatedCloseButton closeButton = new AnimatedCloseButton(context);
        closeButton.setId(R.id.com_batchsdk_messaging_close_button);
        closeButton.setShowBorder(true);
        final Map<String, String> closeButtonRules = getRulesForView(new DOMNode("close"));
        closeButton.applyStyleRules(closeButtonRules);

        int buttonSize = StyleHelper.dpToPixels(getResources(), CLOSE_SIZE_DP);

        int padding = StyleHelper.dpToPixels(getResources(), CLOSE_PADDING_DP);
        closeButton.setPadding(padding);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(buttonSize, buttonSize);
        lp.addRule(ALIGN_PARENT_TOP);
        lp.addRule(ALIGN_PARENT_RIGHT);

        int margin = StyleHelper.dpToPixels(getResources(), FULLSCREEN_CLOSE_BUTTON_MARGIN_DP);
        lp.setMargins(0, margin, margin, 0);

        closeButton.setLayoutParams(lp);
        closeButton.setOnClickListener(v -> {
            closeMessage();
        });

        container.addView(closeButton);
        return closeButton;
    }

    //endregion

    //region System inset handling

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // Apparently, the relative layout does not really like to apply the insets, so convert it as
        // margin
        if (rootContainerView != null) {
            LayoutParams lp = (LayoutParams) rootContainerView.getLayoutParams();
            lp.setMargins(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom()
            );
            rootContainerView.setLayoutParams(lp);
        }

        return super.onApplyWindowInsets(insets.replaceSystemWindowInsets(0, 0, 0, 0));
    }
    //endregion
}
