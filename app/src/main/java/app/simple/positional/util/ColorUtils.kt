package app.simple.positional.util

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

object ColorUtils {
    @ColorInt
    fun Context.resolveAttrColor(@AttrRes attr: Int): Int {
        val a = theme.obtainStyledAttributes(intArrayOf(attr))
        return try {
            a.getColor(0, 0)
        } finally {
            a.recycle()
        }
    }

    fun Int.toHex(): String = String.format("#%06x", this and 0xffffff)
}
