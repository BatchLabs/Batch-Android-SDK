package com.batch.android.messaging.fragment.cep

import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.batch.android.BatchMessage
import com.batch.android.R
import com.batch.android.core.Logger
import com.batch.android.messaging.AsyncImageDownloadTask
import com.batch.android.messaging.AsyncImageDownloadTask.ImageDownloadListener
import com.batch.android.messaging.fragment.BaseDialogFragment
import com.batch.android.messaging.model.CTA
import com.batch.android.messaging.model.MessagingError
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.view.formats.cep.BuildableRootView
import com.batch.android.messaging.view.formats.cep.BuildableRootView.OnActionListener
import com.batch.android.messaging.view.helper.ThemeHelper

class CEPTemplateFragment : BaseDialogFragment<CEPMessage>(), OnActionListener {

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
        if (messageModel.format == InAppProperty.Format.FULLSCREEN) {
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

        rootView = BuildableRootView(themedContext, messageModel, imagesCached)
        rootView.applyDialogInsets(dialog?.window)
        rootView.actionListener = this

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

                    override fun onImageDownloadSuccess(result: AsyncImageDownloadTask.Result<*>?) {
                        if (result != null) {
                            imagesCached[component.id] = result
                            rootView.setDownloadedImage(component.id, result)
                        }
                    }

                    override fun onImageDownloadError(errorCause: MessagingError) {
                        Logger.warning("Failed downloading image: $errorCause")
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
        if (!dismissed) {
            dismissSafely()
            analyticsDelegate.onClosedError(cause)
        }
    }
}
