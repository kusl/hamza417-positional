package app.simple.positional.util

import android.icu.text.MessageFormat

object Ordinal {
    fun Number?.toOrdinal(): String {
        if (this == null) {
            return "N/A"
        }

        val testArgs = arrayOf<Any>(this)
        val messageFormat = MessageFormat("{0,ordinal}", LocaleHelper.getAppLocale())

        return messageFormat.format(testArgs)
    }
}
