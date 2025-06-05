package com.batch.android.messaging.view.formats.cep

import android.app.Activity
import android.content.res.Configuration
import android.os.AsyncTask
import android.os.SystemClock
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.batch.android.BatchMessage
import com.batch.android.MessagingAnalyticsDelegate
import com.batch.android.core.Logger
import com.batch.android.di.providers.MessagingModuleProvider
import com.batch.android.messaging.AsyncImageDownloadTask
import com.batch.android.messaging.AsyncImageDownloadTask.ImageDownloadListener
import com.batch.android.messaging.model.CTA
import com.batch.android.messaging.model.MessagingError
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.view.formats.EmbeddedBannerContainer
import com.batch.android.module.MessagingModule
import com.batch.android.processor.Module
import com.batch.android.processor.Provide

/**
 * Container for Batch's banners built from the CEP. Handles everything related to the banner: its
 * lifecycle, window insets, etc...
 *
 * <p>
 * Kinda similar to
 * https://android.googlesource.com/platform/frameworks/support.git/+/master/design/src/android/support/design/widget/BaseTransientBottomBar.java
 */
@Module
class CEPBannerContainer
private constructor(
    attachTo: View,
    private val message: CEPMessage,
    analyticsDelegate: MessagingAnalyticsDelegate,
    payloadMessage: BatchMessage,
    embed: Boolean,
    messagingModule: MessagingModule,
) :
    EmbeddedBannerContainer(attachTo, messagingModule, analyticsDelegate, payloadMessage, embed),
    BuildableRootView.OnActionListener {

    companion object {
        @Provide
        fun provide(
            attachTo: View,
            rawMessage: BatchMessage,
            message: CEPMessage,
            analyticsDelegate: MessagingAnalyticsDelegate,
            embed: Boolean,
        ): CEPBannerContainer {
            return CEPBannerContainer(
                attachTo,
                message,
                analyticsDelegate,
                rawMessage,
                embed,
                MessagingModuleProvider.get(),
            )
        }
    }

    /** Images cached */
    private var imagesCached: MutableMap<String, AsyncImageDownloadTask.Result<*>> = mutableMapOf()

    /**
     * Listeners for image download tasks Important: This allows to retain references to listeners
     * since it's a weak reference in the async task.
     */
    private var imageDownloadListeners: MutableMap<String, ImageDownloadListener> = mutableMapOf()

    /** Constructor. */
    init {
        // Make the banner view, cannot be done from super constructor
        // since its calling child methods
        super.makeView()

        // Start downloading images and keep them in cache
        downloadImages()

        // Fits system windows to only if message is not fullscreen or an attached banner
        if (message.shouldFitsSystemWindows()) {
            // Fits system windows to dodge the status bar and navigation bar
            rootView.fitsSystemWindows = true
        }

        // Handle the status bar color according to the theme
        val window = (context as? Activity)?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            val nightModeFlags =
                context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
            windowInsetsController.isAppearanceLightStatusBars =
                nightModeFlags == Configuration.UI_MODE_NIGHT_NO
            ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
                ViewCompat.onApplyWindowInsets(view, windowInsets)
            }
        }
    }

    /** Cast the banner view as a RootView. */
    private val buildableRootView: BuildableRootView
        get() = bannerView as BuildableRootView

    /** Make the banner view. */
    override fun makeBannerView(): BuildableRootView {
        return BuildableRootView(this.context, this.message, imagesCached).apply {
            actionListener = this@CEPBannerContainer
        }
    }

    /** Whether the banner can be dismissed by swiping down/up according to the edge. */
    override fun allowSwipeToDismiss(): Boolean {
        return true
    }

    /** Get the edge that the banner is pinned to. */
    override fun getPinnedVerticalEdge(): VerticalEdge {
        return if (message.position == InAppProperty.VerticalAlignment.TOP) VerticalEdge.TOP
        else VerticalEdge.BOTTOM
    }

    /** Schedule the auto close. */
    override fun scheduleAutoClose() {
        if (buildableRootView.canAutoClose()) {
            message.closeOptions?.auto?.let {
                val timestamp: Long = (SystemClock.uptimeMillis() + (it.delay.times(1000)))
                mainThreadHandler.postAtTime(
                    { this.performAutoClose() },
                    autoCloseHandlerToken,
                    timestamp,
                )
                buildableRootView.startAutoCloseCountdown()
            }
        }
    }

    /** Called when the close button is clicked. */
    override fun onCloseAction() {
        dismiss(true)
        analyticsDelegate.onClosed()
    }

    /** Called when a CTA is clicked. */
    override fun onCTAAction(id: String, type: String, cta: CTA) {
        dismiss(true)
        analyticsDelegate.onCTAClicked(id, type, cta)
        messagingModule.performAction(context, payloadMessage, cta)
    }

    /** Called when the image download fails and the message is an image format. */
    override fun onErrorAction(cause: MessagingError) {
        dismiss(true)
        analyticsDelegate.onClosedError(cause)
    }

    /** Download images if needed. */
    private fun downloadImages() {
        for (component in message.getImagesComponents()) {
            if (imagesCached[component.id] != null) {
                // Image already in cache. Skipping.
                continue
            }
            val listener =
                object : ImageDownloadListener {
                    override fun onImageDownloadStart() {
                        buildableRootView.startDownloadImage(component.id)
                    }

                    override fun onImageDownloadSuccess(result: AsyncImageDownloadTask.Result<*>?) {
                        if (result != null) {
                            imagesCached[component.id] = result
                            buildableRootView.setDownloadedImage(component.id, result)
                        }
                    }

                    override fun onImageDownloadError(errorCause: MessagingError) {
                        Logger.warning("Failed downloading image: $errorCause")
                        if (message.isImageFormat()) {
                            onErrorAction(errorCause)
                            return
                        }
                        buildableRootView.downloadImageFailed(component.id)
                    }
                }
            imageDownloadListeners[component.id] = listener
            AsyncImageDownloadTask(listener).apply {
                executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message.urls[component.id])
            }
        }
    }
}
