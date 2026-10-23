package app.simple.positional.util

object TextViewUtils {
    fun String.capitalizeText(): String {
        return lowercase(LocaleHelper.getAppLocale()).split(" ").joinToString(" ") { word ->
            if (word.length <= 1) word else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(LocaleHelper.getAppLocale()) else it.toString() }
        }
    }
}
