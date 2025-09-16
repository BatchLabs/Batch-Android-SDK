package com.batch.android.messaging.model.cep

import com.batch.android.messaging.model.Action
import com.batch.android.messaging.model.Message
import com.batch.android.messaging.model.cep.InAppProperty.*
import java.io.Serializable

/**
 * Represents an in-app message to be displayed within the application.
 *
 * This class encapsulates all the data required to render an in-app message.
 *
 * @property format The format of the in-app message, determining its visual presentation (e.g.,
 *   banner, modal). See [Format].
 * @property position The position where the message will be displayed on the screen. Defaults to
 *   `InAppPosition.CENTER`. See [VerticalAlignment].
 * @property closeOptions Optional configuration for how the message can be closed by the user. If
 *   null, default close behavior applies. See [CloseOptions].
 * @property rootContainer The root container for the message layout. See [RootContainer].
 * @property texts Optional map of text content for the message.
 * @property urls Optional map of URLs associated with the message.
 * @property actions Optional map of actions associated with the message. See [Action].
 */
class CEPMessage(
    val format: Format,
    val rootContainer: RootContainer,
    var position: VerticalAlignment,
    val closeOptions: CloseOptions?,
    val texts: Map<String, String>,
    val urls: Map<String, String>,
    val actions: Map<String, Action>,
) : Message(null) {

    /** Whether the message is fullscreen. */
    fun isFullscreen(): Boolean {
        return format == Format.FULLSCREEN
    }

    /** Whether the message is a WebView. */
    fun isWebView(): Boolean {
        return format == Format.WEBVIEW
    }

    /** Whether the message is a modal. */
    fun isModal(): Boolean {
        return format == Format.MODAL
    }

    /** Whether the message is a modal centered */
    fun isCenterModal(): Boolean {
        return format == Format.MODAL && position == VerticalAlignment.CENTER
    }

    /** Whether the message is a banner. */
    fun isBanner(): Boolean {
        return format == Format.MODAL &&
            (position == VerticalAlignment.BOTTOM || position == VerticalAlignment.TOP)
    }

    /** Whether the message is a bottom banner without margins. */
    fun isAttachedBottomBanner(): Boolean {
        return format == Format.MODAL &&
            position == VerticalAlignment.BOTTOM &&
            rootContainer.margins.none
    }

    /** Whether the message is a top banner. */
    fun isTopBanner(): Boolean {
        return format == Format.MODAL && position == VerticalAlignment.TOP
    }

    /** Whether the message is a top banner without margins. */
    fun isAttachedTopBanner(): Boolean {
        return isTopBanner() && rootContainer.margins.none
    }

    /** Whether the message is only an image */
    fun isImageFormat(): Boolean {
        return rootContainer.children.size == 1 && rootContainer.children[0] is InAppComponent.Image
    }

    /**
     * Whether the message should not fit system windows.
     *
     * @return True if the message is format Fullscreen or Bottom/Top Banner without margins, false
     *   otherwise.
     */
    fun shouldFitsSystemWindows(): Boolean {
        return !this.isFullscreen() && !this.isAttachedBottomBanner() && !this.isAttachedTopBanner()
    }

    /** Get the list of images components present in the message. */
    fun getImagesComponents(): List<InAppComponent.Image> {
        val images = rootContainer.children.filterIsInstance<InAppComponent.Image>().toMutableList()
        images.addAll(
            rootContainer.children.filterIsInstance<InAppComponent.Columns>().flatMap {
                it.children.filterIsInstance<InAppComponent.Image>()
            }
        )
        return images.toList()
    }

    /** Get the image component with the given ID. */
    fun getImageComponentById(id: String): InAppComponent.Image? {
        return getImagesComponents().find { it.id == id }
    }

    /** Get the WebView component in the message. */
    fun getWebViewComponent(): InAppComponent.WebView? {
        return rootContainer.children.filterIsInstance<InAppComponent.WebView>().firstOrNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CEPMessage

        if (format != other.format) return false
        if (rootContainer != other.rootContainer) return false
        if (position != other.position) return false
        if (closeOptions != other.closeOptions) return false
        if (texts != other.texts) return false
        if (urls != other.urls) return false
        if (actions != other.actions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + rootContainer.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + (closeOptions?.hashCode() ?: 0)
        result = 31 * result + texts.hashCode()
        result = 31 * result + urls.hashCode()
        result = 31 * result + actions.hashCode()
        return result
    }
}

/**
 * Represents the root container for a layout.
 *
 * @property backgroundColor Optional background colors for light and dark themes.
 * @property children The list of components within the container.
 * @property margins Optional margins for the container.
 * @property radius Optional radius for the container (modal only).
 * @property border Optional border for the container (modal only).
 */
data class RootContainer(
    val children: List<InAppComponent>,
    val backgroundColor: ThemeColors?,
    val margins: Margin,
    val radius: CornerRadius,
    val border: Border?,
) : Serializable

/**
 * Represents the options for closing an in-app.
 *
 * @property auto The automatic closing options, or null if not used.
 * @property button The close button options, or null if not used.
 */
data class CloseOptions(val auto: Auto? = null, val button: Button? = null) : Serializable {
    /**
     * Represents the color and background color options for a close button.
     *
     * @property color The color tuple for light and dark themes.
     * @property backgroundColor The background color tuple for light and dark themes.
     */
    data class Button(val color: ThemeColors, val backgroundColor: ThemeColors) : Serializable

    /**
     * Represents the delay and color options for automatic closing.
     *
     * @property delay The delay in seconds before automatic closing.
     * @property color The color tuple for light and dark themes.
     */
    data class Auto(val delay: Int, val color: ThemeColors) : Serializable
}
