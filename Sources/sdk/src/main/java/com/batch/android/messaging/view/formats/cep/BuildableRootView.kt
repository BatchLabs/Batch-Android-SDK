package com.batch.android.messaging.view.formats.cep

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat as AndroidxViewCompat
import androidx.core.view.WindowInsetsCompat
import com.batch.android.R
import com.batch.android.core.Logger
import com.batch.android.messaging.AsyncImageDownloadTask
import com.batch.android.messaging.model.CTA
import com.batch.android.messaging.model.MessagingError
import com.batch.android.messaging.model.cep.CEPMessage
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.view.CloseButton
import com.batch.android.messaging.view.CountdownView
import com.batch.android.messaging.view.extensions.px
import com.batch.android.messaging.view.helper.StyleHelper
import com.batch.android.messaging.view.helper.ViewCompat
import com.batch.android.messaging.view.percent.PercentRelativeLayout
import com.batch.android.messaging.view.styled.cep.Button
import com.batch.android.messaging.view.styled.cep.ColumnsView
import com.batch.android.messaging.view.styled.cep.Divider
import com.batch.android.messaging.view.styled.cep.ProgressImageView
import com.batch.android.messaging.view.styled.cep.TextView
import com.batch.android.module.MessagingModule

/** Root view for the message. */
class BuildableRootView(
    context: Context,
    private val message: CEPMessage,
    private val imagesCached: MutableMap<String, AsyncImageDownloadTask.Result<*>>,
) : RelativeLayout(context), OnApplyWindowInsetsListener {

    /** Countdown view for auto dismiss */
    private var countdownView: CountdownView? = null

    /** Images view instances */
    private var imageViews = mutableMapOf<String, ProgressImageView>()

    /** Listener for CTA actions */
    var actionListener: OnActionListener? = null

    /** The scroll view that contains the message content. */
    var scrollView = ScrollView(context)

    /** Interface for handling actions */
    interface OnActionListener {
        /** Called when the close button is clicked or the auto dismiss countdown is finished */
        fun onCloseAction()

        /** Called when a CTA is clicked */
        fun onCTAAction(id: String, type: String, cta: CTA)

        /** Called when the image download fails. */
        fun onErrorAction(cause: MessagingError)
    }

    init {
        id = R.id.com_batchsdk_messaging_root_view

        // Set a window insets listener to handle system bars insets
        // The listener will consume the insets and does not dispatch them to child views
        AndroidxViewCompat.setOnApplyWindowInsetsListener(this, this)

        // Build views
        addRootContainer()

        // Add close option (button and/or delay)
        addCloseOptions()
    }

    /**
     * Apply the insets to the view.
     *
     * Used to add padding to the bottom of the view when the navigation bar is shown. Consumes the
     * insets to avoid dispatching to child views.
     */
    override fun onApplyWindowInsets(
        view: View,
        windowInsets: WindowInsetsCompat,
    ): WindowInsetsCompat {
        if (message.isAttachedBottomBanner()) {
            // Handle navigation bar insets
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + insets.bottom)
        }
        if (message.isAttachedTopBanner()) {
            // Handle status bar insets
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(paddingLeft, paddingTop + insets.top, paddingRight, paddingBottom)
        }

        if (message.isFullscreen()) {
            // Handle system bars insets
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                paddingLeft + insets.left,
                paddingTop + insets.top,
                paddingRight + insets.right,
                paddingBottom + insets.bottom,
            )
        }
        return WindowInsetsCompat.CONSUMED
    }

    /**
     * Apply the dialog insets to the window.
     *
     * @param window The window to apply the insets to
     */
    fun applyDialogInsets(window: Window?) {
        if (message.isCenterModal()) {
            val inset =
                InsetDrawable(
                    Color.TRANSPARENT.toDrawable(),
                    message.rootContainer.margins.left.px,
                    message.rootContainer.margins.top.px,
                    message.rootContainer.margins.right.px,
                    message.rootContainer.margins.bottom.px,
                )
            window?.setBackgroundDrawable(inset)
        }
    }

    /** Add the root container to the view. */
    private fun addRootContainer() {
        // Apply root container margins for banner format
        // Modal format use applyDialogInsets
        if (message.isBanner()) {
            layoutParams =
                FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    .apply {
                        setMargins(
                            message.rootContainer.margins.left.px,
                            message.rootContainer.margins.top.px,
                            message.rootContainer.margins.right.px,
                            message.rootContainer.margins.bottom.px,
                        )
                    }
        }

        // Build background (border, radius, ...)
        val background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                elevation = 10F
                color =
                    ColorStateList.valueOf(
                        StyleHelper.parseColor(
                            message.rootContainer.backgroundColor?.getColorForTheme(context)
                        )
                    )
                if (message.isModal()) {
                    // Border and radius style are only available for modal format
                    cornerRadii = message.rootContainer.radius.cornerRadiiPx(context)
                    message.rootContainer.border?.width?.let {
                        setStroke(
                            it,
                            ColorStateList.valueOf(
                                StyleHelper.parseColor(
                                    message.rootContainer.border.color.getColorForTheme(context)
                                )
                            ),
                        )
                    }
                }
                clipToOutline = true
            }
        setBackground(background)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        val contentLayout =
            LinearLayout(context).apply {
                id = R.id.com_batchsdk_messaging_content_view
                clipChildren = false
                layoutParams =
                    LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                orientation = LinearLayout.VERTICAL
                message.rootContainer.border?.width?.let { setPadding(it, it, it, it) }

                if (message.isFullscreen()) {
                    gravity = message.position.toGravity()
                }

                // Add children views
                for (component in message.rootContainer.children) {
                    addComponentToView(component, this)
                }
            }
        // Encapsulate content layout in a ScrollView
        scrollView.apply {
            id = R.id.com_batchsdk_messaging_scroll_view
            layoutParams =
                FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    if (message.isModal()) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT,
                )
            isFillViewport = true
        }
        scrollView.addView(contentLayout)
        addView(scrollView)
    }

    /** Add a the right view according to the component to the root view. */
    private fun addComponentToView(component: InAppComponent, view: ViewGroup) {
        when (component.type) {
            InAppComponent.Type.TEXT -> addTextToView(component as InAppComponent.Text, view)
            InAppComponent.Type.BUTTON -> addButtonToView(component as InAppComponent.Button, view)
            InAppComponent.Type.IMAGE -> addImageToView(component as InAppComponent.Image, view)
            InAppComponent.Type.DIVIDER ->
                addDividerToView(component as InAppComponent.Divider, view)

            InAppComponent.Type.COLUMNS ->
                addColumnsToView(component as InAppComponent.Columns, view)
        }
    }

    /** Add a text component to the view. */
    private fun addTextToView(component: InAppComponent.Text, view: ViewGroup) {
        val textView =
            TextView(context).apply {
                applyComponentStyle(component)
                text = StyleHelper.textFromHTML(message.texts[component.id])
            }
        view.addView(textView)
    }

    /** Add a button component to the view. */
    private fun addButtonToView(component: InAppComponent.Button, view: ViewGroup) {
        val button =
            Button(context).apply {
                // Set button style
                applyComponentStyle(component)

                // Add text
                text = StyleHelper.textFromHTML(message.texts[component.id])

                // Add listener to handle Actions
                setOnClickListener {
                    actionListener?.onCTAAction(
                        component.id,
                        "button",
                        CTA(
                            message.texts[component.id],
                            message.actions[component.id]?.action,
                            message.actions[component.id]?.args,
                        ),
                    )
                }
            }

        // Add specific container for percent relative layout
        val percentLayout =
            PercentRelativeLayout(context).apply {
                layoutParams =
                    PercentRelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                // Re-set gravity since button is not in the same container
                gravity = component.align.toGravity()
                clipChildren = false
            }
        percentLayout.addView(button)
        view.addView(percentLayout)
    }

    /** Add an image component to the view. */
    private fun addImageToView(component: InAppComponent.Image, view: ViewGroup) {
        val contentDescription = message.texts[component.id]
        val progressImageLayout =
            ProgressImageView(context).apply {
                applyComponentStyle(component)
                setImageContentDescription(contentDescription)
                message.actions[component.id]?.let { imageAction ->
                    setOnClickListener {
                        actionListener?.onCTAAction(
                            component.id,
                            "image",
                            CTA(contentDescription, imageAction.action, imageAction.args),
                        )
                    }
                }
            }

        // Keep layouts to handle images when they are downloaded
        imageViews[component.id] = progressImageLayout

        // We need to wait for the view to be measured to set the image size
        // since its can be resized for auto height
        progressImageLayout.viewTreeObserver.addOnGlobalLayoutListener(
            object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Listener no longer needed
                    progressImageLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    // Check if image is in cache
                    val cachedImage = imagesCached[component.id]
                    if (cachedImage != null) {
                        // Directly set the image since it's already cached
                        progressImageLayout.hideProgressBar()
                        progressImageLayout.setImage(component, cachedImage)
                    } else {
                        // Adding image view hint size while image is downloading.
                        progressImageLayout.setImageHint(component, message.urls[component.id])
                    }
                }
            }
        )
        // Add image to the view
        view.addView(progressImageLayout)
    }

    /** Start the download of an image. */
    fun startDownloadImage(componentId: String) {
        imageViews[componentId]?.showProgressBar()
    }

    /** Set the downloaded image. */
    fun setDownloadedImage(componentId: String, result: AsyncImageDownloadTask.Result<*>?) {
        imageViews[componentId]?.let { progressImageView ->
            progressImageView.hideProgressBar()
            message.getImageComponentById(componentId)?.let { component ->
                if (result == null) {
                    Logger.internal(MessagingModule.TAG, "Downloaded image is null")
                    message.getImageComponentById(componentId)?.let {
                        progressImageView.setImage(it, null)
                    }
                } else {
                    progressImageView.setImage(component, result)
                }
            }
        }
    }

    /** Download image failed. Hide progress bar and set placeholder. */
    fun downloadImageFailed(componentId: String) {
        imageViews[componentId]?.let {
            message.getImageComponentById(componentId)?.let { component ->
                it.hideProgressBar()
                it.setImage(component, null)
            }
        }
    }

    /** Add a divider component to the view. */
    private fun addDividerToView(component: InAppComponent.Divider, view: ViewGroup) {
        val dividerView = Divider(context).apply { applyComponentStyle(component) }
        // Add specific container for percent relative layout if width is defined from percent
        val percentLayout =
            PercentRelativeLayout(context).apply {
                layoutParams =
                    PercentRelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                // Re-set gravity since divider is not in the same container
                gravity = component.align.toGravity()
            }
        percentLayout.addView(dividerView)
        view.addView(percentLayout)
    }

    /** Add a columns component to the view. */
    private fun addColumnsToView(component: InAppComponent.Columns, view: ViewGroup) {
        val layout =
            ColumnsView(context).apply {
                applyComponentStyle(component)
                buildColumns(component) { component, layout ->
                    addComponentToView(component, layout)
                }
            }
        view.addView(layout)
    }

    /** Add close options components to the view. */
    private fun addCloseOptions() {
        // Add close button option
        message.closeOptions?.button.let {
            val closeButton = CloseButton(context)
            closeButton.id = R.id.com_batchsdk_messaging_close_button
            closeButton.apply {
                setSize(24)
                setPadding(10)
                setGlyphColor(StyleHelper.parseColor(it?.color?.getColorForTheme(context)))
                setGlyphStrokeCap(Paint.Cap.ROUND)
                setGlyphPadding(7.px)
                setGlyphWidth(3.px)
                setBackgroundColor(
                    StyleHelper.parseColor(it?.backgroundColor?.getColorForTheme(context))
                )
                layoutParams =
                    LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        .apply { addRule(ALIGN_PARENT_RIGHT) }
            }
            closeButton.setOnClickListener { actionListener?.onCloseAction() }
            addView(closeButton)
        }
        // Add auto dismiss close option
        message.closeOptions?.auto.let {
            countdownView =
                CountdownView(this.context).apply {
                    id = R.id.com_batchsdk_messaging_countdown_progress
                    layoutParams =
                        LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2.px).apply {
                            if (message.isTopBanner()) {
                                addRule(BELOW, R.id.com_batchsdk_messaging_scroll_view)
                            } else {
                                addRule(ALIGN_PARENT_LEFT)
                            }
                        }
                    setColor(StyleHelper.parseColor(it?.color?.getColorForTheme(context)))
                }
            addView(countdownView)
        }
    }

    /** Whether this view can be auto-closed. */
    fun canAutoClose(): Boolean {
        return !ViewCompat.isTouchExplorationEnabled(context)
    }

    /** Start the auto-close countdown. */
    fun startAutoCloseCountdown() {
        message.closeOptions?.auto?.delay?.let {
            if (countdownView != null && it > 0) {
                countdownView!!.animateForDuration(it.toLong() * 1000)
            }
        }
    }
}
