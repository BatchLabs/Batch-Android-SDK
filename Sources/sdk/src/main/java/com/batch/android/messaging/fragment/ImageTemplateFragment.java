package com.batch.android.messaging.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.batch.android.messaging.model.ImageMessage;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.view.formats.ImageFormatView;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ThemeHelper;
import com.batch.android.messaging.view.helper.ViewCompat;

/**
 * Universal messaging template fragment class. Extends DialogFragment so it can be displayed in its own window easily.
 *
 */
public class ImageTemplateFragment
    extends BaseDialogFragment<ImageMessage>
    implements ImageFormatView.OnActionListener, ModalContentPanGestureDetector.OnDismissListener {

    private static final String TAG = "ImageTemplateFragment";
    private ImageFormatView imageFormatView = null;
    private Document style = null;

    private boolean darkStatusbar = false;
    private boolean showStatusbar = true;
    private boolean statusbarBackgroundTranslucent = false;
    private Integer statusbarBackgroundColor = null;

    private Bitmap heroBitmap = null;
    private AsyncImageDownloadTask heroDownloadTask = null;

    private boolean dismissed = false;

    public static ImageTemplateFragment newInstance(BatchMessage payloadMessage, ImageMessage messageModel) {
        final ImageTemplateFragment f = new ImageTemplateFragment();
        f.setMessageArguments(payloadMessage, messageModel);
        return f;
    }

    public ImageTemplateFragment() {
        automaticallyBeginAutoClose = false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int theme = getMessageModel().isFullscreen
            ? R.style.com_batchsdk_ImageDialogFullscreenTheme
            : R.style.com_batchsdk_ImageDialogModalTheme;
        setStyle(STYLE_NO_FRAME, theme);

        // Retain the instance because of the image download.
        // Once we have a way to trigger a download and be notified of the result and handle cache
        // that's more elablorate, we can remove this.
        // Otherwise, the image will be downloaded on every rotation
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        final View v = getImageFormatView(inflater.getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && showStatusbar && darkStatusbar) {
            v.setSystemUiVisibility(v.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (imageFormatView != null) {
            imageFormatView.onShown();
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

    private View getImageFormatView(Context context) {
        // Wrap the theme to force the one we want
        final ImageMessage model = getMessageModel();
        final Context c = new ContextThemeWrapper(context, ThemeHelper.getDefaultLightTheme(context));
        final ImageFormatView imageFormatView = new ImageFormatView(c, model, getStyle(), this);
        imageFormatView.setActionListener(this);

        if (model.allowSwipeToDismiss) {
            ModalContentPanGestureDetector detector = new ModalContentPanGestureDetector(context, model.isFullscreen);
            final ImageFormatView.ImageContainerView target = imageFormatView.getPannableView();
            detector.attach(target, imageFormatView.getPanEffectsView());
            detector.setDismissListener(this);
        }

        final FrameLayout view = new FrameLayout(context);
        view.addView(
            imageFormatView,
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        );
        StyleHelper.applyCommonRules(
            view,
            getStyle().getFlatRules(new DOMNode("root"), ViewCompat.getScreenSize(context))
        );

        this.imageFormatView = imageFormatView;
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

    //region: Auto close handling

    @Override
    protected void onAutoCloseCountdownStarted() {
        if (imageFormatView != null) {
            imageFormatView.startAutoCloseCountdown();
        }
    }

    @Override
    protected boolean canAutoClose() {
        return imageFormatView == null || imageFormatView.canAutoClose();
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
    public void onGlobalAction() {
        if (!dismissed) {
            dismissSafely();
            final ImageMessage message = getMessageModel();
            if (message.globalTapAction != null) {
                analyticsDelegate.onGlobalTap(message.globalTapAction);
                messagingModule.performAction(getContext(), getPayloadMessage(), message.globalTapAction);
            } else {
                Logger.error(TAG, "Could not perform global tap action. Internal error.");
            }
        }
    }

    @Override
    public void onErrorAction(@NonNull MessagingError cause) {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onClosedError(cause);
        }
    }

    @Override
    public void onImageDisplayedAction() {
        beginAutoCloseCountdown();
    }

    @Override
    public void onPanDismiss() {
        if (!dismissed) {
            dismissSafely();
            analyticsDelegate.onClosed();
        }
    }
}
