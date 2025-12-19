package com.batch.android.messaging.fragment.cep

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.batch.android.BatchMessage
import com.batch.android.BatchMessagingWebViewJavascriptBridge.DevelopmentErrorCause
import com.batch.android.R
import com.batch.android.actions.DeeplinkActionRunnable
import com.batch.android.core.Logger
import com.batch.android.json.JSONException
import com.batch.android.json.JSONObject
import com.batch.android.messaging.AsyncImageDownloadTask
import com.batch.android.messaging.AsyncImageDownloadTask.ImageDownloadListener
import com.batch.android.messaging.WebViewActionListener
import com.batch.android.messaging.WebViewHelper
import com.batch.android.messaging.fragment.BaseDialogFragment
import com.batch.android.messaging.model.Action
import com.batch.android.messaging.model.CTA
import com.batch.android.messaging.model.MessagingError
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.view.formats.cep.BuildableRootView
import com.batch.android.messaging.view.formats.cep.BuildableRootView.OnActionListener
import com.batch.android.messaging.view.helper.ThemeHelper
import com.batch.android.module.MessagingModule

class CEPTemplateFragment :
    BaseDialogFragment<CEPMessage>(), OnActionListener, WebViewActionListener {

    companion object {
        @JvmStatic
        fun newInstance(
            payloadMessage: BatchMessage?,
            messageModel: CEPMessage,
        ): CEPTemplateFragment {
            return CEPTemplateFragment().apply { setMessageArguments(payloadMessage, messageModel) }
        }
    }

    /** Whether this view has been dismissed. */
    private var dismissed = false

    /** Root view for the message. */
    private lateinit var rootView: BuildableRootView

    /** Images view instances. */
    private var imagesCached: MutableMap<String, AsyncImageDownloadTask.Result<*>> = mutableMapOf()

    /**
     * Listeners for image download tasks Important: This allows to retain references to listeners
     * since it's a weak reference in the async task.
     */
    private var imageDownloadListeners: MutableMap<String, ImageDownloadListener> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (messageModel.isFullscreen() || messageModel.isWebView()) {
            setStyle(STYLE_NO_FRAME, R.style.com_batchsdk_CEPDialogTheme_Fullscreen)
        } else {
            setStyle(STYLE_NO_FRAME, R.style.com_batchsdk_CEPDialogTheme)
        }

        // Retain the instance because of the image download.
        // Once we have a way to trigger a download and be notified of the result and handle cache
        // that's more elaborate, we can remove this.
        // Otherwise, the image will be downloaded on every rotation
        retainInstance = true
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val window = dialog.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            val nightModeFlags =
                context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
            windowInsetsController.isAppearanceLightStatusBars =
                nightModeFlags == Configuration.UI_MODE_NIGHT_NO
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                ViewCompat.onApplyWindowInsets(view, windowInsets)
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return buildView(inflater.context)
    }

    /** Build the view. */
    private fun buildView(context: Context): View {
        // Wrap the theme to force the one we want
        val themedContext: Context =
            ContextThemeWrapper(context, ThemeHelper.getDefaultLightTheme(context))

        // Build the view
        rootView =
            BuildableRootView(themedContext, messageModel, payloadMessage, imagesCached, this, this)
        rootView.applyDialogInsets(dialog?.window)

        // Start downloading images
        downloadImages()

        return rootView
    }

    /** Download images if not already downloaded. */
    private fun downloadImages() {
        for (component in messageModel.getImagesComponents()) {
            if (imagesCached[component.id] != null) {
                // Image already in cache. Skipping.
                continue
            }
            val listener =
                object : ImageDownloadListener {
                    override fun onImageDownloadStart() {
                        rootView.startDownloadImage(component.id)
                    }

                    override fun onImageDownloadSuccess(result: AsyncImageDownloadTask.Result<*>) {
                        imagesCached[component.id] = result
                        rootView.setDownloadedImage(component.id, result)
                    }

                    override fun onImageDownloadError(errorCause: MessagingError) {
                        Logger.warning(MessagingModule.TAG, "Failed downloading image: $errorCause")
                        if (messageModel.isImageFormat()) {
                            onErrorAction(errorCause)
                            return
                        }
                        rootView.downloadImageFailed(component.id)
                    }
                }
            imageDownloadListeners[component.id] = listener
            AsyncImageDownloadTask(listener).apply {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, messageModel.urls[component.id])
            }
        }
    }

    /** Start the auto close countdown. */
    override fun onAutoCloseCountdownStarted() {
        rootView.startAutoCloseCountdown()
    }

    /** Whether this view can be auto-closed. */
    override fun canAutoClose(): Boolean {
        return rootView.canAutoClose()
    }

    /** Get the auto close delay in milliseconds. */
    override fun getAutoCloseDelayMillis(): Int {
        return messageModel.closeOptions?.auto?.delay.let { (it?.times(1000)) ?: 0 }
    }

    /** Perform the auto close. */
    override fun performAutoClose() {
        if (!dismissed) {
            dismissSafely()
            if (analyticsDelegate != null) {
                analyticsDelegate.onAutoClosedAfterDelay()
            }
        }
    }

    /** Dismiss the dialog safely. */
    override fun dismissSafely() {
        super.dismissSafely()
        dismissed = true
    }

    /** Called when the close button is clicked. */
    override fun onCloseAction() {
        if (!dismissed) {
            dismissSafely()
            if (analyticsDelegate != null) {
                analyticsDelegate.onClosed()
            }
        }
    }

    /** Called when the dismiss button is clicked from a webview component. */
    override fun onDismissAction(analyticsID: String?) {
        onPerformAction("batch.dismiss", JSONObject(), analyticsID)
    }

    /** Called when the webview fails to load. */
    override fun onErrorAction(
        developmentCause: DevelopmentErrorCause,
        messagingCause: MessagingError,
        description: String?,
    ) {
        if (!showDevelopmentError(developmentCause, messagingCause, description)) {
            // Dismiss instantly. If a development error has been shown,
            // the dialog will take care of dismissing the format
            dismissForError(messagingCause)
        }
    }

    /** Called when a deeplink is clicked from a webview component. */
    override fun onOpenDeeplinkAction(
        url: String,
        openInAppOverride: Boolean?,
        analyticsID: String?,
    ) {
        var analyticsID = analyticsID
        if (TextUtils.isEmpty(analyticsID)) {
            analyticsID = WebViewHelper.getAnalyticsIDFromURL(url)
        }
        messageModel.getWebViewComponent()?.let {
            var openInApp: Boolean = it.openDeeplinksInApp
            if (openInAppOverride != null) {
                openInApp = openInAppOverride
            }
            val args = JSONObject()
            try {
                args.put(DeeplinkActionRunnable.ARGUMENT_DEEPLINK_URL, url)
                args.put(DeeplinkActionRunnable.ARGUMENT_SHOW_LINK_INAPP, openInApp)
                onPerformAction(DeeplinkActionRunnable.IDENTIFIER, args, analyticsID)
            } catch (_: JSONException) {
                // This can only happen for invalid keys. We control them, so throw the exception
                // away
            }
        }
            ?: run {
                Logger.warning(
                    MessagingModule.TAG,
                    "Cannot perform deeplink: WebView component not found",
                )
            }
    }

    /** Perform an action from a webview component. */
    override fun onPerformAction(action: String, args: JSONObject, analyticsID: String?) {
        if (!dismissed) {
            dismissSafely()

            val actionObj = Action(action, args)
            messagingModule.performAction(context, getPayloadMessage(), actionObj)
            if (analyticsDelegate != null) {
                analyticsDelegate.onWebViewClickTracked(actionObj, analyticsID)
            }
        }
    }

    /** Called when a CTA is clicked. */
    override fun onCTAAction(id: String, type: String, cta: CTA) {
        if (!dismissed) {
            dismissSafely()
            if (analyticsDelegate != null) {
                analyticsDelegate.onCTAClicked(id, type, cta)
            }
            messagingModule.performAction(context, payloadMessage, cta)
        }
    }

    /** Called when the image download fails and the message is an image format. */
    override fun onErrorAction(cause: MessagingError) {
        dismissForError(cause)
    }

    /** Dismiss the dialog and dispatches an error event. */
    private fun dismissForError(cause: MessagingError) {
        if (!dismissed) {
            dismissSafely()
            analyticsDelegate.onClosedError(cause)
        }
    }

    // Show a development error
    // Dismisses the format when closed
    // Returns whether the error alert has been shown
    private fun showDevelopmentError(
        cause: DevelopmentErrorCause,
        messagingCause: MessagingError,
        description: String?,
    ): Boolean {
        var description = description
        when (cause) {
            DevelopmentErrorCause.SSL -> description = "SSL Error. Is your certificate valid?"
            DevelopmentErrorCause.TIMEOUT -> description = "Request timed out."
            DevelopmentErrorCause.BAD_HTTP_STATUSCODE ->
                description = "HTTP Error Code $description."

            DevelopmentErrorCause.UNKNOWN -> {
                val originalErrorMessage = description
                description = "Unknown error"
                if (originalErrorMessage != null) {
                    description += ":\n$originalErrorMessage"
                } else {
                    description += "."
                }
            }
        }

        // Always log errors
        Logger.error(MessagingModule.TAG, "WebView was closed because of an error")
        Logger.internal(MessagingModule.TAG, "WebView error: $cause ($description)")

        val component = messageModel.getWebViewComponent()
        if (component == null) {
            return false
        }

        if (!component.devMode) {
            return false
        }

        if (context == null) {
            return false
        }

        val builder =
            AlertDialog.Builder(
                ContextThemeWrapper(context, ThemeHelper.getDefaultTheme(context!!))
            )
        builder.setTitle("WebView Error")
        builder.setMessage(
            "The WebView encountered an error and will be closed.\nThis error will only be shown during development.\n\nCause: " +
                description
        )
        builder.setNegativeButton(android.R.string.ok) { dialog: DialogInterface?, which: Int -> }
        builder.setOnDismissListener { dialog: DialogInterface? -> dismissForError(messagingCause) }
        builder.show()
        return true
    }
}
