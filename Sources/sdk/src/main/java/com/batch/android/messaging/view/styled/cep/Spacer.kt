package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.batch.android.core.Logger
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.view.extensions.px
import com.batch.android.module.MessagingModule

class Spacer(context: Context, private val parentView: View) : View(context), Styleable {

    override fun applyComponentStyle(component: InAppComponent) {

        if (component !is InAppComponent.Spacer) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-spacer style")
            return
        }

        if (component.height.isFill()) {
            layoutParams =
                LinearLayout.LayoutParams(0, 0, 1.0f).apply {
                    val parentLinearLayout = parentView as LinearLayout
                    if (parentLinearLayout.orientation == LinearLayout.VERTICAL) {
                        height = 0
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                    } else {
                        width = 0
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
        } else if (component.height.isPixel()) {
            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    component.height.floatValue.px,
                )
        } else {
            Logger.internal(
                MessagingModule.TAG,
                "Spacer component added to a non-LinearLayout. It might not behave as expected.",
            )
        }
        setBackgroundColor(Color.TRANSPARENT)
    }
}
