package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.core.net.toUri
import com.batch.android.core.Logger
import com.batch.android.messaging.AsyncImageDownloadTask
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.view.extensions.px
import com.batch.android.messaging.view.helper.ImageHelper
import com.batch.android.messaging.view.percent.PercentRelativeLayout
import com.batch.android.module.MessagingModule

/**
 * A custom layout that displays an ImageView with a centered progress bar and a clipped percent
 * layout.
 *
 * This layout is designed to show an image while a progress indicator is overlaid on top of it.
 * Once the image is loaded, the progress bar is hidden. It also provides the ability to set the
 * image within a percent relative layout.
 *
 * The layout consists of:
 * - A [ProgressBar]: Centered in the parent layout, used to indicate loading progress.
 * - A [PercentRelativeLayout]: A container for the [ImageView], allowing for percentage-based
 *   dimensions and positioning. It's clipped to its bounds.
 * - An [ImageView]: The image view that displays the image content.
 *
 * This class implements the `Styleable` interface, enabling it to apply styles from an
 * `InAppComponent` instance.
 *
 * @param context The application context.
 * @constructor Creates a new ProgressImageViewLayout.
 */
class ProgressImageView(context: Context) : RelativeLayout(context), Styleable {

    /** The progress bar displayed while the image is loading. */
    private val progressBar: ProgressBar =
        ProgressBar(context).apply {
            layoutParams =
                LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { addRule(CENTER_IN_PARENT) }
            indeterminateTintList = ColorStateList.valueOf(Color.BLACK)
        }

    /** Shows the progress bar. */
    fun showProgressBar() {
        progressBar.visibility = VISIBLE
    }

    /** Hides the progress bar. */
    fun hideProgressBar() {
        progressBar.visibility = GONE
    }

    /** The container for the [ImageView]. */
    private val percentLayout =
        PercentRelativeLayout(context).apply {
            layoutParams =
                PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            gravity = Gravity.CENTER
            clipToOutline = true
        }

    /** The [ImageView] that displays the image content. */
    private val imageView = ImageView(context)

    /** Initializes the layout. */
    init {
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.CENTER
        addView(progressBar)
        percentLayout.addView(imageView)
        addView(percentLayout)
    }

    /** Set the image in the [ImageView]. */
    fun setImage(component: InAppComponent.Image, result: AsyncImageDownloadTask.Result<*>?) {
        // Apply image size
        setImageSize(component)

        // Set image
        if (result == null) {
            imageView.setImageDrawable(null)
            return
        }
        if (component.height.isAuto()) {
            ImageHelper.setDownloadResultInImageWithResize(
                imageView,
                result,
                percentLayout.width.toFloat(),
            )
        } else {
            ImageHelper.setDownloadResultInImage(imageView, result)
        }
    }

    /**
     * Set the image size according to the size given from the image URL to have the right size
     * before the image finish downloading.
     */
    fun setImageHint(component: InAppComponent.Image, url: String?) {
        if (url == null) return

        Logger.warning(url)

        val imageUri = url.toUri()
        val heightHint = imageUri.getQueryParameter("h")?.toFloatOrNull() ?: 0f
        val widthHint = imageUri.getQueryParameter("w")?.toFloatOrNull() ?: 0f

        if (heightHint == 0f || widthHint == 0f) {
            setImageSize(component, null)
            return
        }

        val computedAutoHeight = (percentLayout.width.toFloat() / widthHint) * heightHint
        setImageSize(component, computedAutoHeight.toInt())
    }

    /** Set the image size */
    private fun setImageSize(component: InAppComponent.Image, height: Int? = null) {
        imageView.apply {
            layoutParams =
                when (component.height.unit) {
                    InAppProperty.Size.Unit.PIXEL ->
                        LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            component.height.floatValue.px,
                        )

                    InAppProperty.Size.Unit.PERCENTAGE ->
                        PercentRelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            .apply {
                                percentLayoutInfo.heightPercent = component.height.floatValue / 100
                            }

                    InAppProperty.Size.Unit.AUTO ->
                        LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            height ?: ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
        }
    }

    /** Apply a style to the image view. */
    override fun applyComponentStyle(component: InAppComponent) {
        // Ensure the component is an Image
        if (component !is InAppComponent.Image) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-image style")
            return
        }
        imageView.scaleType = component.aspectRatio.toScaleType()
        percentLayout.layoutParams =
            PercentRelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                .apply {
                    setMargins(
                        component.margins.left.px,
                        component.margins.top.px,
                        component.margins.right.px,
                        component.margins.bottom.px,
                    )
                }
        percentLayout.background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = component.radius.cornerRadiiPx(context)
                color = ColorStateList.valueOf(Color.TRANSPARENT)
            }
    }

    /** Set the image content description for accessibility. */
    fun setImageContentDescription(contentDescription: String?) {
        imageView.contentDescription = contentDescription
        imageView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
}
