package app.simple.positional.extensions.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.AndroidViewModel
import app.simple.positional.singleton.SharedPreferences.getSharedPreferences
import app.simple.positional.util.ContextUtils

open class WrappedViewModel(application: Application) : AndroidViewModel(application), OnSharedPreferenceChangeListener {
    @Volatile
    private var localized: Pair<String, Context>? = null

    init {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    protected fun getContext(): Context {
        val language = ContextUtils.language()
        localized?.let { (code, context) -> if (code == language) return context }
        return ContextUtils.wrap(getApplication<Application>().applicationContext, language, applyNightMode = false)
            .also { localized = language to it }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}

    override fun onCleared() {
        super.onCleared()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    }
}
