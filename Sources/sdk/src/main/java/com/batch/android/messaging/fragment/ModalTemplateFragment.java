package com.batch.android.messaging.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchMessage;
import com.batch.android.R;
import com.batch.android.core.Logger;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.ModalContentPanGestureDetector;
import com.batch.android.messaging.css.CSSParsingException;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.css.Parser;
import com.batch.android.messaging.css.builtin.BuiltinStyleProvider;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.ModalMessage;
import com.batch.android.messaging.view.formats.BannerView;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ThemeHelper;
import com.batch.android.messaging.view.helper.ViewCompat;
import java.util.Locale;
import java.util.Map;

/**
 * Universal messaging template fragment class. Extends DialogFragment so it can be displayed in its own window easily.
 *
 */
public class ModalTemplateFragment
    extends BaseDialogFragment<ModalMessage>
    implements BannerView.OnActionListener, ModalContentPanGestureDetector.OnDismissListener {

    private static final String TAG = "ModalTemplateFragment";

    private BannerView bannerView = null;
    private Document style = null;

    private boolean darkStatusbar = false;
    private boolean showStatusbar = true;
    private boolean statusbarBackgroundTranslucent = false;
    private Integer statusbarBackgroundColor = null;

    private Bitmap heroBitmap = null;
    private AsyncImageDownloadTask heroDownloadTask = null;

    private boolean dismissed = false;

    public static ModalTemplateFragment newInstance(BatchMessage payloadMessage, ModalMessage messageModel) {
        final ModalTemplateFragment f = new ModalTemplateFragment();
        f.setMessageArguments(payloadMessage, messageModel);
        return f;
    }

    public ModalTemplateFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshStatusbarStyle();
        setStyle(
            STYLE_NO_FRAME,
            showStatusbar ? R.style.com_batchsdk_ModalDialogTheme : R.style.com_batchsdk_ModalDialogTheme_Fullscreen
        );

        // See UniversalTemplateFragment as of why we're retaining the instance
        setRetainInstance(true);
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

    @Nullable
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        final View v = getBannerView(inflater.getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && showStatusbar && darkStatusbar) {
            v.setSystemUiVisibility(v.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (bannerView != null) {
            bannerView.onShown();
        }
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
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

    private View getBannerView(Context context) {
        // Wrap the theme to force the one we want
        final ModalMessage model = getMessageModel();
        final Context c = new ContextThemeWrapper(context, ThemeHelper.getDefaultLightTheme(context));
        final BannerView bannerView = new BannerView(c, model, getStyle(), new DOMNode("container"), this);
        bannerView.setActionListener(this);

        if (model.allowSwipeToDismiss) {
            ModalContentPanGestureDetector detector = new ModalContentPanGestureDetector(context, false);
            detector.attach(bannerView.getContentView(), bannerView);
            detector.setDismissListener(this);
        }

        final FrameLayout view = new FrameLayout(context);
        // BannerView comes with its own FrameLayout layout params
        view.addView(bannerView);

        StyleHelper.applyCommonRules(
            view,
            getStyle().getFlatRules(new DOMNode("root"), ViewCompat.getScreenSize(context))
        );

        this.bannerView = bannerView;
        return view;
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

    //region: Auto close handling

    @Override
    protected void onAutoCloseCountdownStarted() {
        if (bannerView != null) {
            bannerView.startAutoCloseCountdown();
        }
    }

    @Override
    protected boolean canAutoClose() {
        return bannerView == null || bannerView.canAutoClose();
    }

    @Override
    protected int getAutoCloseDelayMillis() {
        return getMessageModel().autoCloseDelay;
    }

    @Override
    protected void performAutoClose() {
        if (!dismissed) {
            dismissSafely();
            if (analyticsDelegate != null) {
                analyticsDelegate.onAutoClosedAfterDelay();
            }
        }
    }

    //endregion

    @Override
    public void onCloseAction() {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onClosed();
        }
    }

    @Override
    public void onCTAAction(int index, @NonNull CTA cta) {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onCTAClicked(index, cta);
            messagingModule.performAction(getContext(), getPayloadMessage(), cta);
        }
    }

    @Override
    public void onGlobalAction() {
        if (!dismissed) {
            dismissSafely();
            final ModalMessage message = getMessageModel();
            messagingModule.onMessageGlobalTap(message, message.globalTapAction);
            if (message.globalTapAction != null) {
                messagingModule.performAction(getContext(), getPayloadMessage(), message.globalTapAction);
            } else {
                Logger.internal(TAG, "Could not perform global tap action. Internal error.");
            }
        }
    }

    @Override
    public void onPanDismiss() {
        if (!dismissed) {
            dismissSafely();
        }
    }
}
