package com.batch.android.messaging.view.styled.cep

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.batch.android.Batch
import com.batch.android.BatchMessage
import com.batch.android.BatchMessagingWebViewJavascriptBridge
import com.batch.android.BatchMessagingWebViewJavascriptBridge.DevelopmentErrorCause
import com.batch.android.R
import com.batch.android.core.Logger
import com.batch.android.messaging.WebViewActionListener
import com.batch.android.messaging.model.MessagingError
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.parsing.CEPPayloadParser
import com.batch.android.messaging.view.helper.ThemeHelper
import com.batch.android.messaging.view.helper.ViewCompat
import com.batch.android.module.MessagingModule

class WebView(context: Context) : RelativeLayout(context), Styleable {

    /** Listener for the webview actions */
    private var actionListener: WebViewActionListener? = null

    /** Whether the timeout has been done */
    private var timeoutDone = false

    /** Handler for the timeout */
    private var timeoutHandler: Handler? = null

    /** Timeout (in ms) */
    private var timeout: Int = CEPPayloadParser.DEFAULT_WEBVIEW_TIMEOUT * 1000

    /** Webview */
    private lateinit var webView: WebView

    /** ProgressBar for loading */
    private lateinit var loader: ProgressBar

    override fun applyComponentStyle(component: InAppComponent) {

        if (component !is InAppComponent.WebView) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-webview style")
            return
        }

        // Set Container layout params
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // Add webview
        webView =
            WebView(context).apply {
                id = R.id.com_batchsdk_messaging_web_view
                layoutParams =
                    FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setBackgroundColor(Color.TRANSPARENT)
            }
        addView(webView)

