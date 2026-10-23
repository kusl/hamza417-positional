package app.simple.positional.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

open class ContextUtils(context: Context) : ContextWrapper(context) {
    companion object {
        @SuppressLint("ObsoleteSdkInt")
        fun updateLocale(baseContext: Context, languageCode: String): ContextWrapper {
            val localeToSwitchTo = if (languageCode == "default") {
                if (LocaleHelper.isOneOfTraditionalChinese()) {
                    Locale.forLanguageTag("zh-TW")
                } else {
                    Locale.forLanguageTag(LocaleHelper.getSystemLanguageCode())
                }
            } else {
                Locale.forLanguageTag(languageCode)
            }

            var context = baseContext
            val resources: Resources = context.resources
            val configuration: Configuration = resources.configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(localeToSwitchTo)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else {
                @Suppress("deprecation")
                configuration.locale = localeToSwitchTo
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                context = context.createConfigurationContext(configuration)
            } else {
                @Suppress("deprecation")
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }

            return ContextUtils(context)
        }
    }
}
