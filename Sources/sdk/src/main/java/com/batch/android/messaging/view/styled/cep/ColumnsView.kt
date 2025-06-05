package com.batch.android.messaging.view.styled.cep

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import com.batch.android.core.Logger
import com.batch.android.messaging.model.cep.InAppComponent
import com.batch.android.messaging.view.extensions.px
import com.batch.android.module.MessagingModule

/**
 * A custom LinearLayout that represents a view for displaying content in columns.
 *
 * This view is designed to handle a list of components and arrange them into columns based on
 * specified ratios. It supports custom margins, spacing between columns, and dynamically adding
 * content to each column.
 *
 * This class implements the `Styleable` interface, enabling it to apply styles from an
 * `InAppComponent` instance.
 *
 * @param context The application context.
 */
class ColumnsView(context: Context) : LinearLayout(context), Styleable {

    /** Apply the style of a component to this view. */
    override fun applyComponentStyle(component: InAppComponent) {

        // Ensure the component is a columns
        if (component !is InAppComponent.Columns) {
            Logger.internal(MessagingModule.TAG, "Trying to apply a non-columns style")
            return
        }

        clipChildren = false
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply {
                    setMargins(
                        component.margins.left.px,
                        component.margins.top.px,
                        component.margins.right.px,
                        component.margins.bottom.px,
                    )
                }
        gravity = component.contentAlign.toGravity()
    }

    /** Build the columns based on the provided component. */
    fun buildColumns(
        component: InAppComponent.Columns,
        addComponentToView: (InAppComponent, LinearLayout) -> Unit,
    ) {
        for (i in component.ratios.indices) {
            val column =
                LinearLayout(context).apply {
                    clipChildren = false
                    layoutParams =
                        LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, component.ratios[i])
                            .apply {
                                setMargins(
                                    0,
                                    0,
                                    if (i != component.ratios.size - 1) component.spacing.px else 0,
                                    0,
                                )
                            }
                    orientation = VERTICAL
                }
            if (!component.children[i].isEmpty()) {
                addComponentToView(component.children[i] as InAppComponent, column)
            }
            this.addView(column)
        }
    }
}
