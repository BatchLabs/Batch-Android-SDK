package com.batch.android.messaging.model.cep

import android.graphics.Typeface
import com.batch.android.messaging.model.cep.InAppProperty.*

/**
 * Represents a component within an in-app message.
 *
 * This abstract class serves as the base for various UI components that can be included in an
 * in-app message, such as text, buttons, images, dividers, and column layouts.
 *
 * @property type The type of the component. Determines the specific nature of the component (e.g.,
 *   Text, Button, Image). This is defined by the [Type] enum.
 */
sealed class InAppComponent(val type: Type) {
    /** Represents the type of the component. */
    enum class Type {
        TEXT,
        BUTTON,
        IMAGE,
        DIVIDER,
        COLUMNS,
    }

    /** Represents a component that can be part of a column layout. */
    sealed interface Column {
        fun isEmpty(): Boolean {
            return false
        }
    }

    /** Interface for components that can have text decorations applied. */
    sealed interface FontDecorationComponent {

        companion object {
            /** Typeface override for the text view. */
            @JvmStatic var typefaceOverride: Typeface? = null

            /** Typeface override for the text view when the text is bold. */
            @JvmStatic var boldTypefaceOverride: Typeface? = null

            /**
             * Sets the custom typeface for the text view based on the font decoration component.
             *
             * @param textView The text view to apply the typeface to.
             * @param component The font decoration component containing the typeface information.
             */
            fun setCustomTypeface(
                textView: android.widget.TextView,
                component: FontDecorationComponent,
            ) {
                textView.apply {
                    // Apply typeface based on the component's font decoration
                    val customTypeface =
                        if (component.isBold()) {
                            boldTypefaceOverride
                        } else {
                            typefaceOverride
                        }
                    if (customTypeface != null) {
                        setTypeface(Typeface.create(customTypeface, component.typeface))
                    } else {
                        setTypeface(typeface, component.typeface)
                    }
                    // Add font decorations config
                    paint.apply {
                        isUnderlineText = component.isUnderline()
                        isStrikeThruText = component.isStroke()
                    }
                }
            }
        }

        val fontDecorations: Set<FontDecoration>

        fun isBoldItalic(): Boolean {
            return isBold() && isItalic()
        }

        fun isBold(): Boolean {
            return fontDecorations.contains(FontDecoration.BOLD)
        }

        fun isItalic(): Boolean {
            return fontDecorations.contains(FontDecoration.ITALIC)
        }

        fun isUnderline(): Boolean {
            return fontDecorations.contains(FontDecoration.UNDERLINE)
        }

        fun isStroke(): Boolean {
            return fontDecorations.contains(FontDecoration.STROKE)
        }

        val typeface: Int
            get() =
                when {
                    isBoldItalic() -> Typeface.BOLD_ITALIC
                    isBold() -> Typeface.BOLD
                    isItalic() -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
    }

    /**
     * Represents a text component within an in-app message.
     *
     * This class defines the properties and styling options for a text element. It inherits from
     * [InAppComponent] and implements the [Column] interface, indicating that it's a component and
     * can be part of a column layout.
     *
     * @property id The unique identifier for the text component.
     * @property color A [ThemeColors] representing the text color in light and dark mode.
     * @property margins A [Margin] defining the margin around the text.
     * @property textAlignment The alignment of the text within the component.
     * @property fontDecorations A set of [FontDecoration] options to apply to the text.
     * @property fontSize The font size of the text.
     * @property maxLines The maximum number of lines the text can span.
     */
    data class Text(
        val id: String,
        val color: ThemeColors,
        val margins: Margin,
        val textAlignment: HorizontalAlignment,
        override val fontDecorations: Set<FontDecoration>,
        val fontSize: Int,
        val maxLines: Int,
    ) : InAppComponent(Type.TEXT), FontDecorationComponent, Column

