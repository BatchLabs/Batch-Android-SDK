package com.batch.android.messaging.view.extensions

import android.content.res.Resources

/** Float extension to convert dp value to px. (Extension is scoped to this class only) */
val Float.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/** Float extension to convert dp value to px. (Extension is scoped to this class only) */
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
