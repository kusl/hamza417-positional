package app.simple.positional.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import app.simple.positional.preferences.MainPreferences
import java.util.Locale

object ContextUtils {
    fun wrap(base: Context, languageCode: String, applyNightMode: Boolean = true): Context {
        val locale = when {
            languageCode != "default" -> Locale.forLanguageTag(languageCode)
            LocaleHelper.isOneOfTraditionalChinese() -> Locale.forLanguageTag("zh-TW")
            else -> Locale.forLanguageTag(LocaleHelper.getSystemLanguageCode())
        }
        val configuration = Configuration(base.resources.configuration)
        val locales = LocaleList(locale)
        LocaleList.setDefault(locales)
        configuration.setLocales(locales)
        if (applyNightMode) {
            val night = if (NightMode.isNight(base)) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        }
        return base.createConfigurationContext(configuration)
    }

    fun language(): String = MainPreferences.getAppLanguage() ?: "default"
}
