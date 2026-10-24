package app.simple.positional.extensions.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import app.simple.positional.util.ContextUtils

open class WrappedViewModel(application: Application) : AndroidViewModel(application) {
    @Volatile
    private var localized: Pair<String, Context>? = null

    protected fun getContext(): Context {
        val language = ContextUtils.language()
        localized?.let { (code, context) -> if (code == language) return context }
        return ContextUtils.wrap(getApplication<Application>().applicationContext, language, applyNightMode = false)
            .also { localized = language to it }
    }
}