        // Add loader
        loader =
            ProgressBar(context).apply {
                setId(ViewCompat.generateViewId())
                isIndeterminate = true
                val nightModeFlags =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE))
                } else {
                    setIndeterminateTintList(ColorStateList.valueOf(Color.BLACK))
                }
                layoutParams =
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        addRule(CENTER_IN_PARENT)
                    }
            }
        addView(loader)

        // Set timeout (in ms)
        timeout = component.timeout * 1000
    }

    /** Start loading the url */
    fun startLoading(url: String) {
        scheduleTimeout()
        val headers: MutableMap<String?, String?> = HashMap()
        val language = Batch.User.getLanguage(this.context)
        val region = Batch.User.getRegion(this.context)

        if (!TextUtils.isEmpty(language)) {
            headers.put("X-Batch-Custom-Language", language)
        }

        if (!TextUtils.isEmpty(region)) {
            headers.put("X-Batch-Custom-Region", language)
        }
        webView.loadUrl(url, headers)
    }

    /** Handle a webview error */
    private fun handleWebViewError(description: String?, errorCode: Int) {
        // Try to get an accurate MessagingError out of the WebView error code
        var messagingCause = MessagingError.UNKNOWN
        when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP,
            WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_TIMEOUT,
            WebViewClient.ERROR_IO -> messagingCause = MessagingError.CLIENT_NETWORK
            WebViewClient.ERROR_AUTHENTICATION,
            WebViewClient.ERROR_FILE_NOT_FOUND,
            WebViewClient.ERROR_UNSUPPORTED_SCHEME,
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE,
            WebViewClient.ERROR_BAD_URL,
            WebViewClient.ERROR_UNSAFE_RESOURCE -> messagingCause = MessagingError.SERVER_FAILURE
        }
        closeMessageWithError(DevelopmentErrorCause.UNKNOWN, messagingCause, description)
    }

    /** Schedule the timeout */
    private fun scheduleTimeout() {
        if (timeout > 0) {
            timeoutHandler = Handler(context.mainLooper)
            timeoutHandler?.postDelayed({ this.performTimeout() }, timeout.toLong())
        }
    }

    /** Perform the timeout */
    private fun performTimeout() {
        if (!timeoutDone) {
            closeMessageWithError(
                DevelopmentErrorCause.TIMEOUT,
                MessagingError.CLIENT_NETWORK,
                null,
            )
        }
    }

    /** Remove the loader */
    private fun removeWebViewLoader() {
        loader.visibility = GONE
    }

    /** Dismiss the message */
    private fun dismissMessage() {
        actionListener?.onDismissAction(null)
    }

    /** Close the message with an error */
    private fun closeMessageWithError(
        cause: DevelopmentErrorCause,
        error: MessagingError,
        extra: String?,
    ) {
        actionListener?.onErrorAction(cause, error, extra)
    }

    /** Make an alert builder */
    private fun makeAlertBuilder(): AlertDialog.Builder {
        return AlertDialog.Builder(
            ContextThemeWrapper(context, ThemeHelper.getDefaultTheme(context))
        )
    }

    /** Initialize the webview settings */
    @SuppressLint("SetJavaScriptEnabled")
    fun initWebSettings(publicMessage: BatchMessage, actionListener: WebViewActionListener?) {
        this.actionListener = actionListener

        val webSettings: WebSettings = webView.getSettings()
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.defaultTextEncodingName = "utf-8"
        webSettings.setSupportMultipleWindows(true)
        webSettings.setSupportZoom(false)
        webSettings.allowContentAccess = false
        // Work around an issue where android could show "ERR_CACHE_MISS"
        // In a perfect world we would like to make use of the browser cache
        // but as those messages are usually one shot, it doesn't really matter.
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

        // Add Batch JS Bridge
        val jsInterface =
            BatchMessagingWebViewJavascriptBridge(context, publicMessage, this.actionListener)
        webView.addJavascriptInterface(jsInterface, "_batchAndroidBridge")
        webView.setWebChromeClient(
            object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?,
                ): Boolean {
                    if (!isDialog && isUserGesture && actionListener != null) {
                        // A link with target="_blank" has been clicked
                        val result = view.getHitTestResult()
                        var url = result.extra
                        // Fix the case where we have an image in a hyperlink and
                        // view.getHitTestResult() returns the source of the image
                        // rather than the url.
                        if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            val handler = view.handler
                            if (handler != null) {
                                val href = handler.obtainMessage()
                                view.requestFocusNodeHref(href)
                                val data = href.getData()
                                if (data != null) {
                                    val imageUrl = data.getString("url")
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        url = imageUrl
                                    }
                                }
                            }
                        }
                        if (url != null) {
                            actionListener.onOpenDeeplinkAction(url, null, null)
                        }
                    }
                    return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }

                override fun onCloseWindow(window: WebView) {
                    super.onCloseWindow(window)
                    if (window.id == R.id.com_batchsdk_messaging_web_view) {
                        dismissMessage()
                    }
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress >= 100) {
                        removeWebViewLoader()
                    }
                }

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult,
                ): Boolean {
                    val builder: AlertDialog.Builder = makeAlertBuilder()
                    builder.setTitle(android.R.string.dialog_alert_title)
                    builder.setMessage(message)
                    builder.setPositiveButton(android.R.string.ok) {
                        dialog: DialogInterface?,
                        which: Int ->
                        result.confirm()
                    }
                    builder.setOnCancelListener { dialog: DialogInterface? -> result.cancel() }

                    builder.show()
                    return true
                }

                override fun onJsConfirm(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult,
                ): Boolean {
                    val builder: AlertDialog.Builder = makeAlertBuilder()
                    builder.setTitle(android.R.string.dialog_alert_title)
                    builder.setMessage(message)
                    builder.setPositiveButton(android.R.string.ok) {
                        dialog: DialogInterface?,
                        which: Int ->
                        result.confirm()
                    }
                    builder.setOnCancelListener { dialog: DialogInterface? -> result.cancel() }

                    builder.show()
                    return true
                }

                override fun onJsPrompt(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    defaultValue: String?,
                    result: JsPromptResult,
                ): Boolean {
                    val editText = EditText(context)
                    editText.setText(defaultValue)

                    val builder: AlertDialog.Builder = makeAlertBuilder()
                    builder.setTitle(android.R.string.dialog_alert_title)
                    builder.setMessage(message)
                    builder.setView(editText)
                    builder.setPositiveButton(android.R.string.ok) {
                        dialogInterface: DialogInterface?,
                        i: Int ->
                        val text = editText.getText().toString()
                        result.confirm(text)
                    }
                    builder.setNegativeButton(android.R.string.cancel) {
                        dialog: DialogInterface?,
                        which: Int ->
                        result.cancel()
                    }
                    builder.setOnCancelListener { dialog: DialogInterface? -> result.cancel() }

                    builder.show()
                    return true
                }
            }
        )

        webView.setWebViewClient(
            object : WebViewClient() {
                private var mainFrameFinished = false

                // Page started to be drawn on screen. We consider this as loaded, meaning that we
                // should dismiss the loaders
                // and stop the timeouts.
                // On API 21-22, this will unfortunately be when the DOM is ready as there is no
                // better callback.
                // This method is also prefixed so that we never conflict with an Android SDK update
                fun onBatchPageStartedDrawing() {
                    timeoutDone = true
                    if (!mainFrameFinished) {
                        mainFrameFinished = true
                        // Fist page has been successfully loaded, hide the spinner
                        removeWebViewLoader()
                    }
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    onBatchPageStartedDrawing()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // Only called for main frame !
                    super.onPageFinished(view, url)
                    // On API 23+, onPageCommitVisible will be called instead
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        onBatchPageStartedDrawing()
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?,
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    // Only needed when API lvl < 21
                    if (!mainFrameFinished && (failingUrl != null && failingUrl == view.getUrl())) {
                        handleWebViewError(description, errorCode)
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?,
                ) {
                    super.onReceivedSslError(view, handler, error)
                    if (!mainFrameFinished) {
                        closeMessageWithError(
                            DevelopmentErrorCause.SSL,
                            MessagingError.SERVER_FAILURE,
                            null,
                        )
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request.isForMainFrame) {
                        closeMessageWithError(
                            DevelopmentErrorCause.BAD_HTTP_STATUSCODE,
                            MessagingError.SERVER_FAILURE,
                            errorResponse.statusCode.toString(),
                        )
                    }
                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame && request.url.toString() == view.getUrl()) {
                        handleWebViewError(error.description.toString(), error.errorCode)
                    }
                }
            }
        )
    }
}
