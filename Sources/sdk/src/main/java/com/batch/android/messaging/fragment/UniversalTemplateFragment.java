package com.batch.android.messaging.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.batch.android.BatchMessage;
import com.batch.android.R;
import com.batch.android.core.Logger;
import com.batch.android.messaging.AsyncImageDownloadTask;
import com.batch.android.messaging.css.CSSParsingException;
import com.batch.android.messaging.css.DOMNode;
import com.batch.android.messaging.css.Document;
import com.batch.android.messaging.css.Parser;
import com.batch.android.messaging.css.builtin.BuiltinStyleProvider;
import com.batch.android.messaging.model.CTA;
import com.batch.android.messaging.model.Message;
import com.batch.android.messaging.model.MessagingError;
import com.batch.android.messaging.model.UniversalMessage;
import com.batch.android.messaging.view.formats.UniversalRootView;
import com.batch.android.messaging.view.helper.StyleHelper;
import com.batch.android.messaging.view.helper.ThemeHelper;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Universal messaging template fragment class. Extends DialogFragment so it can be displayed in its own window easily.
 *
 */
public class UniversalTemplateFragment
    extends BaseDialogFragment<UniversalMessage>
    implements
        UniversalRootView.OnActionListener,
        AsyncImageDownloadTask.ImageDownloadListener,
        MediaPlayer.OnPreparedListener,
        TextureView.SurfaceTextureListener {

    private static final String TAG = "UniversalTemplateFragment";

    private static final String BUNDLE_KEY_MESSAGE_MODEL = "messageModel";
    private UniversalRootView view = null;
    private Document style = null;

    private boolean darkStatusbar = false;
    private boolean showStatusbar = true;
    private boolean statusbarBackgroundTranslucent = false;
    private Integer statusbarBackgroundColor = null;

    private AsyncImageDownloadTask.Result heroDownloadResult = null;
    private AsyncImageDownloadTask heroDownloadTask = null;

    private MediaPlayer mediaPlayer = null;
    private boolean mediaPlayerPrepared = false;
    private Surface videoSurface = null;

    private boolean dismissed = false;

    public static UniversalTemplateFragment newInstance(BatchMessage payloadMessage, UniversalMessage messageModel) {
        final UniversalTemplateFragment f = new UniversalTemplateFragment();
        f.setMessageArguments(payloadMessage, messageModel);
        return f;
    }

    public UniversalTemplateFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshStatusbarStyle();
        setStyle(
            STYLE_NO_FRAME,
            showStatusbar
                ? R.style.com_batchsdk_UniversalDialogTheme
                : R.style.com_batchsdk_UniversalDialogTheme_Fullscreen
        );

        // Retain the instance because of the image download.
        // Once we have a way to trigger a download and be notified of the result and handle cache
        // that's more elablorate, we can remove this.
        // Otherwise, the image will be downloaded on every rotation
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
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        final UniversalMessage messageModel = getMessageModel();
        // Create the video player if necessary
        if (messageModel.videoURL != null) {
            // Since the fragment retains its instance, we can reuse the old mediaplayer
            if (mediaPlayer == null) {
                mediaPlayerPrepared = false;
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setVolume(0, 0);
                mediaPlayer.setLooping(true);
                try {
                    mediaPlayer.setDataSource(messageModel.videoURL);
                } catch (IOException e) {
                    Logger.internal(TAG, "Error while creating the MediaPlayer for URL " + messageModel.videoURL, e);
                }
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.prepareAsync();
            }
        }

        final View v = getUniversalView(inflater.getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && showStatusbar && darkStatusbar) {
            v.setSystemUiVisibility(v.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Start downloading the image here
        if (messageModel.videoURL == null && messageModel.heroImageURL != null && heroDownloadTask == null) {
            // Check if image is in cache
            AsyncImageDownloadTask.Result cachedResult = get(messageModel.heroImageURL);
            if (cachedResult != null) {
                displayImage(cachedResult);
            } else {
                heroDownloadTask = new AsyncImageDownloadTask(this);
                heroDownloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, messageModel.heroImageURL);
            }
        }

        String voiceString = messageModel.getVoiceString();
        if (voiceString != null) {
            AccessibilityManager manager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager != null && manager.isEnabled()) {
                final AccessibilityEvent e = AccessibilityEvent.obtain();
                e.setEventType(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);

                v.onInitializeAccessibilityEvent(e);
                e.getText().add(voiceString);
                manager.sendAccessibilityEvent(e);
            }
        }
        return v;
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

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.setDisplay(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }
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

    private View getUniversalView(@NonNull Context context) {
        // Wrap the theme to force the one we want
        final Context themedContext = new ContextThemeWrapper(context, ThemeHelper.getDefaultLightTheme(context));
        view =
            new UniversalRootView(
                themedContext,
                getMessageModel(),
                getStyle(),
                heroDownloadResult,
                shouldWaitForHeroImage()
            );
        view.setActionListener(this);
        view.setSurfaceHolderCallback(this);
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

    private synchronized boolean shouldWaitForHeroImage() {
        if (heroDownloadResult == null && getMessageModel().heroImageURL != null) {
            if (heroDownloadTask == null) {
                return true;
            } else {
                if (heroDownloadTask.getStatus() != AsyncTask.Status.FINISHED) {
                    return true;
                }
            }
        }
        return false;
    }

    //region: Auto close handling

    @Override
    protected void onAutoCloseCountdownStarted() {
        if (view != null) {
            view.startAutoCloseCountdown();
        }
    }

    @Override
    protected boolean canAutoClose() {
        return view == null || view.canAutoClose();
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
            if (analyticsDelegate != null) {
                analyticsDelegate.onClosed();
            }
        }
    }

    @Override
    public void onCTAAction(int index, @NonNull CTA cta) {
        if (!dismissed) {
            dismissSafely();
            if (analyticsDelegate != null) {
                analyticsDelegate.onCTAClicked(index, cta);
            }
            messagingModule.performAction(getContext(), getPayloadMessage(), cta);
        }
    }

    @Override
    public void onImageDownloadStart() {
        view.onHeroBitmapStartsDownloading();
    }

    @Override
    public void onImageDownloadSuccess(AsyncImageDownloadTask.Result result) {
        // Add image in cache then display it
        put(result);
        displayImage(result);
    }

    @Override
    public void onImageDownloadError(@NonNull MessagingError ignored) {
        heroDownloadResult = null;
        view.onHeroDownloaded(null);
    }

    private void displayImage(AsyncImageDownloadTask.Result result) {
        heroDownloadResult = result;
        view.onHeroDownloaded(heroDownloadResult);
    }

    //region: MediaPlayer callbacks

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayerPrepared = true;
        startPlayingVideo();
    }

    //endregion

    private void startPlayingVideo() {
        if (mediaPlayer != null && mediaPlayerPrepared && videoSurface != null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        videoSurface = new Surface(surfaceTexture);

        if (mediaPlayer != null) {
            mediaPlayer.setSurface(videoSurface);
        }
        startPlayingVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        }

        if (videoSurface != null) {
            videoSurface.release();
            videoSurface = null;
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
}
