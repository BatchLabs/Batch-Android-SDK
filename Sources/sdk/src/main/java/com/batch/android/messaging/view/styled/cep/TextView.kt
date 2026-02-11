package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.batch.android.core.Logger
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.view.extensions.px
import com.batch.android.messaging.view.helper.StyleHelper
import com.batch.android.module.MessagingModule

class TextView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatTextView(context, attrs, defStyleAttr), Styleable {

    override fun applyComponentStyle(component: InAppComponent) {

        // Ensure the component is a text
        if (component !is InAppComponent.Text) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-text style")
            return
        }

        layoutParams =
            LinearLayout.LayoutParams(
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

        // Add max lines / ellipsize configuration
        if (component.maxLines > 0) {
            maxLines = component.maxLines
            ellipsize = TextUtils.TruncateAt.END
        }
        setTextColor(StyleHelper.parseColor(component.color.getColorForTheme(context)))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, component.fontSize.toFloat())
        letterSpacing = 0f
        setLineSpacing(0f, 1f)

        // Set text alignment
        gravity = component.textAlignment.toGravity()

        // Set typeface
        InAppComponent.FontDecorationComponent.setCustomTypeface(this, component)
    }
}
