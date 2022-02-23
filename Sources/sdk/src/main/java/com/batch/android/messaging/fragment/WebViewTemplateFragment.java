package com.batch.android.messaging.fragment;

import static com.batch.android.BatchMessagingWebViewJavascriptBridge.DevelopmentErrorCause;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.batch.android.BatchMessage;
import com.batch.android.BatchMessagingWebViewJavascriptBridge;
import com.batch.android.R;
import com.batch.android.actions.DeeplinkActionRunnable;
import com.batch.android.core.Logger;
import com.batch.android.json.JSONException;
import com.batch.android.json.JSONObject;
import com.batch.android.messaging.WebViewActionListener;
import com.batch.android.messaging.WebViewHelper;
import com.batch.android.messaging.css.CSSParsingException;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.css.Parser;
import com.batch.android.messaging.css.builtin.BuiltinStyleProvider;
import com.batch.android.messaging.model.Action;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.model.WebViewMessage;
import com.batch.android.messaging.view.formats.WebFormatView;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ThemeHelper;
import com.batch.android.messaging.view.helper.ViewCompat;
import com.batch.android.module.MessagingModule;
import java.util.Locale;
import java.util.Map;

/**
 * WebView messaging template fragment class. Extends DialogFragment so it can be displayed in its own window easily.
 *
 */
