package com.batch.android.messaging.model.cep

import android.content.Context
import android.content.res.Configuration
import android.view.Gravity
import com.batch.android.messaging.view.helper.StyleHelper

typealias Padding = InAppProperty.Margin

abstract class InAppProperty {
    /** Represents the format of an in-app message. */
    enum class Format {
        MODAL,
        FULLSCREEN,
    }

    /** Represents a vertical alignment like the position of an in-app message in fullscreen. */
    enum class VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM;

        fun toGravity(): Int {
            return when (this) {
                TOP -> Gravity.TOP
                CENTER -> Gravity.CENTER_VERTICAL
                BOTTOM -> Gravity.BOTTOM
            }
        }
    }

    /** Represents the horizontal alignment of a component. */
    enum class HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT;

        fun toGravity(): Int {
            return when (this) {
                LEFT -> Gravity.START
                CENTER -> Gravity.CENTER_HORIZONTAL
                RIGHT -> Gravity.END
            }
        }
    }

    /** Represents a font decoration. */
    enum class FontDecoration {
        BOLD,
        ITALIC,
        UNDERLINE,
        STROKE,
    }

    /**
     * Represents a color tuple for light and dark themes.
     *
     * @property light The color to use in light theme.
     * @property dark The color to use in dark theme.
     */
    data class ThemeColors(val light: String, val dark: String) {
        fun getColorForTheme(context: Context): String {
            val nightModeFlags =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val color =
                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_YES -> dark
                    Configuration.UI_MODE_NIGHT_NO -> light
                    else -> light
                }
            return StyleHelper.rgbaToArgb(color)
        }
    }

    /** Represents border properties. */
    data class Border(val width: Int = 0, val color: ThemeColors)

    /**
     * Represents a size object.
     *
     * @property value The string representation of the size. Can be in px, percentage or auto.
     * @throws IllegalArgumentException If the value is not in a valid format.
     */
    data class Size(val value: String) {

        companion object {
            private const val AUTO = "auto"
            private const val PERCENTAGE = "%"
            private const val PIXEL = "px"
        }

        enum class Unit {
            PIXEL,
            PERCENTAGE,
            AUTO,
        }

        var unit: Unit =
            when {
                value.endsWith(PERCENTAGE) -> Unit.PERCENTAGE
                value.endsWith(PIXEL) -> Unit.PIXEL
                value == AUTO -> Unit.AUTO
                else -> throw IllegalArgumentException("Invalid size value: $value")
            }

        fun isAuto(): Boolean = unit == Unit.AUTO

        fun isPercentage(): Boolean = unit == Unit.PERCENTAGE

        val floatValue: Float
            get() =
                when (unit) {
                    Unit.PERCENTAGE -> {
                        value.removeSuffix(PERCENTAGE).toFloat()
                    }

                    Unit.PIXEL -> {
                        value.removeSuffix(PIXEL).toFloat()
                    }

                    Unit.AUTO -> 0f
                }
    }

    /** Represents a margin object. */
    data class Margin(val value: IntArray = intArrayOf(0, 0, 0, 0)) {
        constructor(value: Int) : this(generateSequence { value }.take(4).toList().toIntArray())

        constructor(
            vertical: Int,
            horizontal: Int,
        ) : this(intArrayOf(vertical, horizontal, vertical, horizontal))

        constructor(
            top: Int,
            right: Int,
            bottom: Int,
            left: Int,
        ) : this(intArrayOf(top, right, bottom, left))

        val top: Float
            get() = value[0].toFloat()

        val right: Float
            get() = value[1].toFloat()

        val bottom: Float
            get() = value[2].toFloat()

        val left: Float
            get() = value[3].toFloat()

        val none: Boolean
            get() = value.all { it == 0 }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Margin
            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    /** Represents a corner radius object. */
    data class CornerRadius(val value: IntArray = intArrayOf(4, 4, 4, 4)) {
        constructor(value: Int) : this(generateSequence { value }.take(4).toList().toIntArray())

        constructor(
            topLeftBottomRight: Int,
            topRightBottomLeft: Int,
        ) : this(
            intArrayOf(
                topLeftBottomRight,
                topRightBottomLeft,
                topLeftBottomRight,
                topRightBottomLeft,
            )
        )

        constructor(
            topLeft: Int,
            topRight: Int,
            bottomRight: Int,
            bottomLeft: Int,
        ) : this(intArrayOf(topLeft, topRight, bottomRight, bottomLeft))

        val topLeft: Float
            get() = value[0].toFloat()

        val topRight: Float
            get() = value[1].toFloat()

        val bottomRight: Float
            get() = value[2].toFloat()

        val bottomLeft: Float
            get() = value[3].toFloat()

        val cornerRadii: FloatArray
            get() =
                floatArrayOf(
                    topLeft,
                    topLeft,
                    topRight,
                    topRight,
                    bottomRight,
                    bottomRight,
                    bottomLeft,
                    bottomLeft,
                )

        fun cornerRadiiPx(context: Context): FloatArray {
            return cornerRadii
                .map { StyleHelper.dpToPixels(context.resources, it).toFloat() }
                .toFloatArray()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CornerRadius
            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
}
