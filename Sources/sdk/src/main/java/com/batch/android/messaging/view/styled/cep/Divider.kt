package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.batch.android.core.Logger
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.model.cep.InAppProperty
import com.batch.android.messaging.view.extensions.px
import com.batch.android.messaging.view.helper.StyleHelper
import com.batch.android.messaging.view.percent.PercentRelativeLayout
import com.batch.android.module.MessagingModule

/**
 * `Divider` is a custom View designed to render a horizontal line, often used as a visual separator
 * between UI elements within a layout. It dynamically adjusts its size, margins, and color based on
 * the provided `InAppComponent.Divider` style.
 *
 * This class implements the `Styleable` interface, enabling it to apply styles from an
 * `InAppComponent` instance.
 *
 * @property context The context. Required for creating and configuring the View.
 * @constructor Creates a new instance of the `Divider` View.
 */
class Divider(context: Context) : View(context), Styleable {

    /** Apply the style of a component to this view. */
    override fun applyComponentStyle(component: InAppComponent) {

        // Ensure the component is a button
        if (component !is InAppComponent.Divider) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-divider style")
            return
        }

        layoutParams =
            when (component.width.unit) {
                InAppProperty.Size.Unit.PIXEL ->
                    LinearLayout.LayoutParams(
                        component.width.floatValue.px,
                        component.thickness.toFloat().px,
                    )

                InAppProperty.Size.Unit.PERCENTAGE ->
                    PercentRelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            component.thickness.px,
                        )
                        .apply { percentLayoutInfo.widthPercent = component.width.floatValue / 100 }

                InAppProperty.Size.Unit.AUTO,
                InAppProperty.Size.Unit.FILL ->
                    // Since Fill width is not supported, we fallback on auto
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        component.thickness.px,
                    )
            }.apply {
                setMargins(
                    component.margins.left.px,
                    component.margins.top.px,
                    component.margins.right.px,
                    component.margins.bottom.px,
                )
            }
        val background =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = InAppProperty.CornerRadius((component.thickness.px / 2)).cornerRadii
                color =
                    ColorStateList.valueOf(
                        StyleHelper.parseColor(component.color.getColorForTheme(context))
                    )
            }
        setBackground(background)
    }
}