public class WebViewTemplateFragment
    extends BaseDialogFragment<WebViewMessage>
    implements WebViewActionListener, MenuItem.OnMenuItemClickListener {

    private static final String TAG = "HtmlTemplateFragment";
    private WebFormatView webView = null;
    private Document style = null;

    private boolean darkStatusbar = false;
    private boolean showStatusbar = true;
    private boolean statusbarBackgroundTranslucent = false;
    private Integer statusbarBackgroundColor = null;

    private boolean dismissed = false;

    private int developmentMenuReloadItemID = ViewCompat.generateViewId();

    public static WebViewTemplateFragment newInstance(BatchMessage payloadMessage, WebViewMessage messageModel) {
        final WebViewTemplateFragment f = new WebViewTemplateFragment();
        f.setMessageArguments(payloadMessage, messageModel);
        return f;
    }

    public WebViewTemplateFragment() {
        automaticallyBeginAutoClose = false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshStatusbarStyle();
        setStyle(STYLE_NO_FRAME, R.style.com_batchsdk_WebViewDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        // We try to use the activity so the webview can display JS Alert etc
        Context context = getActivity();
        if (context == null) {
            context = inflater.getContext();
        }

        View v = getWebFormatView(context);
        if (savedInstanceState != null) {
            this.webView.restoreState(savedInstanceState);
        } else {
            this.webView.startLoading();
        }

        if (getMessageModel().devMode) {
            registerForContextMenu(webView.getCloseButton());
        }

        return v;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        this.webView.saveState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        final Window window = dialog.getWindow();
        if (showStatusbar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && statusbarBackgroundTranslucent) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                statusbarBackgroundColor != null &&
                statusbarBackgroundColor != Color.TRANSPARENT
            ) {
                window.setStatusBarColor(statusbarBackgroundColor);
            }
        }
        return dialog;
    }

    @Override
    public void onDestroyView() {
        final Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (getMessageModel().devMode && v == webView.getCloseButton()) {
            menu.setHeaderTitle("Development menu");
            menu.add(0, developmentMenuReloadItemID, 0, "Reload");
            // onContextItemSelected doesn't work for some reason
            menu.getItem(0).setOnMenuItemClickListener(this);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        // onContextItemSelected doesn't work for some reason
        if (item.getItemId() == developmentMenuReloadItemID) {
            webView.startLoading();
            return true;
        }
        return false;
    }

    /**
     * Dismiss.
     *
     * @deprecated Use dismissSafely() to prevent crashes
     */
    @Override
    @Deprecated
    public void dismiss() {
        super.dismiss();
        dismissed = true;
    }

    /**
     * Dismiss.
     *
     * @deprecated Use dismissSafely() to prevent crashes
     */
    @Override
    @Deprecated
    public void dismissAllowingStateLoss() {
        super.dismissAllowingStateLoss();
        dismissed = true;
    }

    @Override
    protected void dismissSafely() {
        super.dismissSafely();
        dismissed = true;
    }

    private View getWebFormatView(Context context) {
        // Wrap the theme to force the one we want
        final WebViewMessage model = getMessageModel();
        final Context c = new ContextThemeWrapper(context, ThemeHelper.getDefaultLightTheme(context));
        final BatchMessagingWebViewJavascriptBridge jsInterface = new BatchMessagingWebViewJavascriptBridge(
            context,
            getPayloadMessage(),
            this
        );
        final WebFormatView webView = new WebFormatView(c, model, getStyle(), jsInterface);
        webView.setActionListener(this);

        final FrameLayout view = new FrameLayout(context);
        view.addView(
            webView,
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        StyleHelper.applyCommonRules(
            view,
            getStyle().getFlatRules(new DOMNode("root"), ViewCompat.getScreenSize(context))
        );

        this.webView = webView;
        return view;
    }

    private void refreshStatusbarStyle() {
        Map<String, String> rootRules = getStyle().getFlatRules(new DOMNode("root"), null);
        for (Map.Entry<String, String> rule : rootRules.entrySet()) {
            final String ruleName = rule.getKey();
            final String ruleValue = rule.getValue();
            if ("statusbar".equalsIgnoreCase(ruleName)) {
                switch (ruleValue.toLowerCase(Locale.US)) {
                    case "light":
                        darkStatusbar = false;
                        showStatusbar = true;
                        break;
                    case "dark":
                        darkStatusbar = true;
                        showStatusbar = true;
                        break;
                    case "hidden":
                        showStatusbar = false;
                        break;
                }
            } else if ("statusbar-bg".equalsIgnoreCase(ruleName)) {
                if ("translucent".equalsIgnoreCase(ruleValue)) {
                    statusbarBackgroundColor = null;
                    statusbarBackgroundTranslucent = true;
                } else {
                    statusbarBackgroundColor = StyleHelper.parseColor(ruleValue);
                    statusbarBackgroundTranslucent = false;
                }
            }
        }
    }

    private Document getStyle() {
        if (style == null) {
            try {
                style = new Parser(new BuiltinStyleProvider(), getMessageModel().css).parse();
            } catch (CSSParsingException e) {
                throw new IllegalArgumentException("Unparsable style", e);
            }

            if (style == null) {
                throw new IllegalArgumentException("An error occurred while parsing message style");
            }
        }
        return style;
    }

    private void dismissForError(@NonNull MessagingError cause) {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onClosedError(cause);
        }
    }

    // Show a development error
    // Dismisses the format when closed
    // Returns whether the error alert has been shown
    private boolean showDevelopmentError(
        @NonNull DevelopmentErrorCause cause,
        @NonNull MessagingError messagingCause,
        @Nullable String description
    ) {
        switch (cause) {
            case SSL:
                description = "SSL Error. Is your certificate valid?";
                break;
            case TIMEOUT:
                description = "Request timed out.";
                break;
            case BAD_HTTP_STATUSCODE:
                description = "HTTP Error Code " + description + ".";
                break;
            case UNKNOWN:
                final String originalErrorMessage = description;
                description = "Unknown error";
                if (originalErrorMessage != null) {
                    description += ":\n" + originalErrorMessage;
                } else {
                    description += ".";
                }
                break;
        }

        // Always log errors
        Logger.error(MessagingModule.TAG, "WebView was closed because of an error");
        Logger.internal(MessagingModule.TAG, "WebView error: " + cause + " (" + description + ")");

        if (!getMessageModel().devMode) {
            return false;
        }

        Context c = getContext();
        if (c == null) {
            return false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(c, ThemeHelper.getDefaultTheme(c))
        );
        builder.setTitle("WebView Error");
        builder.setMessage(
            "The WebView encountered an error and will be closed.\nThis error will only be shown during development.\n\nCause: " +
            description
        );
        builder.setNegativeButton(android.R.string.ok, (dialog, which) -> {});
        builder.setOnDismissListener(dialog -> dismissForError(messagingCause));

        builder.show();

        return true;
    }

    @Override
    public void onCloseAction() {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onClosed();
        }
    }

    @Override
    public void onDismissAction(@Nullable String analyticsID) {
        onPerformAction("batch.dismiss", new JSONObject(), analyticsID);
    }

    @Override
    public void onErrorAction(
        @NonNull DevelopmentErrorCause developmentCause,
        @NonNull MessagingError messagingCause,
        @Nullable String description
    ) {
        if (!showDevelopmentError(developmentCause, messagingCause, description)) {
            // Dismiss instantly. If a development error has been shown,
            // the dialog will take care of dismissing the format
            dismissForError(messagingCause);
        }
    }

    @Override
    public void onOpenDeeplinkAction(
        @NonNull String url,
        @Nullable Boolean openInAppOverride,
        @Nullable String analyticsID
    ) {
        if (TextUtils.isEmpty(analyticsID)) {
            analyticsID = WebViewHelper.getAnalyticsIDFromURL(url);
        }

        boolean openInApp = getMessageModel().openDeeplinksInApp;
        if (openInAppOverride != null) {
            openInApp = openInAppOverride;
        }
        JSONObject args = new JSONObject();
        try {
            args.put(DeeplinkActionRunnable.ARGUMENT_DEEPLINK_URL, url);
            args.put(DeeplinkActionRunnable.ARGUMENT_SHOW_LINK_INAPP, openInApp);
            onPerformAction(DeeplinkActionRunnable.IDENTIFIER, args, analyticsID);
        } catch (JSONException ignored) {
            // This can only happen for invalid keys. We control them, so throw the exception away
        }
    }

    @Override
    public void onPerformAction(@NonNull String action, @NonNull JSONObject args, @Nullable String analyticsID) {
        if (!dismissed) {
            dismissSafely();

            Action actionObj = new Action(action, args);
            messagingModule.performAction(getContext(), getPayloadMessage(), actionObj);
            if (analyticsDelegate != null) {
                analyticsDelegate.onWebViewClickTracked(actionObj, analyticsID);
            }
        }
    }

    //region: Auto close handling
    @Override
    protected void onAutoCloseCountdownStarted() {}

    @Override
    protected boolean canAutoClose() {
        return false;
    }

    @Override
    protected int getAutoCloseDelayMillis() {
        return 0;
    }

    @Override
    protected void performAutoClose() {}
}
