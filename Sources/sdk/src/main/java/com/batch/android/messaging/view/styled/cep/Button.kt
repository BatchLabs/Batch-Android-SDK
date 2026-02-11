package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.batch.android.core.Logger
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.view.extensions.px
import com.batch.android.messaging.view.helper.StyleHelper
import com.batch.android.messaging.view.percent.PercentRelativeLayout
import com.batch.android.module.MessagingModule

/**
 * `Button` is a custom View designed to render a button. It dynamically adjusts its size, margins,
 * and color based on the provided `InAppComponent.Button` style.
 *
 * This class implements the `Styleable` interface, enabling it to apply styles from an
 * `InAppComponent` instance.
 *
 * @constructor Creates a new instance of the `Button` View.
 */
class Button
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatButton(context, attrs, defStyleAttr), Styleable {

    override fun applyComponentStyle(component: InAppComponent) {

        // Ensure the component is a button
        if (component !is InAppComponent.Button) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-button style")
            return
        }

        // Build button's background
        val background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = component.radius.cornerRadiiPx(context)
                color =
                    ColorStateList.valueOf(
                        StyleHelper.parseColor(component.backgroundColor.getColorForTheme(context))
                    )
                component.border?.let {
                    setStroke(
                        it.width,
                        ColorStateList.valueOf(
                            StyleHelper.parseColor(it.color.getColorForTheme(context))
                        ),
                    )
                }
            }

        // Remove default min height from theme
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0

        // Remove default uppercase text
        isAllCaps = false

        // Remove button's shadow
        stateListAnimator = null

        // Define width according to the size unit
        component.width?.let {
            layoutParams =
                when (component.width.unit) {
                    InAppProperty.Size.Unit.PIXEL ->
                        LinearLayout.LayoutParams(
                            it.floatValue.px,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )

                    InAppProperty.Size.Unit.PERCENTAGE ->
                        PercentRelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                            .apply { percentLayoutInfo.widthPercent = it.floatValue / 100 }

                    InAppProperty.Size.Unit.AUTO,
                    InAppProperty.Size.Unit.FILL ->
                        // Since Fill width is not supported, we fallback on auto
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }.apply {
                    setMargins(
                        component.margins.left.px,
                        component.margins.top.px,
                        component.margins.right.px,
                        component.margins.bottom.px,
                    )
                    setPadding(
                        component.paddings.left.px,
                        component.paddings.top.px,
                        component.paddings.right.px,
                        component.paddings.bottom.px,
                    )
                }
        }

        // Set button's gravity
        gravity = Gravity.CENTER_VERTICAL or component.textAlignment.toGravity()

        // Add ripple effect
        val rippleColor = StyleHelper.parseColor("#80FFFFFF")
        val rippleDrawable =
            RippleDrawable(ColorStateList.valueOf(rippleColor), background, background)

        backgroundTintList = null
        backgroundTintMode = null

        // Set background
        setBackground(rippleDrawable)

        // Set text color and size
        setTextColor(StyleHelper.parseColor(component.textColor.getColorForTheme(context)))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, component.fontSize.toFloat())

        // Add max lines if needed
        if (component.maxLines > 0) {
            maxLines = component.maxLines
            ellipsize = TextUtils.TruncateAt.END
        }

        // Set typeface
        InAppComponent.FontDecorationComponent.setCustomTypeface(this, component)
    }
}