    /**
     * Represents a button component within an in-app message.
     *
     * This class defines the properties and styling options for a button element. It inherits from
     * [InAppComponent] and implements the [Column] interface, indicating that it's a component and
     * can be part of a column layout.
     *
     * @property id The unique identifier for the button component.
     * @property backgroundColor A [ThemeColors] representing the background color in light and dark
     *   mode.
     * @property textColor A [ThemeColors] representing the text color in light and dark mode.
     * @property margins A [Margin] defining the margin around the button.
     * @property paddings A [Padding] defining the padding around the button.
     * @property width The width of the button. Can be a fixed value or a percentage.
     * @property align The alignment of the button within the parent component.
     * @property radius A [CornerRadius] defining the corner radius of the button.
     * @property border A [Border] defining the border of the button.
     * @property textAlignment The alignment of the text within the button.
     * @property fontDecorations A set of [FontDecoration] options to apply to the text.
     * @property fontSize The font size of the text.
     * @property maxLines The maximum number of lines the text can span.
     */
    data class Button(
        val id: String,
        val backgroundColor: ThemeColors,
        val textColor: ThemeColors,
        val margins: Margin,
        val paddings: Padding,
        val width: Size?,
        val align: HorizontalAlignment,
        val radius: CornerRadius = CornerRadius(),
        val border: Border?,
        val textAlignment: HorizontalAlignment,
        override val fontDecorations: Set<FontDecoration>,
        val fontSize: Int,
        val maxLines: Int,
    ) : InAppComponent(Type.BUTTON), FontDecorationComponent, Column

    /**
     * Represents an image component within an in-app message.
     *
     * This class defines the properties and styling options for an image element. It inherits from
     * [InAppComponent] and implements the [Column] interface, indicating that it's a component and
     * can be part of a column layout.
     *
     * @property id The unique identifier for the image component.
     * @property height The height of the image.
     * @property margins A [Margin] defining the margin around the image.
     * @property aspectRatio The aspect ratio of the image.
     * @property radius A [CornerRadius] defining the corner radius of the image.
     */
    data class Image(
        val id: String,
        val height: Size,
        val margins: Margin,
        val aspectRatio: AspectRatio,
        val radius: CornerRadius,
    ) : InAppComponent(Type.IMAGE), Column {
        /** Represents the aspect ratio of the image. */
        enum class AspectRatio {
            FIT,
            FILL;

            fun toScaleType(): android.widget.ImageView.ScaleType {
                return when (this) {
                    FIT -> android.widget.ImageView.ScaleType.FIT_CENTER
                    FILL -> android.widget.ImageView.ScaleType.CENTER_CROP
                }
            }
        }
    }

    /**
     * Represents a divider component within an in-app message.
     *
     * This class defines the properties and styling options for a divider element. It inherits from
     * [InAppComponent] and implements the [Column] interface, indicating that it's a component and
     * can be part of a column layout.
     *
     * @property thickness The thickness of the divider.
     * @property color A [ThemeColors] representing the color of the divider in light and dark mode.
     * @property width The width of the divider. Can be a fixed value or a percentage.
     * @property align The alignment of the divider within the parent component.
     * @property margins A [Margin] defining the margin around the divider.
     */
    data class Divider(
        val thickness: Int,
        val color: ThemeColors,
        val width: Size,
        val align: HorizontalAlignment,
        val margins: Margin,
    ) : InAppComponent(Type.DIVIDER), Column

    /** Represents an empty spacer component within n Columns component message. */
    data class EmptySpacer(val empty: Boolean = true) : Column {
        override fun isEmpty(): Boolean {
            return empty
        }
    }

    /**
     * Represents a column layout component within an in-app message.
     *
     * This class defines the properties and styling options for a column layout element. It
     * inherits from [InAppComponent] and implements the [Column] interface, indicating that it's a
     * component and can be part of a column layout.
     *
     * @property ratios An array of floats representing the relative sizes of the columns.
     * @property spacing The spacing between columns.
     * @property margins A [Margin] defining the margin around the column layout.
     * @property contentAlign The alignment of the content within the columns.
     * @property children A list of [Column] objects representing the columns within the layout.
     */
    data class Columns(
        val ratios: FloatArray,
        val spacing: Int,
        val margins: Margin,
        val contentAlign: VerticalAlignment,
        val children: List<Column>,
    ) : InAppComponent(Type.COLUMNS) {

        init {
            require(children.size == ratios.size) {
                "Number of ratios must match the number of children"
            }
            require(listOf(1F, 100F).contains(ratios.sum())) { "Sum of ratio must be 1 or 100" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Columns

            if (children != other.children) return false
            if (!ratios.contentEquals(other.ratios)) return false
            if (spacing != other.spacing) return false
            if (margins != other.margins) return false
            if (contentAlign != other.contentAlign) return false

            return true
        }

        override fun hashCode(): Int {
            var result = children.hashCode()
            result = 31 * result + ratios.contentHashCode()
            result = 31 * result + spacing
            result = 31 * result + margins.hashCode()
            result = 31 * result + contentAlign.hashCode()
            return result
        }
    }
}
