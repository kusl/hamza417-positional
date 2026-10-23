package app.simple.positional.util

import android.annotation.SuppressLint
import android.content.res.Resources

object StatusBarHeight {
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getStatusBarHeight(resources: Resources): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }
}
