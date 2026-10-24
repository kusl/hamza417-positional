package app.simple.positional.extensions.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.lifecycle.AndroidViewModel
import app.simple.positional.singleton.SharedPreferences.getSharedPreferences
import app.simple.positional.util.ContextUtils

open class WrappedViewModel(application: Application) : AndroidViewModel(application), OnSharedPreferenceChangeListener {
    init {
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    protected fun getContext(): Context =
        ContextUtils.wrap(getApplication<Application>().applicationContext, ContextUtils.language(), applyNightMode = false)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {}

    override fun onCleared() {
        super.onCleared()
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    }
}
